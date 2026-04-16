package core;

import java.util.*;
import java.io.*;

public final class BWT {

    public static final int DEFAULT_CHUNK_SIZE = 900_000;

    // ── Chunked encode ──────────────────────────────────────────────────────

    public static File encodeChunkedToFile(ArrayList<Character> input, int chunkSize) throws IOException {
        File tempFile = new File("bwt_encoded.tmp");

        int totalChunks = (int) Math.ceil((double) input.size() / chunkSize);
        int chunkNum = 0;

        System.out.printf("  [BWT] encoding %d chars in %d chunks -> %s%n",
            input.size(), totalChunks, tempFile.getAbsolutePath());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            for (int offset = 0; offset < input.size(); offset += chunkSize) {
                int end = Math.min(offset + chunkSize, input.size());
                chunkNum++;
                System.out.printf("  [BWT] chunk %d/%d (chars %d-%d)%n",
                    chunkNum, totalChunks, offset, end);

                ArrayList<Character> chunk = new ArrayList<>(end - offset + 1);
                for (int i = offset; i < end; i++) chunk.add(input.get(i));
                chunk.add('\0');

                ArrayList<Character> encoded = encode(chunk);
                for (char c : encoded) writer.write(c);
            }
        }

        System.out.printf("  [BWT] encode complete (%d chunks)%n", totalChunks);
        return tempFile;
    }

    public static File encodeChunkedToFile(ArrayList<Character> input) throws IOException {
        return encodeChunkedToFile(input, DEFAULT_CHUNK_SIZE);
    }

    // ── Chunked decode ──────────────────────────────────────────────────────

    public static ArrayList<Character> decodeChunkedFromFile(File encodedFile, int chunkSize) throws IOException {
        ArrayList<Character> result = new ArrayList<>();
        long fileSize = encodedFile.length();
        long bytesRead = 0;
        int chunkNum = 0;

        System.out.printf("  [BWT] decoding from %s (%d bytes)%n",
            encodedFile.getAbsolutePath(), fileSize);

        try (BufferedReader reader = new BufferedReader(new FileReader(encodedFile))) {
            char[] buf = new char[chunkSize + 1];
            int read;
            while ((read = reader.read(buf, 0, chunkSize + 1)) != -1) {
                chunkNum++;
                bytesRead += read;
                System.out.printf("  [BWT] decode chunk %d (%.1f%%)%n",
                    chunkNum, (bytesRead * 100.0) / fileSize);

                ArrayList<Character> chunk = new ArrayList<>(read);
                for (int i = 0; i < read; i++) chunk.add(buf[i]);

                ArrayList<Character> decoded = decode(chunk);
                if (!decoded.isEmpty() && decoded.get(decoded.size() - 1) == '\0')
                    decoded.remove(decoded.size() - 1);

                result.addAll(decoded);
            }
        }

        System.out.printf("  [BWT] decode complete (%d chunks)%n", chunkNum);
        return result;
    }

    public static ArrayList<Character> decodeChunkedFromFile(File encodedFile) throws IOException {
        return decodeChunkedFromFile(encodedFile, DEFAULT_CHUNK_SIZE);
    }

    // ── Single-chunk primitives ─────────────────────────────────────────────

    public static ArrayList<Character> encode(ArrayList<Character> str) {
        ArrayList<Character> BWT = new ArrayList<>();
        int[] SA = SuffixArray.compute(str);

        for (int i = 0; i < str.size(); i++) {
            if (SA[i] > 0)
                BWT.add(str.get(SA[i] - 1));
            else
                BWT.add(str.get(str.size() - 1));
        }
        return BWT;
    }

    public static ArrayList<Character> decode(ArrayList<Character> bwt) {
        int[] R = new int[bwt.size()];
        HashMap<Character, Integer> C = new HashMap<>();

        TreeMap<Character, Integer> freq = new TreeMap<>();
        for (int i = 0; i < bwt.size(); i++) {
            freq.merge(bwt.get(i), 1, Integer::sum);
            R[i] = freq.get(bwt.get(i));
        }

        int counter = 0;
        for (Map.Entry<Character, Integer> entry : freq.entrySet()) {
            C.put(entry.getKey(), counter);
            counter += entry.getValue();
        }

        int x = 0;
        for (int i = 0; i < bwt.size(); i++) {
            if (bwt.get(i) == '\0') { x = i; break; }
        }

        char[] original = new char[bwt.size()];
        for (int i = bwt.size() - 1; i >= 0; i--) {
            original[i] = bwt.get(x);
            x = C.get(bwt.get(x)) + R[x] - 1;
        }

        ArrayList<Character> og = new ArrayList<>(original.length);
        for (char c : original) og.add(c);
        return og;
    }
}