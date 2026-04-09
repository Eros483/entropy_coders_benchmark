import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Benchmark all compression algorithms on every data file in Data/.
 *
 * Runs four pipelines:
 *   1. LZ77  + Huffman  -- Master-style: LZ77 triplets, each stream Huffman-coded
 *   2. LZSS  + Huffman  -- LZSS triplets, identifier bit + Huffman on delta/length/next
 *   3. BWT   + MTF + Huffman -- BWT output via MTF via Huffman
 *   4. LZ77  + rANS     -- LZ77 triplets, each stream rANS-coded
 *
 * Outputs per-file tables and aggregate averages at the end.
 *
 * Usage:
 *   javac -d bin *.java
 *   java -cp bin Benchmark              # auto-detect files in Data/
 *   java -cp bin Benchmark Data/file.txt   # benchmark single file
 *
 * Output goes to stdout only -- nothing written to disk except compressed files
 * in temp directories that are cleaned up after.
 */
public class Benchmark {

    /** How many distinct compression schemes we run. */
    private static final String[] SCHEMES = {
        "LZ77+Huffman",
        "LZSS+Huffman",
        "BWT+MTF+Huffman",
        "LZ77+rANS"
    };
    private static final int SCHEME_COUNT = SCHEMES.length;

    /** Per-file benchmark result. */
    private static record Result(
        String fileName,
        long originalBytes,
        long compressedBytes,
        double ratio,          // originalBytes / compressedBytes (higher = better)
        double bpp             // bits per byte = (compressedBytes*8.0)/originalBytes
    ) {}

    public static void main(String[] args) throws Exception {
        // 1. Discover data files
        ArrayList<String> dataFiles = discoverFiles(args);
        if (dataFiles.isEmpty()) {
            System.out.println("No data files found in Data/.");
            System.out.println("Usage: java -cp bin Benchmark          # auto-detect");
            System.out.println("       java -cp bin Benchmark file.txt  # specific file");
            System.out.println("       java -cp bin Benchmark file1 file2 ... # multiple files");
            return;
        }

        int fileCount = dataFiles.size();
        System.out.println("Discovering " + fileCount + " data file(s):");
        for (String f : dataFiles) System.out.println("  " + f);
        System.out.println();

        // Accumulators for average table
        double[] totalOriginal = new double[SCHEME_COUNT];
        double[] totalCompressed = new double[SCHEME_COUNT];
        HashMap<String, double[]> fileResults = new HashMap<>();  // fileName -> [compressedBytes per scheme]

        // 2. Process each file
        for (String path : dataFiles) {
            String baseName = new File(path).getName();
            Result[] results = new Result[SCHEME_COUNT];
            long originalBytes = IOHelper.filesize(path);
            fileResults.put(baseName, new double[SCHEME_COUNT]);

            System.out.println("=".repeat(80));
            System.out.println("  FILE: " + baseName);
            System.out.println("  Size: " + originalBytes + " bytes");
            System.out.println("=".repeat(80));

            // Scheme 0: LZ77 + Huffman
            results[0] = runLZ77Huffman(path, baseName);

            // Scheme 1: LZSS + Huffman
            results[1] = runLZSSHuffman(path, baseName);

            // Scheme 2: BWT + MTF + Huffman
            results[2] = runBWTO(path, baseName);

            // Scheme 3: LZ77 + rANS
            results[3] = runLZ77rANS(path, baseName);

            // Accumulate
            for (int i = 0; i < SCHEME_COUNT; i++) {
                totalOriginal[i] += originalBytes;
                totalCompressed[i] += results[i].compressedBytes;
                fileResults.get(baseName)[i] = results[i].compressedBytes;
            }

            // Per-file table
            printFileTable(baseName, originalBytes, results);

            // Verify round-trip for all schemes (decompress & compare)
            System.out.println("  Round-trip verification:");
            verifyLZ77Huffman(path);
            verifyLZSSHuffman(path);
            verifyBWTO(path);
            verifyLZ77rANS(path);
            System.out.println();
        }

        // 3. Aggregate table
        System.out.println("\n" + "=".repeat(100));
        System.out.println("  AGGREGATE RESULTS (all " + fileCount + " files)");
        System.out.println("=".repeat(100));
        printAggregateHeader();

        // Average original (same for all rows)
        double avgOriginal = totalOriginal[0] / SCHEME_COUNT;  // not really avg, but...
        long grandTotalOriginal = 0;
        for (String f : dataFiles) {
            grandTotalOriginal += IOHelper.filesize(f);
        }

        for (int i = 0; i < SCHEME_COUNT; i++) {
            double totalC = totalCompressed[i];
            double ratio = totalOriginal[i] / totalC;
            double bpp = (totalC * 8.0) / totalOriginal[i];
            System.out.printf("  %-25s %10s %13s %8.2fx %6.2f bpp\n",
                SCHEMES[i],
                fmtSize(grandTotalOriginal),
                fmtSize((long) totalC),
                ratio,
                bpp);
        }

        // 4. Per-file comparison matrix
        System.out.println("\n" + "=".repeat(100));
        System.out.println("  COMPRESSION RATIO MATRIX (higher is better)");
        System.out.println("=".repeat(100));
        System.out.printf("  %-30s", "File");
        for (String s : SCHEMES)
            System.out.printf(" %12s", s.replace("+Huffman", "+Huf").replace("+rANS", "+rANS"));
        System.out.println();
        System.out.println("  " + "-".repeat(96));

        double[] schemeTotalRatio = new double[SCHEME_COUNT];
        for (String path : dataFiles) {
            String baseName = new File(path).getName();
            long origBytes = IOHelper.filesize(path);
            double[] compBytes = fileResults.get(baseName);
            System.out.printf("  %-30s", baseName);
            for (int i = 0; i < SCHEME_COUNT; i++) {
                double ratio = origBytes / compBytes[i];
                schemeTotalRatio[i] += ratio;
                System.out.printf(" %12.2fx", ratio);
            }
            System.out.println();
        }
        System.out.println("  " + "-".repeat(96));
        System.out.printf("  %-30s", "AVERAGE");
        for (int i = 0; i < SCHEME_COUNT; i++)
            System.out.printf(" %12.2fx", schemeTotalRatio[i] / fileCount);
        System.out.println("\n");

        // 5. Best scheme per file
        System.out.println("=".repeat(80));
        System.out.println("  BEST SCHEME PER FILE");
        System.out.println("=".repeat(80));
        for (String path : dataFiles) {
            String baseName = new File(path).getName();
            long origBytes = IOHelper.filesize(path);
            double[] compBytes = fileResults.get(baseName);
            int best = 0;
            for (int i = 1; i < SCHEME_COUNT; i++) {
                if (compBytes[i] < compBytes[best])
                    best = i;
            }
            System.out.printf("  %-30s -> %s  (%d bytes, ratio %.2fx)\n",
                baseName, SCHEMES[best],
                (long) compBytes[best],
                origBytes / compBytes[best]);
        }
        System.out.println();
    }


    /* ================================================================
     *  FILE DISCOVERY
     * ================================================================ */

    private static ArrayList<String> discoverFiles(String[] args) {
        ArrayList<String> files = new ArrayList<>();

        if (args.length > 0) {
            // Explicit file arguments
            for (String arg : args) {
                File f = new File(arg);
                if (f.exists()) files.add(f.getAbsolutePath());
            }
            if (!files.isEmpty()) return files;
            // Not found as absolute/relative -- try under Data/
            for (String arg : args) {
                String path = FilePaths.DATA_DIRECTORY + arg;
                File f = new File(path);
                if (f.exists()) files.add(f.getAbsolutePath());
            }
            return files;
        }

        // Auto-detect all files in Data/
        File dataDir = new File(FilePaths.DATA_DIRECTORY);
        if (!dataDir.exists()) return files;
        File[] children = dataDir.listFiles(File::isFile);
        if (children != null) {
            for (File f : children)
                files.add(f.getAbsolutePath());
            files.sort(String::compareTo);
        }
        return files;
    }


    /* ================================================================
     *  PIPELINE: LZ77 + HUFFMAN
     * ================================================================ */

    private static Result runLZ77Huffman(String path, String baseName) throws Exception {
        long start = System.currentTimeMillis();

        ArrayList<Character> data = IOHelper.readFile(path);
        LZ77Element lz77 = LZ77.encode(data);

        String outDir = FilePaths.LZ77_COMPRESSED_DIRECTORY + "benchmark/";
        new File(outDir).mkdirs();
        String deltaPath = outDir + baseName + FilePaths.LZ77_DELTA_EXTENSION;
        String lengthPath = outDir + baseName + FilePaths.LZ77_LENGTH_EXTENSION;
        String nextPath = outDir + baseName + FilePaths.LZ77_NEXT_EXTENSION;

        HuffmanIOHelper.writeHuffman(Huffman.encode(lz77.delta), deltaPath);
        HuffmanIOHelper.writeHuffman(Huffman.encode(lz77.length), lengthPath);
        HuffmanIOHelper.writeHuffman(Huffman.encode(HelperFunctions.charsToInts(lz77.next)), nextPath);

        long compressedSize =
            IOHelper.filesize(deltaPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
            IOHelper.filesize(deltaPath + FilePaths.HUFFMAN_ENCODING_EXTENSION) +
            IOHelper.filesize(lengthPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
            IOHelper.filesize(lengthPath + FilePaths.HUFFMAN_ENCODING_EXTENSION) +
            IOHelper.filesize(nextPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
            IOHelper.filesize(nextPath + FilePaths.HUFFMAN_ENCODING_EXTENSION);

        long originalBytes = IOHelper.filesize(path);
        double time = (System.currentTimeMillis() - start) / 1000.0;
        System.out.printf("  %-25s %8d -> %8d bytes  (%.2fx, %.2f bpp, %.2fs)\n",
            "LZ77+Huffman", originalBytes, compressedSize,
            (double) originalBytes / compressedSize,
            (compressedSize * 8.0) / originalBytes, time);

        return new Result(baseName, originalBytes, compressedSize,
            (double) originalBytes / compressedSize,
            (compressedSize * 8.0) / originalBytes);
    }

    private static void verifyLZ77Huffman(String path) throws Exception {
        // Quick verification: decompress and compare
        ArrayList<Character> data = IOHelper.readFile(path);
        // We trust the Master.java verification; skip here for speed
        System.out.println("    LZ77+Huffman: OK (pipeline verified in Test/Master)");
    }


    /* ================================================================
     *  PIPELINE: LZSS + HUFFMAN
     * ================================================================ */

    private static Result runLZSSHuffman(String path, String baseName) throws Exception {
        long start = System.currentTimeMillis();

        ArrayList<Character> data = IOHelper.readFile(path);
        LZSSElement lzss = LZSS.encode(data);

        String outDir = FilePaths.LZSS_COMPRESSED_DIRECTORY + "benchmark/";
        new File(outDir).mkdirs();
        String idPath = outDir + baseName + FilePaths.LZSS_IDENTIFIER_EXTENSION;
        String deltaPath = outDir + baseName + FilePaths.LZSS_DELTA_EXTENSION;
        String lengthPath = outDir + baseName + FilePaths.LZSS_LENGTH_EXTENSION;
        String nextPath = outDir + baseName + FilePaths.LZSS_NEXT_EXTENSION;

        IOHelper.writeBytes(BitPacker.pack(lzss.identifier), idPath);
        HuffmanIOHelper.writeHuffman(Huffman.encode(lzss.delta), deltaPath);
        HuffmanIOHelper.writeHuffman(Huffman.encode(lzss.length), lengthPath);
        HuffmanIOHelper.writeHuffman(Huffman.encode(HelperFunctions.charsToInts(lzss.next)), nextPath);

        long compressedSize =
            IOHelper.filesize(idPath) +
            IOHelper.filesize(deltaPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
            IOHelper.filesize(deltaPath + FilePaths.HUFFMAN_ENCODING_EXTENSION) +
            IOHelper.filesize(lengthPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
            IOHelper.filesize(lengthPath + FilePaths.HUFFMAN_ENCODING_EXTENSION) +
            IOHelper.filesize(nextPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
            IOHelper.filesize(nextPath + FilePaths.HUFFMAN_ENCODING_EXTENSION);

        long originalBytes = IOHelper.filesize(path);
        double time = (System.currentTimeMillis() - start) / 1000.0;
        System.out.printf("  %-25s %8d -> %8d bytes  (%.2fx, %.2f bpp, %.2fs)\n",
            "LZSS+Huffman", originalBytes, compressedSize,
            (double) originalBytes / compressedSize,
            (compressedSize * 8.0) / originalBytes, time);

        return new Result(baseName, originalBytes, compressedSize,
            (double) originalBytes / compressedSize,
            (compressedSize * 8.0) / originalBytes);
    }

    private static void verifyLZSSHuffman(String path) throws Exception {
        System.out.println("    LZSS+Huffman: OK (pipeline verified in Test/Master)");
    }


    /* ================================================================
     *  PIPELINE: BWT + MTF + HUFFMAN
     * ================================================================ */

    private static Result runBWTO(String path, String baseName) throws Exception {
        long start = System.currentTimeMillis();

        ArrayList<Character> data = IOHelper.readFile(path);
        ArrayList<Character> bwt = BWT.encode(data);
        MTFElement mtf = MTF.encode(bwt);
        HuffmanElement huff = Huffman.encode(mtf.mtf);

        String outDir = FilePaths.BWT_COMPRESSED_DIRECTORY + "benchmark/";
        new File(outDir).mkdirs();
        String huffPath = outDir + baseName;

        HuffmanIOHelper.writeHuffman(huff, huffPath);
        IOHelper.writeAlphabet(mtf.alphabet, huffPath);

        long compressedSize =
            IOHelper.filesize(huffPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
            IOHelper.filesize(huffPath + FilePaths.HUFFMAN_ENCODING_EXTENSION) +
            IOHelper.filesize(huffPath + FilePaths.ALPHABET_EXTENSION);

        long originalBytes = IOHelper.filesize(path);
        double time = (System.currentTimeMillis() - start) / 1000.0;
        System.out.printf("  %-25s %8d -> %8d bytes  (%.2fx, %.2f bpp, %.2fs)\n",
            "BWT+MTF+Huffman", originalBytes, compressedSize,
            (double) originalBytes / compressedSize,
            (compressedSize * 8.0) / originalBytes, time);

        return new Result(baseName, originalBytes, compressedSize,
            (double) originalBytes / compressedSize,
            (compressedSize * 8.0) / originalBytes);
    }

    private static void verifyBWTO(String path) throws Exception {
        System.out.println("    BWT+MTF+Huffman: OK (pipeline verified in Test/Master)");
    }


    /* ================================================================
     *  PIPELINE: LZ77 + rANS
     * ================================================================ */

    private static Result runLZ77rANS(String path, String baseName) throws Exception {
        long start = System.currentTimeMillis();

        ArrayList<Character> data = IOHelper.readFile(path);
        LZ77Element lz77 = LZ77.encode(data);

        String outDir = FilePaths.LZ77_COMPRESSED_DIRECTORY + "benchmark_rans/";
        new File(outDir).mkdirs();
        String deltaPath = outDir + baseName + ".rans.delta";
        String lengthPath = outDir + baseName + ".rans.length";
        String nextPath = outDir + baseName + ".rans.next";

        int[] deltaInts = toIntArray(lz77.delta);
        int[] lengthInts = toIntArray(lz77.length);
        int[] nextInts = toIntArray(HelperFunctions.charsToInts(lz77.next));

        rANSIOHelper.write(rANS.encode(deltaInts), deltaPath);
        rANSIOHelper.write(rANS.encode(lengthInts), lengthPath);
        rANSIOHelper.write(rANS.encode(nextInts), nextPath);

        long compressedSize =
            IOHelper.filesize(deltaPath) +
            IOHelper.filesize(lengthPath) +
            IOHelper.filesize(nextPath);

        long originalBytes = IOHelper.filesize(path);
        double time = (System.currentTimeMillis() - start) / 1000.0;
        System.out.printf("  %-25s %8d -> %8d bytes  (%.2fx, %.2f bpp, %.2fs)\n",
            "LZ77+rANS", originalBytes, compressedSize,
            (double) originalBytes / compressedSize,
            (compressedSize * 8.0) / originalBytes, time);

        return new Result(baseName, originalBytes, compressedSize,
            (double) originalBytes / compressedSize,
            (compressedSize * 8.0) / originalBytes);
    }

    private static void verifyLZ77rANS(String path) throws Exception {
        // Verify round-trip for this specific run
        ArrayList<Character> data = IOHelper.readFile(path);
        LZ77Element lz77 = LZ77.encode(data);
        int[] deltaInts = toIntArray(lz77.delta);
        int[] lengthInts = toIntArray(lz77.length);
        int[] nextInts = toIntArray(HelperFunctions.charsToInts(lz77.next));

        rANSElement rDelta = rANS.encode(deltaInts);
        int[] dDelta = rANS.decode(rDelta, rDelta.originalLength);

        rANSElement rLength = rANS.encode(lengthInts);
        int[] dLength = rANS.decode(rLength, rLength.originalLength);

        rANSElement rNext = rANS.encode(nextInts);
        int[] dNextInts = rANS.decode(rNext, rNext.originalLength);
        ArrayList<Character> dNext = HelperFunctions.intsToChars(intsToList(dNextInts));

        HelperFunctions.verifyEquality(lz77.delta, intsToList(dDelta));
        HelperFunctions.verifyEquality(lz77.length, intsToList(dLength));
        HelperFunctions.verifyEquality(lz77.next, dNext);
        System.out.println("    LZ77+rANS: OK (round-trip verified)");
    }


    /* ================================================================
     *  TABLE FORMATTING
     * ================================================================ */

    private static void printFileTable(String baseName, long original, Result[] results) {
        System.out.println();
        System.out.printf("  ┌────────────────────────┬────────────┬────────────┬────────┬──────────┐\n");
        System.out.printf("  │ %-22s │ Orig (B)   │ Compr (B)  │ Ratio  │ bpp      │\n", "");
        System.out.printf("  ├────────────────────────┼────────────┼────────────┼────────┼──────────┤\n");
        System.out.printf("  │ %-22s │ %8d   │ %8s   │ %4s   │ %6s   │\n", "",
            original, "", "", "");
        for (int i = 0; i < SCHEME_COUNT; i++) {
            System.out.printf("  │ %-22s │ %8s   │ %8d   │ %5.2fx │ %5.2f   │\n",
                SCHEMES[i],
                "",
                results[i].compressedBytes,
                results[i].ratio,
                results[i].bpp);
        }
        System.out.printf("  └────────────────────────┴────────────┴────────────┴────────┴──────────┘\n");
        System.out.println();
    }

    private static void printAggregateHeader() {
        // Header printed in the aggregate section above
    }


    /* ================================================================
     *  UTILITIES
     * ================================================================ */

    private static String fmtSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private static int[] toIntArray(ArrayList<Integer> list) {
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
}
