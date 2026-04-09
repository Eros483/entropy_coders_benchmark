import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Benchmark all compression algorithms on every data file in Data/.
 *
 * Runs 8 compression pipelines and compares them side-by-side:
 *   0. LZ77  + Huffman
 *   1. LZSS  + Huffman
 *   2. BWT+MTF+Huffman
 *   3. LZ77  + Arithmetic
 *   4. LZSS  + Arithmetic
 *   5. BWT+MTF+Arithmetic
 *   6. LZ77  + rANS
 *   7. BWT+MTF+rANS
 *
 * Per-file table, aggregate totals, ratio matrix, and best-scheme picks.
 *
 * Usage:
 *   java -cp bin BenchmarkArithmetic                      # all Data/ files
 *   java -cp bin BenchmarkArithmetic file1 file2           # specific files
 */
public class BenchmarkArithmetic {

    private static final String[] SCHEMES = {
        "LZ77+Huffman", "LZSS+Huffman", "BWT+MTF+Huffman",
        "LZ77+Arith",   "LZSS+Arith",   "BWT+MTF+Arith",
        "LZ77+rANS",    "BWT+MTF+rANS"
    };
    private static final int N = SCHEMES.length;

    private static record Result(String file, long origBytes, long compBytes,
                                  double ratio, double bpp, double timeSec) {}

    public static void main(String[] args) throws Exception {
        ArrayList<String> dataFiles = discoverFiles(args);
        if (dataFiles.isEmpty()) {
            System.out.println("No data files found.  Usage: java -cp bin BenchmarkArithmetic [file ...]");
            return;
        }
        System.out.println("Benchmarking " + dataFiles.size() + " file(s)...\n");

        double[] totalOrig = new double[N];
        double[] totalComp = new double[N];
        HashMap<String, double[]> compMap = new HashMap<>();

        for (String path : dataFiles) {
            String base = new File(path).getName();
            Result[] r = new Result[N];
            long origBytes = IOHelper.filesize(path);
            double[] compArr = new double[N];
            compMap.put(base, compArr);

            System.out.println("=".repeat(90));
            System.out.println("  FILE: " + base + "  (" + origBytes + " bytes)");
            System.out.println("=".repeat(90));

            double t0 = System.currentTimeMillis();
            r[0] = scheme(path, base, origBytes, 0); totalOrig[0] += origBytes; totalComp[0] += r[0].compBytes; compArr[0] = r[0].compBytes;
            r[1] = scheme(path, base, origBytes, 1); totalOrig[1] += origBytes; totalComp[1] += r[1].compBytes; compArr[1] = r[1].compBytes;
            r[2] = scheme(path, base, origBytes, 2); totalOrig[2] += origBytes; totalComp[2] += r[2].compBytes; compArr[2] = r[2].compBytes;
            r[3] = scheme(path, base, origBytes, 3); totalOrig[3] += origBytes; totalComp[3] += r[3].compBytes; compArr[3] = r[3].compBytes;
            r[4] = scheme(path, base, origBytes, 4); totalOrig[4] += origBytes; totalComp[4] += r[4].compBytes; compArr[4] = r[4].compBytes;
            r[5] = scheme(path, base, origBytes, 5); totalOrig[5] += origBytes; totalComp[5] += r[5].compBytes; compArr[5] = r[5].compBytes;
            r[6] = scheme(path, base, origBytes, 6); totalOrig[6] += origBytes; totalComp[6] += r[6].compBytes; compArr[6] = r[6].compBytes;
            r[7] = scheme(path, base, origBytes, 7); totalOrig[7] += origBytes; totalComp[7] += r[7].compBytes; compArr[7] = r[7].compBytes;

            printTable(base, origBytes, r);
            System.out.println();
        }

        // Aggregate
        System.out.println("=".repeat(105));
        System.out.println("  AGGREGATE (" + dataFiles.size() + " files)");
        System.out.println("=".repeat(105));
        long grandOrig = 0;
        for (String p : dataFiles) grandOrig += IOHelper.filesize(p);
        for (int i = 0; i < N; i++) {
            double ratio = totalOrig[i] / totalComp[i];
            double bpp = (totalComp[i] * 8.0) / totalOrig[i];
            System.out.printf("  %-25s %10s %13s %8.2fx  %6.2f bpp%n",
                SCHEMES[i], fmtSize(grandOrig), fmtSize((long) totalComp[i]), ratio, bpp);
        }

        // Ratio matrix
        System.out.println("\n" + "=".repeat(120));
        System.out.printf("  COMPRESSION RATIO (higher=better)%n");
        System.out.println("=".repeat(120));
        System.out.printf("  %-30s", "File");
        String[] shortNames = {"LZ77+Huf", "LZSS+Huf", "BWT+MTF+H",
                               "LZ77+Ar", "LZSS+Ar", "BWT+MTF+A",
                               "LZ77+rANS", "BWT+MTF+r"};
        for (String s : shortNames) System.out.printf(" %11s", s);
        System.out.println();
        double[] avgRatio = new double[N];
        for (String p : dataFiles) {
            String b2 = new File(p).getName();
            long ob = IOHelper.filesize(p);
            double[] cb = compMap.get(b2);
            System.out.printf("  %-30s", b2);
            for (int i = 0; i < N; i++) {
                double ratio = ob / cb[i];
                avgRatio[i] += ratio;
                System.out.printf(" %9.2fx", ratio);
            }
            System.out.println();
        }
        System.out.println("  " + "-".repeat(116));
        System.out.printf("  %-30s", "AVERAGE");
        for (int i = 0; i < N; i++) System.out.printf(" %9.2fx", avgRatio[i] / dataFiles.size());
        System.out.println();

        // Best per file
        System.out.println("\n" + "=".repeat(80));
        System.out.println("  BEST SCHEME PER FILE");
        System.out.println("=".repeat(80));
        for (String p : dataFiles) {
            String b2 = new File(p).getName();
            long ob = IOHelper.filesize(p);
            double[] cb = compMap.get(b2);
            int best = 0;
            for (int i = 1; i < N; i++) if (cb[i] < cb[best]) best = i;
            System.out.printf("  %-30s -> %-25s (%d bytes, %.2fx)%n",
                b2, SCHEMES[best], (long) cb[best], ob / cb[best]);
        }
        System.out.println();
    }

    /* ---- Run a specific scheme ---- */

    @SuppressWarnings("unchecked")
    private static Result scheme(String path, String base, long origBytes, int idx) throws Exception {
        long t0 = System.currentTimeMillis();
        ArrayList<Character> data;
        long compBytes;
        String outDir;

        switch (idx) {
            case 0:
                data = IOHelper.readFile(path);
                LZ77Element lz77 = LZ77.encode(data);
                outDir = FilePaths.LZ77_COMPRESSED_DIRECTORY + "bench/";
                new File(outDir).mkdirs();
                String l0d = outDir + base + FilePaths.LZ77_DELTA_EXTENSION;
                String l0l = outDir + base + FilePaths.LZ77_LENGTH_EXTENSION;
                String l0n = outDir + base + FilePaths.LZ77_NEXT_EXTENSION;
                HuffmanIOHelper.writeHuffman(Huffman.encode(lz77.delta), l0d);
                HuffmanIOHelper.writeHuffman(Huffman.encode(lz77.length), l0l);
                HuffmanIOHelper.writeHuffman(Huffman.encode(HelperFunctions.charsToInts(lz77.next)), l0n);
                compBytes = io(l0d + FilePaths.HUFFMAN_MAP_EXTENSION) + io(l0d + FilePaths.HUFFMAN_ENCODING_EXTENSION)
                          + io(l0l + FilePaths.HUFFMAN_MAP_EXTENSION) + io(l0l + FilePaths.HUFFMAN_ENCODING_EXTENSION)
                          + io(l0n + FilePaths.HUFFMAN_MAP_EXTENSION) + io(l0n + FilePaths.HUFFMAN_ENCODING_EXTENSION);
                break;

            case 1:
                data = IOHelper.readFile(path);
                LZSSElement lzss = LZSS.encode(data);
                outDir = FilePaths.LZSS_COMPRESSED_DIRECTORY + "bench/";
                new File(outDir).mkdirs();
                String l1i = outDir + base + FilePaths.LZSS_IDENTIFIER_EXTENSION;
                String l1d = outDir + base + FilePaths.LZSS_DELTA_EXTENSION;
                String l1l = outDir + base + FilePaths.LZSS_LENGTH_EXTENSION;
                String l1n = outDir + base + FilePaths.LZSS_NEXT_EXTENSION;
                IOHelper.writeBytes(BitPacker.pack(lzss.identifier), l1i);
                HuffmanIOHelper.writeHuffman(Huffman.encode(lzss.delta), l1d);
                HuffmanIOHelper.writeHuffman(Huffman.encode(lzss.length), l1l);
                HuffmanIOHelper.writeHuffman(Huffman.encode(HelperFunctions.charsToInts(lzss.next)), l1n);
                compBytes = io(l1i) + io(l1d + FilePaths.HUFFMAN_MAP_EXTENSION) + io(l1d + FilePaths.HUFFMAN_ENCODING_EXTENSION)
                          + io(l1l + FilePaths.HUFFMAN_MAP_EXTENSION) + io(l1l + FilePaths.HUFFMAN_ENCODING_EXTENSION)
                          + io(l1n + FilePaths.HUFFMAN_MAP_EXTENSION) + io(l1n + FilePaths.HUFFMAN_ENCODING_EXTENSION);
                break;

            case 2:
                data = IOHelper.readFile(path);
                MTFElement mtf2 = MTF.encode(BWT.encode(data));
                outDir = FilePaths.BWT_COMPRESSED_DIRECTORY + "bench/";
                new File(outDir).mkdirs();
                String l2h = outDir + base;
                HuffmanIOHelper.writeHuffman(Huffman.encode(mtf2.mtf), l2h);
                IOHelper.writeAlphabet(mtf2.alphabet, l2h);
                compBytes = io(l2h + FilePaths.HUFFMAN_MAP_EXTENSION) + io(l2h + FilePaths.HUFFMAN_ENCODING_EXTENSION)
                          + io(l2h + FilePaths.ALPHABET_EXTENSION);
                break;

            case 3: {
                data = IOHelper.readFile(path);
                LZ77Element el3 = LZ77.encode(data);
                outDir = FilePaths.LZ77_COMPRESSED_DIRECTORY + "bench_ar/";
                new File(outDir).mkdirs();
                String l3d = outDir + base + ".ar.d";
                String l3l = outDir + base + ".ar.l";
                String l3n = outDir + base + ".ar.n";
                ArithmeticIOHelper.write(Arithmetic.encode(toInt(el3.delta)), l3d);
                ArithmeticIOHelper.write(Arithmetic.encode(toInt(el3.length)), l3l);
                ArithmeticIOHelper.write(Arithmetic.encode(toInt(HelperFunctions.charsToInts(el3.next))), l3n);
                compBytes = io(l3d) + io(l3l) + io(l3n);
                break;
            }

            case 4: {
                data = IOHelper.readFile(path);
                LZSSElement el4 = LZSS.encode(data);
                outDir = FilePaths.LZSS_COMPRESSED_DIRECTORY + "bench_ar/";
                new File(outDir).mkdirs();
                String l4i = outDir + base + ".ar.i";
                String l4d = outDir + base + ".ar.d";
                String l4l = outDir + base + ".ar.l";
                String l4n = outDir + base + ".ar.n";
                IOHelper.writeBytes(BitPacker.pack(el4.identifier), l4i);
                ArithmeticIOHelper.write(Arithmetic.encode(toInt(el4.delta)), l4d);
                ArithmeticIOHelper.write(Arithmetic.encode(toInt(el4.length)), l4l);
                ArithmeticIOHelper.write(Arithmetic.encode(toInt(HelperFunctions.charsToInts(el4.next))), l4n);
                compBytes = io(l4i) + io(l4d) + io(l4l) + io(l4n);
                break;
            }

            case 5: {
                data = IOHelper.readFile(path);
                MTFElement m5 = MTF.encode(BWT.encode(data));
                outDir = FilePaths.BWT_COMPRESSED_DIRECTORY + "bench_ar/";
                new File(outDir).mkdirs();
                String l5a = outDir + base + ".ar";
                ArithmeticIOHelper.write(Arithmetic.encode(toInt(m5.mtf)), l5a);
                IOHelper.writeAlphabet(m5.alphabet, l5a);
                compBytes = io(l5a) + io(l5a + FilePaths.ALPHABET_EXTENSION);
                break;
            }

            case 6: {
                data = IOHelper.readFile(path);
                LZ77Element el6 = LZ77.encode(data);
                outDir = FilePaths.LZ77_COMPRESSED_DIRECTORY + "bench_rans/";
                new File(outDir).mkdirs();
                String l6d = outDir + base + ".r.d";
                String l6l = outDir + base + ".r.l";
                String l6n = outDir + base + ".r.n";
                rANSIOHelper.write(rANS.encode(toInt(el6.delta)), l6d);
                rANSIOHelper.write(rANS.encode(toInt(el6.length)), l6l);
                rANSIOHelper.write(rANS.encode(toInt(HelperFunctions.charsToInts(el6.next))), l6n);
                compBytes = io(l6d) + io(l6l) + io(l6n);
                break;
            }

            case 7: {
                data = IOHelper.readFile(path);
                MTFElement m7 = MTF.encode(BWT.encode(data));
                outDir = FilePaths.BWT_COMPRESSED_DIRECTORY + "bench_rans/";
                new File(outDir).mkdirs();
                String l7a = outDir + base + ".r";
                rANSIOHelper.write(rANS.encode(toInt(m7.mtf)), l7a);
                IOHelper.writeAlphabet(m7.alphabet, l7a);
                compBytes = io(l7a) + io(l7a + FilePaths.ALPHABET_EXTENSION);
                break;
            }

            default: throw new IllegalStateException("Unknown scheme: " + idx);
        }

        double sec = (System.currentTimeMillis() - t0) / 1000.0;
        double ratio = (double) origBytes / compBytes;
        double bpp = (compBytes * 8.0) / origBytes;
        System.out.printf("  %-25s %8d -> %8d bytes  (%.2fx, %.2f bpp, %.3fs)%n",
            SCHEMES[idx], origBytes, compBytes, ratio, bpp, sec);
        return new Result(base, origBytes, compBytes, ratio, bpp, sec);
    }

    /* ---- Helpers ---- */

    private static int[] toInt(ArrayList<Integer> list) {
        int[] a = new int[list.size()];
        for (int i = 0; i < list.size(); i++) a[i] = list.get(i);
        return a;
    }

    private static long io(String path) { return IOHelper.filesize(path); }

    private static ArrayList<String> discoverFiles(String[] args) {
        ArrayList<String> files = new ArrayList<>();
        if (args.length > 0) {
            for (String a : args) {
                File f = new File(a);
                if (!f.exists()) f = new File(FilePaths.DATA_DIRECTORY + a);
                if (f.exists()) files.add(f.getAbsolutePath());
            }
            return files;
        }
        File d = new File(FilePaths.DATA_DIRECTORY);
        if (d.exists()) {
            File[] ch = d.listFiles(File::isFile);
            if (ch != null) for (File f : ch) files.add(f.getAbsolutePath());
        }
        files.sort(String::compareTo);
        return files;
    }

    private static void printTable(String base, long orig, Result[] results) {
        System.out.println();
        System.out.printf("  ┌──────────────────────┬──────────┬──────────┬───────┬────────┐%n");
        System.out.printf("  │ Scheme               │ Orig     │ Compressed│ Ratio │ bpp    │%n");
        System.out.printf("  ├──────────────────────┼──────────┼──────────┼───────┼────────┤%n");
        for (int i = 0; i < N; i++) {
            System.out.printf("  │ %-22s │ %8d │ %8d │ %5.2fx │ %5.2f │%n",
                SCHEMES[i], results[i].origBytes, results[i].compBytes,
                results[i].ratio, results[i].bpp);
        }
        System.out.printf("  └──────────────────────┴──────────┴──────────┴───────┴────────┘%n");
    }

    private static String fmtSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1048576) return String.format("%.1f KB", b / 1024.0);
        return String.format("%.1f MB", b / 1048576.0);
    }
}
