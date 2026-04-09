import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Standalone test suite for rANS encoding and the LZ77+rANS pipeline.
 *
 * Does not modify Test.java or Master.java — run independently with:
 *   javac -d bin *.java
 *   java -cp bin TestRANS
 */
public class TestRANS {

    /* ============================================================
     *  UNIT TESTS
     * ============================================================ */

    /**
     * Verifies that rANS correctly encodes and decodes several hand-crafted
     * integer streams by round-tripping through encode → decode and checking
     * exact equality.
     */
    private static void testRANSCorrectness() throws Exception {
        System.out.println("*** rANS Unit Tests ***\n");

        // Test 1: simple repeating pattern
        testCase("Simple pattern", new int[]{1, 2, 3, 1, 2, 3, 1, 2, 3});

        // Test 2: all identical (edge case: alphabet size = 1)
        testCase("Single symbol", new int[]{7, 7, 7, 7, 7});

        // Test 3: every symbol is distinct
        testCase("All distinct", new int[]{10, 20, 30, 40, 50});

        // Test 4: ASCII-like values (similar to what LZ77 "next" stream would have)
        testCase("ASCII range", new int[]{109, 105, 115, 115, 105, 115, 115, 105, 112, 112, 105});

        // Test 5: larger stream with skewed distribution
        int[] skewed = new int[500];
        for (int i = 0; i < skewed.length; i++)
            skewed[i] = i % 5 == 0 ? 99 : 1;
        testCase("Skewed distribution", skewed);

        // Test 6: LZ77-like delta values (many small values, few large)
        int[] deltas = new int[]{0, 1, 1, 2, 3, 1, 2, 3, 1, 1, 2, 1, 1, 3, 2, 1};
        testCase("LZ77-style deltas", deltas);

        // Test 7: empty (edge case)
        System.out.println("Test: empty input");
        int[] empty = new int[0];
        rANSElement el = rANS.encode(empty);
        int[] emptyDecoded = rANS.decode(el, el.originalLength);
        System.out.println("  Encoded bytes: " + el.encodedBytes.length);
        System.out.println("  Decoded: " + Arrays.toString(emptyDecoded));
        HelperFunctions.verifyEquality(
            intsToList(empty),
            intsToList(emptyDecoded));
        System.out.println("  PASSED\n");

        System.out.println("All rANS unit tests passed!\n");
    }

    private static void testCase(String name, int[] input) throws Exception {
        System.out.println("Test: " + name);
        rANSElement encoded = rANS.encode(input);
        System.out.println("  Input length:      " + input.length);
        System.out.println("  Alphabet size:     " + encoded.alphabet.length);
        System.out.println("  Encoded bytes:     " + encoded.encodedBytes.length);
        System.out.println("  Bits per symbol:   " + String.format("%.2f", (encoded.encodedBytes.length * 8.0) / input.length));

        int[] decoded = rANS.decode(encoded, encoded.originalLength);

        HelperFunctions.verifyEquality(intsToList(input), intsToList(decoded));
        System.out.println("  PASSED\n");
    }


    /* ============================================================
     *  PIPELINE TEST: LZ77 + rANS
     * ============================================================ */

    /**
     * Mimics Master.lz77() flow but replaces Huffman with rANS:
     *   1. Read a file from Data/
     *   2. LZ77 encode → (delta, length, next) triplets
     *   3. rANS encode each stream (delta, length, next separately)
     *   4. Write compressed files to disk
     *   5. Read compressed files back
     *   6. rANS decode each stream
     *   7. LZ77 decode → verify against original
     */
    private static void testLZ77rANSPipeline() throws Exception {
        System.out.println("*** LZ77 + rANS Pipeline Test ***\n");

        // Use the first available data file from the list.
        // If Data/ doesn't exist, create a temporary small test file.
        String testFilePath = findOrCreateTestFile();

        System.out.println("Using test file: " + testFilePath);
        System.out.println();

        // ====== COMPRESSION SIDE ======
        long startTime = System.currentTimeMillis();

        // 1. Read original file
        ArrayList<Character> originalData = IOHelper.readFile(testFilePath);
        System.out.println("Original file loaded: " + originalData.size() + " chars");

        // 2. LZ77 encode
        LZ77Element lz77 = LZ77.encode(originalData);
        System.out.println("LZ77 triplets: " + lz77.delta.size());

        // 3. rANS encode each stream
        int[] deltaInts = listToInts(lz77.delta);
        int[] lengthInts = listToInts(lz77.length);
        int[] nextInts = listToInts(HelperFunctions.charsToInts(lz77.next));

        System.out.println("rANS encoding delta stream...");
        rANSElement ransDelta = rANS.encode(deltaInts);

        System.out.println("rANS encoding length stream...");
        rANSElement ransLength = rANS.encode(lengthInts);

        System.out.println("rANS encoding next stream...");
        rANSElement ransNext = rANS.encode(nextInts);

        // 4. Write compressed files to disk
        String outDir = FilePaths.DATA_DIRECTORY + "OutputJava/rANS_test/";
        new java.io.File(outDir).mkdirs();

        String deltaPath = outDir + "delta.rans";
        String lengthPath = outDir + "length.rans";
        String nextPath = outDir + "next.rans";

        rANSIOHelper.write(ransDelta, deltaPath);
        rANSIOHelper.write(ransLength, lengthPath);
        rANSIOHelper.write(ransNext, nextPath);

        long compressionTime = System.currentTimeMillis() - startTime;
        System.out.printf("Compression took %.2f seconds\n", compressionTime / 1000.0);

        // Report sizes
        long originalSize = IOHelper.filesize(testFilePath);
        long compressedSize = IOHelper.filesize(deltaPath) +
                              IOHelper.filesize(lengthPath) +
                              IOHelper.filesize(nextPath);
        System.out.println("Original size:    " + originalSize + " bytes");
        System.out.println("Compressed size:  " + compressedSize + " bytes");
        System.out.printf("Compression ratio: %.2f\n\n", originalSize / (double) compressedSize);

        // ====== DECOMPRESSION SIDE ======
        startTime = System.currentTimeMillis();

        // 5. Read compressed files back
        System.out.println("Reading compressed files...");
        rANSElement fromFile_Delta = rANSIOHelper.read(deltaPath);
        rANSElement fromFile_Length = rANSIOHelper.read(lengthPath);
        rANSElement fromFile_Next = rANSIOHelper.read(nextPath);

        // 6. rANS decode each stream
        System.out.println("rANS decoding delta stream...");
        int[] decodedDelta = rANS.decode(fromFile_Delta, fromFile_Delta.originalLength);

        System.out.println("rANS decoding length stream...");
        int[] decodedLength = rANS.decode(fromFile_Length, fromFile_Length.originalLength);

        System.out.println("rANS decoding next stream...");
        int[] decodedNextInts = rANS.decode(fromFile_Next, fromFile_Next.originalLength);
        ArrayList<Character> decodedNext = HelperFunctions.intsToChars(intsToList(decodedNextInts));

        // 7. LZ77 decode
        System.out.println("LZ77 decoding...");
        LZ77Element decodedTriplet = new LZ77Element(
            intsToList(decodedDelta),
            intsToList(decodedLength),
            decodedNext
        );
        ArrayList<Character> decodedData = LZ77.decode(decodedTriplet);

        long decompressionTime = System.currentTimeMillis() - startTime;
        System.out.printf("Decompression took %.2f seconds\n", decompressionTime / 1000.0);

        // 8. Verify
        System.out.println("Verifying round-trip...");
        HelperFunctions.verifyEquality(originalData, decodedData);
        System.out.println("\nLZ77 + rANS pipeline: SUCCESS — decoded data matches original!");
    }


    /* ============================================================
     *  HELPERS
     * ============================================================ */

    /**
     * Finds the first existing file from Data/ using FilePaths.DATA,
     * or creates a small inline test file if none exist.
     */
    private static String findOrCreateTestFile() {
        for (int i = 0; i < FilePaths.NUM_DATA; i++) {
            String path = FilePaths.DATA_DIRECTORY + FilePaths.DATA[i];
            java.io.File f = new java.io.File(path);
            if (f.exists())
                return path;
        }

        // No data files available — create a temporary small test file.
        String tempPath = FilePaths.DATA_DIRECTORY + "rans_temp_test.txt";
        new java.io.File(FilePaths.DATA_DIRECTORY).mkdirs();
        String testContent = "mississippimissississispsispmississippimissississispsisp";
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(tempPath), testContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary test file", e);
        }
        return tempPath;
    }

    // --- ArrayList <-> int[] conversions ---

    private static int[] listToInts(ArrayList<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++)
            arr[i] = list.get(i);
        return arr;
    }

    private static ArrayList<Integer> intsToList(int[] arr) {
        ArrayList<Integer> list = new ArrayList<>(arr.length);
        for (int v : arr)
            list.add(v);
        return list;
    }


    /* ============================================================
     *  MAIN
     * ============================================================ */

    public static void main(String[] args) throws Exception {
        testRANSCorrectness();
        testLZ77rANSPipeline();
    }
}
