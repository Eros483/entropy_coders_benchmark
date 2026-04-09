import java.util.HashMap;
import java.io.ByteArrayOutputStream;

/**
 * Arithmetic coder using Witten-Neal-Cleary byte-renormalization.
 * 16-bit precision, O(N) streaming encode/decode.
 * Works appropriately for non-streaming setups now.
 */
public class Arithmetic {

    private static final int TF    = 1 << 12;
    private static final int HALF  = 1 << 15;
    private static final int FIRST_QTR = 1 << 14;
    private static final int THIRD_QTR = 3 * (1 << 14);
    private static final int MAX_RANGE = 1 << 16;

    /* ---- Frequency table ---- */
    private static class FreqInfo {
        int[] alphabet, freqs, cumFreqs;
        HashMap<Integer, Integer> symToIdx;
    }

    @SuppressWarnings("unchecked")
    private static FreqInfo build(int[] input) {
        FreqInfo f = new FreqInfo();
        HashMap<Integer, Integer> raw = new HashMap<>();
        for (int s : input) raw.merge(s, 1, Integer::sum);

        f.alphabet = raw.keySet().stream().sorted().mapToInt(Integer::intValue).toArray();
        int n = f.alphabet.length;
        f.freqs = new int[n];
        f.cumFreqs = new int[n + 1];
        f.symToIdx = new HashMap<>();
        for (int i = 0; i < n; i++) f.symToIdx.put(f.alphabet[i], i);

        if (n == 1) {
            f.freqs[0] = TF; f.cumFreqs[0] = 0; f.cumFreqs[1] = TF;
            return f;
        }

        int total = input.length, sum = 0;
        for (int i = 0; i < n; i++) {
            f.freqs[i] = Math.max(1, (int)((long)raw.get(f.alphabet[i]) * TF / total));
            sum += f.freqs[i];
        }

        // Handle Overshoot (The bug fix!)
        while (sum > TF) {
            int best = -1;
            int maxFreq = 0;
            for (int i = 0; i < n; i++) {
                if (f.freqs[i] > maxFreq) {
                    maxFreq = f.freqs[i];
                    best = i;
                }
            }
            if (best == -1 || f.freqs[best] <= 1) break;
            f.freqs[best]--;
            sum--;
        }

        // Handle Undershoot
        while (sum < TF) {
            int best = -1; double bestD = -1.0;
            for (int i = 0; i < n; i++) {
                double d = (double)raw.get(f.alphabet[i]) * TF / total - f.freqs[i];
                if (best == -1 || d > bestD) { bestD = d; best = i; }
            }
            f.freqs[best]++; sum++;
        }

        f.cumFreqs[0] = 0;
        for (int i = 0; i < n; i++) f.cumFreqs[i+1] = f.cumFreqs[i] + f.freqs[i];
        return f;
    }

    /* ---- Bit buffer (Now back to memory, much faster) ---- */
    private static class BitBuf {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private int acc = 0, nBits = 0;

        void bit(int b) {
            acc = (acc << 1) | (b & 1);
            nBits++;
            if (nBits == 8) {
                out.write(acc);
                acc = 0;
                nBits = 0;
            }
        }

        void finish() {
            if (nBits > 0) {
                acc <<= (8 - nBits);
                out.write(acc);
            }
        }

        byte[] bytes() {
            return out.toByteArray();
        }
    }

    /* ---- Bit reader ---- */
    private static class BitReader {
        private final byte[] data;
        int ptr = 0, bitPos = 7;
        BitReader(byte[] d) { data = d; }
        int get() {
            if (ptr >= data.length) return 0;
            int b = (data[ptr] >>> bitPos) & 1;
            if (--bitPos < 0) { bitPos = 7; ptr++; }
            return b;
        }
        int read16() {
            int v = 0;
            for (int i = 0; i < 16; i++) v = (v << 1) | get();
            return v;
        }
    }

    /* ---- Encode ---- */
    public static ArithmeticElement encode(int[] input) {
        if (input == null || input.length == 0) return new ArithmeticElement();

        FreqInfo f = build(input);
        BitBuf buf = new BitBuf();

        int low = 0, high = MAX_RANGE - 1;
        int followOn = 0;

        for (int s : input) {
            int idx = f.symToIdx.get(s);
            int range = high - low + 1;
            int cf  = f.cumFreqs[idx];
            int cfN = f.cumFreqs[idx + 1];

            high = low + (range * cfN) / TF - 1;
            low  = low + (range * cf ) / TF;

            for (;;) {
                if (high < HALF) {
                    buf.bit(0);
                    for (int j = 0; j < followOn; j++) buf.bit(1);
                    followOn = 0;
                    low  <<= 1; high = (high << 1) | 1;
                } else if (low >= HALF) {
                    buf.bit(1);
                    for (int j = 0; j < followOn; j++) buf.bit(0);
                    followOn = 0;
                    low  = ((low  - HALF) << 1); high = ((high - HALF) << 1) | 1;
                } else if (low >= FIRST_QTR && high < THIRD_QTR) {
                    followOn++;
                    low = ((low - FIRST_QTR) << 1); high = ((high - FIRST_QTR) << 1) | 1;
                } else break;
            }
        }

        buf.bit(0);
        for (int j = 0; j < followOn; j++) buf.bit(1);
        int emit = low;
        for (int k = 0; k < 16; k++) { buf.bit((emit >>> 15) & 1); emit <<= 1; }

        buf.finish();

        byte[] result = buf.bytes();
        return new ArithmeticElement(result, (long)(result.length * 8),
            f.alphabet, f.cumFreqs, input.length);
    }

    /* ---- Decode ---- */
    public static int[] decode(ArithmeticElement el, int originalLength) {
        if (originalLength == 0) return new int[0];

        int[] result = new int[originalLength];
        int n = el.alphabet.length;

        BitReader br = new BitReader(el.encodedBytes);
        int low = 0, high = MAX_RANGE - 1;
        int code = br.read16();

        for (int i = 0; i < originalLength; i++) {
            int range = high - low + 1;
            int position = ((code - low + 1) * TF - 1) / range;
            if (position < 0) position = 0;
            if (position >= TF) position = TF - 1;

            int lo2 = 0, hi2 = n - 1, sym = -1;
            while (lo2 <= hi2) {
                int mid = (lo2 + hi2) >>> 1;
                if (position >= el.cumFreqs[mid]) {
                    if (position < el.cumFreqs[mid + 1]) { sym = mid; break; }
                    lo2 = mid + 1;
                } else hi2 = mid - 1;
            }
            if (sym == -1)
                throw new RuntimeException("Arith decode step " + i +
                    ": pos=" + position + " range=" + range + " n=" + n);

            int cf  = el.cumFreqs[sym];
            int cfN = el.cumFreqs[sym + 1];
            high = low + (range * cfN) / TF - 1;
            low  = low + (range * cf ) / TF;

            result[i] = el.alphabet[sym];

            for (;;) {
                if (high < HALF) {
                    low <<= 1; high = (high << 1) | 1;
                    code = ((code << 1) | br.get()) & 0xFFFF;
                } else if (low >= HALF) {
                    low = ((low - HALF) << 1); high = ((high - HALF) << 1) | 1;
                    code = (((code - HALF) << 1) | br.get()) & 0xFFFF;
                } else if (low >= FIRST_QTR && high < THIRD_QTR) {
                    low = ((low - FIRST_QTR) << 1); high = ((high - FIRST_QTR) << 1) | 1;
                    code = (((code - FIRST_QTR) << 1) | br.get()) & 0xFFFF;
                } else break;
            }
        }
        return result;
    }
}