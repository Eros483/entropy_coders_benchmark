import java.io.ByteArrayOutputStream;
import java.util.HashMap;

/**
 * Streaming rANS with 64-bit state, byte-level renormalization. O(N).
 *
 * <h2>Byte-sync</h2>
 * Encoder pushes bytes (LSB-first) when state >= ENC_UPPER.  These bytes
 * are stored in REVERSE order so that the decoder (which decodes LIFO)
 * reads them in the correct push-reverse sequence.
 *
 * ENC_UPPER = 2^24,  DEC_LOWER = 2^16.
 * After encoder pushes: state < 2^16.  Decoder pulls when state < 2^16.
 * Since ENC_UPPER / 256 = 65536 = DEC_LOWER, the thresholds are matched.
 *
 * Output: [8-byte state (BE)] [norm bytes in reverse push order].
 */
public class rANS {

    private static final int R    = 1 << 16;
    private static final long ENC_UPPER = 1L << 24;
    private static final long DEC_LOWER = 1L << 16;

    /* ---- Frequency table ---- */

    private static class FreqInfo {
        int[] alphabet, freqs, cumFreqs;
        HashMap<Integer, Integer> symToIdx;
    }

    @SuppressWarnings("unchecked")
    private static FreqInfo buildFreqInfo(int[] input) {
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
            f.freqs[0] = R; f.cumFreqs[0] = 0; f.cumFreqs[1] = R;
            return f;
        }

        int total = input.length, sum = 0;
        for (int i = 0; i < n; i++) {
            f.freqs[i] = Math.max(1, (int)((long)raw.get(f.alphabet[i]) * R / total));
            sum += f.freqs[i];
        }
        while (sum < R) {
            int best = -1; double bestD = 0;
            for (int i = 0; i < n; i++) {
                double d = (double)raw.get(f.alphabet[i]) * R / total - f.freqs[i];
                if (best == -1 || d > bestD) { bestD = d; best = i; }
            }
            f.freqs[best]++; sum++;
        }
        f.cumFreqs[0] = 0;
        for (int i = 0; i < n; i++) f.cumFreqs[i+1] = f.cumFreqs[i] + f.freqs[i];
        return f;
    }

    /* ---- Encode ---- */

    public static rANSElement encode(int[] input) {
        if (input == null || input.length == 0) return new rANSElement();

        FreqInfo f = buildFreqInfo(input);

        // Collect norm bytes in forward order, then reverse at write time
        java.util.ArrayList<Byte> normList = new java.util.ArrayList<>();
        long state = 0;

        for (int s : input) {
            int idx = f.symToIdx.get(s);

            // Push renorm bytes when state too large
            while (state >= ENC_UPPER) {
                normList.add((byte)(state & 0xFF));
                state >>>= 8;
            }

            // Encode: C(s, state) = floor(state/fs)*R + (state%fs) + cf[s]
            int fr = f.freqs[idx];
            int cs = f.cumFreqs[idx];
            state = (state / fr) * R + (state % fr) + cs;
        }

        // Output: [8-byte state (BE)] [norm bytes in reverse push order]
        // Reverse so decoder reads them in the order the encoder pushed
        // (LIFO consumes the last pushed byte first).
        byte[] data = new byte[8 + normList.size()];
        for (int i = 0; i < 8; i++)
            data[7 - i] = (byte)(state & 0xFF);
        for (int i = 0; i < normList.size(); i++)
            data[8 + normList.size() - 1 - i] = normList.get(i);

        return new rANSElement(data, f.alphabet, f.cumFreqs, input.length);
    }

    /* ---- Decode ---- */

    public static int[] decode(rANSElement el, int originalLength) {
        if (originalLength == 0) return new int[0];

        int[] result = new int[originalLength];
        byte[] data = el.encodedBytes;

        long state = 0;
        for (int i = 0; i < 8; i++)
            state = (state << 8) | (data[i] & 0xFF);

        int ptr = 8;  // reads norm bytes in reversed order (encoder-last first)

        for (int i = originalLength - 1; i >= 0; i--) {
            int sIdx = (int)(state % R);

            int lo = 0, hi = el.alphabet.length - 1, sym = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (sIdx >= el.cumFreqs[mid]) {
                    if (sIdx < el.cumFreqs[mid + 1]) { sym = mid; break; }
                    lo = mid + 1;
                } else hi = mid - 1;
            }
            if (sym == -1)
                throw new RuntimeException("rANS decode step " + i +
                    ": no sym for sIdx=" + sIdx);

            int fr = el.cumFreqs[sym + 1] - el.cumFreqs[sym];
            state = (long)fr * (state / R) + (state % R) - el.cumFreqs[sym];
            result[i] = el.alphabet[sym];

            if (i > 0 && state < DEC_LOWER) {
                if (ptr >= data.length)
                    throw new RuntimeException("rANS decode: out of bytes step " + i);
                state = (state << 8) | (data[ptr] & 0xFF);
                ptr++;
            }
        }

        return result;
    }
}
