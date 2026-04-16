package core;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class LZW {
    private static final int MAX_DICT_SIZE = 4096;
    private static final int MAX_BIT_WIDTH = 12;

    public static byte[] encode(ArrayList<Character> input) {
        if (input == null || input.isEmpty()) return new byte[0];

        HashMap<Long, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) dictionary.put((long) i, i);

        int dictSize = 256;
        int bitWidth = 9;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bitBuffer = 0, bitCount = 0;

        int prefixCode = -1;
        for (char c : input) {
            long key = (prefixCode == -1) ? (long) c : ((long) prefixCode << 16) | c;
            if (dictionary.containsKey(key)) {
                prefixCode = dictionary.get(key);
            } else {
                bitBuffer = (bitBuffer << bitWidth) | prefixCode;
                bitCount += bitWidth;
                while (bitCount >= 8) {
                    bitCount -= 8;
                    out.write((bitBuffer >> bitCount) & 0xFF);
                }

                if (dictSize < MAX_DICT_SIZE) {
                    dictionary.put(key, dictSize++);
                    if (dictSize > (1 << bitWidth) && bitWidth < MAX_BIT_WIDTH) bitWidth++;
                }
                prefixCode = (int) c;
            }
        }

        if (prefixCode != -1) {
            bitBuffer = (bitBuffer << bitWidth) | prefixCode;
            bitCount += bitWidth;
            while (bitCount >= 8) {
                bitCount -= 8;
                out.write((bitBuffer >> bitCount) & 0xFF);
            }
        }
        if (bitCount > 0) out.write((bitBuffer << (8 - bitCount)) & 0xFF);
        return out.toByteArray();
    }

    public static ArrayList<Character> decode(byte[] compressed) {
        if (compressed == null || compressed.length == 0) return new ArrayList<>();

        ArrayList<String> dictionary = new ArrayList<>();
        for (int i = 0; i < 256; i++) dictionary.add(String.valueOf((char) i));

        int dictSize = 256, bitWidth = 9;
        ArrayList<Character> result = new ArrayList<>();
        int[] state = {0, 0, 0}; // idx, buffer, count

        int oldCode = readBits(compressed, state, bitWidth);
        if (oldCode == -1) return result;

        String s = dictionary.get(oldCode);
        for (char c : s.toCharArray()) result.add(c);

        while (true) {
            int newCode = readBits(compressed, state, bitWidth);
            if (newCode == -1) break;

            String entry = (newCode < dictSize) ? dictionary.get(newCode) : s + s.charAt(0);
            for (char c : entry.toCharArray()) result.add(c);

            if (dictSize < MAX_DICT_SIZE) {
                dictionary.add(s + entry.charAt(0));
                dictSize++;
                if ((dictSize - 1) == (1 << bitWidth) - 1 && bitWidth < MAX_BIT_WIDTH) bitWidth++;
            }
            s = entry;
        }
        return result;
    }
    
    public static int[] getCodes(ArrayList<Character> input) {
        if (input == null || input.isEmpty()) return new int[0];
        HashMap<Long, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) dictionary.put((long) i, i);
        int dictSize = 256;
        ArrayList<Integer> codes = new ArrayList<>();
        int prefixCode = -1;
        for (char c : input) {
            long key = (prefixCode == -1) ? (long) c : ((long) prefixCode << 16) | c;
            if (dictionary.containsKey(key)) {
                prefixCode = dictionary.get(key);
            } else {
                codes.add(prefixCode);
                if (dictSize < 4096) dictionary.put(key, dictSize++);
                prefixCode = (int) c;
            }
        }
        if (prefixCode != -1) codes.add(prefixCode);
        return codes.stream().mapToInt(i -> i).toArray();
    }

    private static int readBits(byte[] data, int[] state, int width) {
        while (state[2] < width) {
            if (state[0] >= data.length) return -1;
            state[1] = (state[1] << 8) | (data[state[0]++] & 0xFF);
            state[2] += 8;
        }
        state[2] -= width;
        return (state[1] >> state[2]) & ((1 << width) - 1);
    }
}