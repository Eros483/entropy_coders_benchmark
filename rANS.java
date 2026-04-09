import java.util.HashMap;

/**
 * Streaming rANS with 64-bit state, byte-level renormalization. O(N).
 */
public class rANS {

    private static final int R    = 1 << 16;
    private static final long L   = 1L << 24; // Lower bound for state

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
        java.util.ArrayList<Byte> normList = new java.util.ArrayList<>();

        // Initial state MUST be the lower bound to maintain symmetry
        long state = L;

        for (int s : input) {
            int idx = f.symToIdx.get(s);
            int fr = f.freqs[idx];
            int cs = f.cumFreqs[idx];

            // Push renorm bytes: limit depends on the frequency of the current symbol
            long limit = (L >>> 8) * fr;
            while (state >= limit) {
                normList.add((byte)(state & 0xFF));
                state >>>= 8;
            }

            // Encode: C(s, state) = (state / fr) * R + (state % fr) + cs
            state = (state / fr) * R + (state % fr) + cs;
        }

        byte[] data = new byte[8 + normList.size()];
        
        // Write 64-bit state (BE). Fixed: shifting tempState to capture all bytes.
        long tempState = state;
        for (int i = 0; i < 8; i++) {
            data[7 - i] = (byte)(tempState & 0xFF);
            tempState >>>= 8;
        }
        
        // Output norm bytes in reverse push order (LIFO)
        for (int i = 0; i < normList.size(); i++) {
            data[8 + normList.size() - 1 - i] = normList.get(i);
        }

        return new rANSElement(data, f.alphabet, f.cumFreqs, input.length);
    }

    /* ---- Decode ---- */

    public static int[] decode(rANSElement el, int originalLength) {
        if (originalLength == 0) return new int[0];

        int[] result = new int[originalLength];
        byte[] data = el.encodedBytes;

        long state = 0;
        for (int i = 0; i < 8; i++) {
            state = (state << 8) | (data[i] & 0xFF);
        }

        int ptr = 8; 

        for (int i = originalLength - 1; i >= 0; i--) {
            int sIdx = (int)(state % R);

            int lo = 0, hi = el.alphabet.length - 1, sym = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (sIdx >= el.cumFreqs[mid]) {
                    if (sIdx < el.cumFreqs[mid + 1]) { sym = mid; break; }
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            if (sym == -1)
                throw new RuntimeException("rANS decode step " + i + ": no sym for sIdx=" + sIdx);

            int fr = el.cumFreqs[sym + 1] - el.cumFreqs[sym];
            state = (long)fr * (state / R) + (state % R) - el.cumFreqs[sym];
            result[i] = el.alphabet[sym];

            // Renormalize: MUST be a while loop. Extremely rare symbols might need >1 byte read
            while (state < L) {
                if (ptr >= data.length) {
                    throw new RuntimeException("rANS decode: out of bytes step " + i);
                }
                state = (state << 8) | (data[ptr] & 0xFF);
                ptr++;
            }
        }

        return result;
    }
}