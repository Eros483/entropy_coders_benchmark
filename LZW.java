import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class LZW {
    private static final int MAX_DICT_SIZE = 4096;
    private static final int MAX_BIT_WIDTH = 12;

    public static byte[] encode(ArrayList<Character> input) {
        if (input == null || input.isEmpty()) return new byte[0];
        HashMap<String, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dictionary.put(String.valueOf((char) i), i);
        }
        int dictSize = 256;
        int bitWidth = 9;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bitBuffer = 0;
        int bitCount = 0;
        String current = "";
        for (char c : input) {
            String next = current + c;
            if (dictionary.containsKey(next)) {
                current = next;
            } else {
                int code = dictionary.get(current);
                bitBuffer = (bitBuffer << bitWidth) | code;
                bitCount += bitWidth;
                while (bitCount >= 8) {
                    bitCount -= 8;
                    out.write((bitBuffer >> bitCount) & 0xFF);
                }
                if (dictSize < MAX_DICT_SIZE) {
                    dictionary.put(next, dictSize++);
                    if (dictSize > (1 << bitWidth) && bitWidth < MAX_BIT_WIDTH) {
                        bitWidth++;
                    }
                }
                current = String.valueOf(c);
            }
        }
        if (!current.isEmpty()) {
            int code = dictionary.get(current);
            bitBuffer = (bitBuffer << bitWidth) | code;
            bitCount += bitWidth;
            while (bitCount >= 8) {
                bitCount -= 8;
                out.write((bitBuffer >> bitCount) & 0xFF);
            }
        }
        if (bitCount > 0) {
            out.write((bitBuffer << (8 - bitCount)) & 0xFF);
        }
        return out.toByteArray();
    }

    public static ArrayList<Character> decode(byte[] compressed) {
        if (compressed == null || compressed.length == 0) return new ArrayList<>();
        ArrayList<String> dictionary = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            dictionary.add(String.valueOf((char) i));
        }
        int dictSize = 256;
        int bitWidth = 9;
        ArrayList<Character> result = new ArrayList<>();
        int[] byteIdx = {0};
        int[] bitBuffer = {0};
        int[] bitCount = {0};
        int oldCode = readBits(compressed, byteIdx, bitBuffer, bitCount, bitWidth);
        if (oldCode == -1) return result;
        String s = dictionary.get(oldCode);
        for (char c : s.toCharArray()) result.add(c);
        while (true) {
            int newCode = readBits(compressed, byteIdx, bitBuffer, bitCount, bitWidth);
            if (newCode == -1) break;
            String entry;
            if (newCode < dictSize) {
                entry = dictionary.get(newCode);
            } else if (newCode == dictSize) {
                entry = s + s.charAt(0);
            } else {
                break;
            }
            for (char c : entry.toCharArray()) result.add(c);
            if (dictSize < MAX_DICT_SIZE) {
                dictionary.add(s + entry.charAt(0));
                dictSize++;
                if ((dictSize - 1) == (1 << bitWidth) - 1 && bitWidth < MAX_BIT_WIDTH) {
                    bitWidth++;
                }
            }
            s = entry;
        }
        return result;
    }

    private static int readBits(byte[] data, int[] byteIdx, int[] bitBuffer, int[] bitCount, int bitWidth) {
        while (bitCount[0] < bitWidth) {
            if (byteIdx[0] >= data.length) return -1;
            bitBuffer[0] = (bitBuffer[0] << 8) | (data[byteIdx[0]++] & 0xFF);
            bitCount[0] += 8;
        }
        bitCount[0] -= bitWidth;
        return (bitBuffer[0] >> bitCount[0]) & ((1 << bitWidth) - 1);
    }

    public static int[] getCodes(ArrayList<Character> input) {
        HashMap<String, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dictionary.put(String.valueOf((char) i), i);
        }
        int dictSize = 256;
        ArrayList<Integer> codes = new ArrayList<>();
        String current = "";
        for (char c : input) {
            String next = current + c;
            if (dictionary.containsKey(next)) {
                current = next;
            } else {
                codes.add(dictionary.get(current));
                if (dictSize < MAX_DICT_SIZE) {
                    dictionary.put(next, dictSize++);
                }
                current = String.valueOf(c);
            }
        }
        if (!current.isEmpty()) {
            codes.add(dictionary.get(current));
        }
        int[] result = new int[codes.size()];
        for (int i = 0; i < codes.size(); i++) {
            result[i] = codes.get(i);
        }
        return result;
    }
}