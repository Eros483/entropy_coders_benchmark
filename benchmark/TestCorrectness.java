package benchmark;

import java.util.ArrayList;
import core.*;
import compress.*;

/**
 * Correctness tests for all lossless compressors and entropy coders.
 */
public class TestCorrectness {

    private static int passed = 0;
    private static int failed = 0;

    /* ============================================================
     *  LOAD TEST FILES
     * ============================================================ */

    private static final String[] TEST_FILES = {
        "tiny.txt",
        "bible.txt",
        "C.elegans.fna"
    };

    private static ArrayList<String> findTestFiles() {
        ArrayList<String> files = new ArrayList<>();
        for (String name : TEST_FILES) {
            String path = FilePaths.DATA_DIRECTORY + name;
            if (new java.io.File(path).exists())
                files.add(path);
        }
        return files;
    }

    /* ============================================================
     *  ENTROPY CODERS
     * ============================================================ */

    private static void testHuffman(ArrayList<String> files) throws Exception {
        System.out.println("--- Huffman ---");
        for (String path : files) {
            String name = new java.io.File(path).getName();
            ArrayList<Character> chars = IOHelper.readFile(path);
            ArrayList<Integer> input = HelperFunctions.charsToInts(chars);
            HuffmanElement encoded = Huffman.encode(input);
            ArrayList<Integer> decoded = Huffman.decode(encoded);
            verify("Huffman/" + name, input, decoded);
        }
    }

    private static void testArithmetic(ArrayList<String> files) throws Exception {
        System.out.println("--- Arithmetic ---");
        for (String path : files) {
            String name = new java.io.File(path).getName();
            ArrayList<Character> chars = IOHelper.readFile(path);
            int[] input = listToInts(HelperFunctions.charsToInts(chars));
            ArithmeticElement encoded = Arithmetic.encode(input);
            int[] decoded = Arithmetic.decode(encoded, encoded.originalLength);
            verify("Arithmetic/" + name, intsToList(input), intsToList(decoded));
        }
    }

    private static void testRANS(ArrayList<String> files) throws Exception {
        System.out.println("--- rANS ---");
        for (String path : files) {
            String name = new java.io.File(path).getName();
            ArrayList<Character> chars = IOHelper.readFile(path);
            int[] input = listToInts(HelperFunctions.charsToInts(chars));
            rANSElement encoded = rANS.encode(input);
            int[] decoded = rANS.decode(encoded, encoded.originalLength);
            verify("rANS/" + name, intsToList(input), intsToList(decoded));
        }
    }

    /* ============================================================
     *  LOSSLESS COMPRESSORS
     * ============================================================ */

    private static void testLZ77(ArrayList<String> files) throws Exception {
        System.out.println("--- LZ77 ---");
        for (String path : files) {
            String name = new java.io.File(path).getName();
            ArrayList<Character> original = IOHelper.readFile(path);
            LZ77Element encoded = LZ77.encode(original);
            ArrayList<Character> decoded = LZ77.decode(encoded);
            verify("LZ77/" + name, original, decoded);
        }
    }

    private static void testLZSS(ArrayList<String> files) throws Exception {
        System.out.println("--- LZSS ---");
        for (String path : files) {
            String name = new java.io.File(path).getName();
            ArrayList<Character> original = IOHelper.readFile(path);
            LZSSElement encoded = LZSS.encode(original);
            ArrayList<Character> decoded = LZSS.decode(encoded);
            verify("LZSS/" + name, original, decoded);
        }
    }

    private static void testLZ78(ArrayList<String> files) throws Exception {
        System.out.println("--- LZ78 ---");
        for (String path : files) {
            String name = new java.io.File(path).getName();
            ArrayList<Character> original = IOHelper.readFile(path);
            LZ78Element encoded = LZ78.encode(original);
            ArrayList<Character> decoded = LZ78.decode(encoded);
            verify("LZ78/" + name, original, decoded);
        }
    }

    private static void testLZW(ArrayList<String> files) throws Exception {
        System.out.println("--- LZW ---");
        for (String path : files) {
            String name = new java.io.File(path).getName();
            ArrayList<Character> original = IOHelper.readFile(path);
            byte[] encoded = LZW.encode(original);
            ArrayList<Character> decoded = LZW.decode(encoded);
            verify("LZW/" + name, original, decoded);
        }
    }

    private static void testBWTMTF(ArrayList<String> files) throws Exception {
        System.out.println("--- BWT + MTF ---");
        for (String path : files) {
            String name = new java.io.File(path).getName();
            ArrayList<Character> original = IOHelper.readFile(path);
            ArrayList<Character> bwtEncoded = BWT.encode(original);
            MTFElement mtfEncoded = MTF.encode(bwtEncoded);
            ArrayList<Character> mtfDecoded = MTF.decode(mtfEncoded);
            ArrayList<Character> decoded = BWT.decode(mtfDecoded);
            verify("BWT+MTF/" + name, original, decoded);
        }
    }

    /* ============================================================
     *  VERIFY HELPER
     * ============================================================ */

    private static <T> void verify(String label, ArrayList<T> expected, ArrayList<T> actual) {
        try {
            HelperFunctions.verifyEquality(expected, actual);
            System.out.println("  PASSED: " + label);
            passed++;
        } catch (Exception e) {
            System.out.println("  FAILED: " + label + " — " + e.getMessage());
            failed++;
        }
    }

    /* ============================================================
     *  CONVERSION HELPERS
     * ============================================================ */

    private static int[] listToInts(ArrayList<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private static ArrayList<Integer> intsToList(int[] arr) {
        ArrayList<Integer> list = new ArrayList<>(arr.length);
        for (int v : arr) list.add(v);
        return list;
    }

    /* ============================================================
     *  MAIN
     * ============================================================ */

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("  Correctness Test Suite");
        System.out.println("========================================\n");

        ArrayList<String> files = findTestFiles();
        if (files.isEmpty()) {
            System.out.println("No data files found in " + FilePaths.DATA_DIRECTORY);
            System.exit(1);
        }
        System.out.println("Found " + files.size() + " test file(s)\n");

        testHuffman(files);
        testArithmetic(files);
        testRANS(files);
        testLZ77(files);
        testLZSS(files);
        testLZ78(files);
        testLZW(files);
        testBWTMTF(files);

        System.out.println("\n========================================");
        System.out.println("  Results: " + passed + " passed, " + failed + " failed");
        System.out.println("========================================");

        if (failed > 0) System.exit(1);
    }
}
