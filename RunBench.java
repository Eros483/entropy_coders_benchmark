import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Unified benchmark: LZ77, LZSS, BWT/MTF x Huffman / Arithmetic / rANS.
 *
 * Usage:
 *   java -cp bin RunBench                                              all 8 schemes, all Data/ files
 *   java -cp bin RunBench --files tiny.txt,L.monocytogenes.fna          all schemes, specific files
 *   java -cp bin RunBench --schemes LZ77+rANS,LZ77+Arith               LZ77+rANS and LZ77+Arith, all files
 *   java -cp bin RunBench --schemes LZ77+rANS,LZ77+Arith --files bible.txt  two schemes on one file
 */
public class RunBench {

    private static final String[] ALL_SCHEMES = {
        "LZ77+Huffman", "LZ77+Arith", "LZ77+rANS",
        "LZSS+Huffman", "BWT+MTF+Huffman",
        "BWT+MTF+rANS", "BWT+MTF+Arith", 
        "LZ78+rANS", "LZ78+Arith",
        "LZW+rANS", "LZW+Arith"
    };
    private static final int N = ALL_SCHEMES.length;

    private static record Result(String file, long origBytes, long compBytes,
                                  double ratio, double bpp, double timeSec) {}

    /* -------- arg parsing -------- */

    private static ArrayList<Integer> parseSchemeList(String[] raw) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (String s : raw) {
            for (int i = 0; i < N; i++) {
                if (ALL_SCHEMES[i].equalsIgnoreCase(s)) { ids.add(i); break; }
            }
        }
        return ids;
    }

    private static ArrayList<String> discoverFiles(String[] names) {
        ArrayList<String> files = new ArrayList<>();
        for (String n : names) {
            File f = new File(n);
            if (!f.exists()) f = new File(FilePaths.DATA_DIRECTORY + n);
            if (f.exists()) files.add(f.getAbsolutePath());
        }
        files.sort(String::compareTo);
        return files;
    }

    private static ArrayList<String> allDataFiles() {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < FilePaths.NUM_DATA; i++) {
            String p = FilePaths.DATA_DIRECTORY + FilePaths.DATA[i];
            if (new File(p).exists()) list.add(p);
        }
        list.sort(String::compareTo);
        return list;
    }

    private static String shortName(String s) {
        return s.replace("+Huffman", "+Huf").replace("+Arith", "+Ar");
    }

    /* -------- main -------- */

    public static void main(String[] args) throws Exception {

        ArrayList<String> fileArgList = new ArrayList<>();
        ArrayList<String> schemeArgList = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if ("--schemes".equals(args[i]) && i + 1 < args.length) {
                String[] parts = args[++i].split(",");
                for (String p : parts) schemeArgList.add(p.trim());
            } else if ("--files".equals(args[i]) && i + 1 < args.length) {
                String[] parts = args[++i].split(",");
                for (String p : parts) fileArgList.add(p.trim());
            } else if (!args[i].startsWith("--")) {
                fileArgList.add(args[i]);
            }
        }

        ArrayList<Integer> schemeIds = schemeArgList.isEmpty() ? ints(0, N) : parseSchemeList(schemeArgList.toArray(new String[0]));
        ArrayList<String> dataFiles = fileArgList.isEmpty() ? allDataFiles() : discoverFiles(fileArgList.toArray(new String[0]));

        if (dataFiles.isEmpty()) {
            System.out.println("No data files found.");
            return;
        }
        if (schemeIds.isEmpty()) {
            System.out.println("No valid schemes specified.");
            return;
        }

        String[] schemeNames = new String[schemeIds.size()];
        String[] shortNames = new String[schemeIds.size()];
        int[] active = new int[schemeIds.size()];
        for (int i = 0; i < schemeIds.size(); i++) {
            schemeNames[i] = ALL_SCHEMES[schemeIds.get(i)];
            active[i] = schemeIds.get(i);
            shortNames[i] = shortName(ALL_SCHEMES[active[i]]);
        }
        int SN = schemeNames.length;

        System.out.println("Benchmarking " + SN + " scheme(s) on " + dataFiles.size() + " file(s)...\n");

        double[] totalOrig = new double[SN];
        double[] totalComp = new double[SN];
        HashMap<String, double[]> compMap = new HashMap<>();

        for (String path : dataFiles) {
            String base = new File(path).getName();
            Result[] r = new Result[SN];
            long origBytes = IOHelper.filesize(path);
            double[] compArr = new double[SN];
            compMap.put(base, compArr);

            System.out.println("=".repeat(90));
            System.out.println("  FILE: " + base + "  (" + origBytes + " bytes)");
            System.out.println("=".repeat(90));

            for (int i = 0; i < SN; i++) {
                r[i] = runScheme(path, base, origBytes, active[i]);
                totalOrig[i] += r[i].origBytes;
                totalComp[i] += r[i].compBytes;
                compArr[i] = r[i].compBytes;
            }

            printTable(base, origBytes, schemeNames, r);
            System.out.println();
        }

        // Aggregate
        System.out.println("=".repeat(105));
        System.out.println("  AGGREGATE (" + dataFiles.size() + " files)");
        System.out.println("=".repeat(105));
        long grandOrig = 0;
        for (String p : dataFiles) grandOrig += IOHelper.filesize(p);
        for (int i = 0; i < SN; i++) {
            double ratio = totalOrig[i] / totalComp[i];
            double bpp = (totalComp[i] * 8.0) / totalOrig[i];
            System.out.printf("  %-25s %10s %13s %8.2fx  %6.2f bpp%n",
                schemeNames[i], fmtSize(grandOrig), fmtSize((long) totalComp[i]), ratio, bpp);
        }

        // Ratio matrix
        System.out.println();
        System.out.println("=".repeat(120));
        System.out.println("  COMPRESSION RATIO (higher=better)");
        System.out.println("=".repeat(120));
        System.out.printf("  %-30s", "File");
        for (String s : shortNames) System.out.printf(" %14s", s);
        System.out.println();
        double[] avgRatio = new double[SN];
        for (String p : dataFiles) {
            String b2 = new File(p).getName();
            long ob = IOHelper.filesize(p);
            double[] cb = compMap.get(b2);
            System.out.printf("  %-30s", b2);
            for (int i = 0; i < SN; i++) {
                double ratio = ob / cb[i];
                avgRatio[i] += ratio;
                System.out.printf(" %13.2fx", ratio);
            }
            System.out.println();
        }
        System.out.println("  " + "-".repeat(116));
        System.out.printf("  %-30s", "AVERAGE");
        for (int i = 0; i < SN; i++) System.out.printf(" %13.2fx", avgRatio[i] / dataFiles.size());
        System.out.println();

        // Best per file
        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("  BEST SCHEME PER FILE");
        System.out.println("=".repeat(80));
        for (String p : dataFiles) {
            String b2 = new File(p).getName();
            long ob2 = IOHelper.filesize(p);
            double[] cb = compMap.get(b2);
            int best = 0;
            for (int i = 1; i < SN; i++) if (cb[i] < cb[best]) best = i;
            System.out.printf("  %-30s -> %-25s (%d bytes, %.2fx)%n",
                b2, schemeNames[best], (long) cb[best], ob2 / cb[best]);
        }
        System.out.println("\nAll done.");
    }

    /* ---- run one scheme ---- */

    private static String outDir(String d) { new File(d).mkdirs(); return d; }
    private static int[] toInt(ArrayList<Integer> list) {
        int[] a = new int[list.size()];
        for (int i = 0; i < list.size(); i++) a[i] = list.get(i);
        return a;
    }
    private static long io(String p) { return IOHelper.filesize(p); }

    private static ArrayList<Integer> ints(int from, int to) {
        ArrayList<Integer> l = new ArrayList<>();
        for (int i = from; i < to; i++) l.add(i);
        return l;
    }

    private static String fmtSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1048576) return String.format("%.1f KB", b / 1024.0);
        if (b < 1073741824) return String.format("%.1f MB", b / 1048576.0);
        return String.format("%.2f GB", b / 1073741824.0);
    }

    private static void printTable(String base, long orig, String[] names, Result[] results) {
        System.out.println();
        System.out.printf("  \u250C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u252C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u252C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u252C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u252C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u252C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u252C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510\n");
        System.out.printf("  \u2502 Scheme               \u2502 Orig     \u2502 Compressed\u2502 Ratio \u2502 bpp      \u2502 Time (s)\u2502\n");
        System.out.printf("  \u251C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253C\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2524\n");
        for (int i = 0; i < results.length; i++) {
            Result rr = results[i];
            System.out.printf("  \u2502 %-22s \u2502 %8d \u2502 %8d \u2502 %5.2fx \u2502 %5.2f   \u2502  %6.2fs \u2502\n",
                names[i], rr.origBytes, rr.compBytes, rr.ratio, rr.bpp, rr.timeSec);
        }
        System.out.printf("  \u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2534\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2534\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2534\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2534\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2534\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2534\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518\n");
    }

    /* ---- run one scheme by index ---- */

    @SuppressWarnings("unchecked")
    private static Result runScheme(String path, String base, long origBytes, int idx) throws Exception {
        long t0 = System.currentTimeMillis();
        ArrayList<Character> data;
        long compBytes;
        String name = ALL_SCHEMES[idx];

        switch (idx) {
            case 0: { // LZ77+Huffman
                data = IOHelper.readFile(path);
                String od = outDir(FilePaths.LZ77_COMPRESSED_DIRECTORY + "bench/");
                String d = od + base + FilePaths.LZ77_DELTA_EXTENSION;
                String l = od + base + FilePaths.LZ77_LENGTH_EXTENSION;
                String n = od + base + FilePaths.LZ77_NEXT_EXTENSION;
                LZ77Element lz = LZ77.encode(data);
                HuffmanIOHelper.writeHuffman(Huffman.encode(lz.delta), d);
                HuffmanIOHelper.writeHuffman(Huffman.encode(lz.length), l);
                HuffmanIOHelper.writeHuffman(Huffman.encode(HelperFunctions.charsToInts(lz.next)), n);
                compBytes = io(d + FilePaths.HUFFMAN_MAP_EXTENSION) + io(d + FilePaths.HUFFMAN_ENCODING_EXTENSION)
                          + io(l + FilePaths.HUFFMAN_MAP_EXTENSION) + io(l + FilePaths.HUFFMAN_ENCODING_EXTENSION)
                          + io(n + FilePaths.HUFFMAN_MAP_EXTENSION) + io(n + FilePaths.HUFFMAN_ENCODING_EXTENSION);
                break;
            }
            case 1: { // LZ77+Arith
                data = IOHelper.readFile(path);
                String od = outDir(FilePaths.LZ77_COMPRESSED_DIRECTORY + "bench_ar/");
                String d = od + base + ".ar.d", l = od + base + ".ar.l", n = od + base + ".ar.n";
                LZ77Element lz = LZ77.encode(data);
                ArithmeticIOHelper.write(Arithmetic.encode(toInt(lz.delta)), d);
                ArithmeticIOHelper.write(Arithmetic.encode(toInt(lz.length)), l);
                ArithmeticIOHelper.write(Arithmetic.encode(toInt(HelperFunctions.charsToInts(lz.next))), n);
                compBytes = io(d) + io(l) + io(n);
                break;
            }
            case 2: { // LZ77+rANS
                data = IOHelper.readFile(path);
                String od = outDir(FilePaths.LZ77_COMPRESSED_DIRECTORY + "bench_rans/");
                String d = od + base + ".r.d", l = od + base + ".r.l", n = od + base + ".r.n";
                LZ77Element lz = LZ77.encode(data);
                rANSIOHelper.write(rANS.encode(toInt(lz.delta)), d);
                rANSIOHelper.write(rANS.encode(toInt(lz.length)), l);
                rANSIOHelper.write(rANS.encode(toInt(HelperFunctions.charsToInts(lz.next))), n);
                compBytes = io(d) + io(l) + io(n);
                break;
            }
            case 3: { // LZSS+Huffman
                data = IOHelper.readFile(path);
                String od = outDir(FilePaths.LZSS_COMPRESSED_DIRECTORY + "bench/");
                String id = od + base + FilePaths.LZSS_IDENTIFIER_EXTENSION;
                String d = od + base + FilePaths.LZSS_DELTA_EXTENSION;
                String l = od + base + FilePaths.LZSS_LENGTH_EXTENSION;
                String n = od + base + FilePaths.LZSS_NEXT_EXTENSION;
                LZSSElement lz = LZSS.encode(data);
                IOHelper.writeBytes(BitPacker.pack(lz.identifier), id);
                HuffmanIOHelper.writeHuffman(Huffman.encode(lz.delta), d);
                HuffmanIOHelper.writeHuffman(Huffman.encode(lz.length), l);
                HuffmanIOHelper.writeHuffman(Huffman.encode(HelperFunctions.charsToInts(lz.next)), n);
                compBytes = io(id) + io(d + FilePaths.HUFFMAN_MAP_EXTENSION) + io(d + FilePaths.HUFFMAN_ENCODING_EXTENSION)
                          + io(l + FilePaths.HUFFMAN_MAP_EXTENSION) + io(l + FilePaths.HUFFMAN_ENCODING_EXTENSION)
                          + io(n + FilePaths.HUFFMAN_MAP_EXTENSION) + io(n + FilePaths.HUFFMAN_ENCODING_EXTENSION);
                break;
            }
            case 4: { // BWT+MTF+Huffman
                data = IOHelper.readFile(path);
                String od = outDir(FilePaths.BWT_COMPRESSED_DIRECTORY + "bench/");
                String h = od + base;
                MTFElement mt = MTF.encode(BWT.encode(data));
                HuffmanIOHelper.writeHuffman(Huffman.encode(mt.mtf), h);
                IOHelper.writeAlphabet(mt.alphabet, h);
                compBytes = io(h + FilePaths.HUFFMAN_MAP_EXTENSION) + io(h + FilePaths.HUFFMAN_ENCODING_EXTENSION)
                          + io(h + FilePaths.ALPHABET_EXTENSION);
                break;
            }
            case 5: { // BWT+MTF+rANS
                data = IOHelper.readFile(path);
                String od = outDir(FilePaths.BWT_COMPRESSED_DIRECTORY + "bench_rans/");
                String h = od + base;
                MTFElement mt = MTF.encode(BWT.encode(data));
                rANSIOHelper.write(rANS.encode(toInt(mt.mtf)), h + ".rans");
                IOHelper.writeAlphabet(mt.alphabet, h);
                compBytes = io(h + ".rans") + io(h + FilePaths.ALPHABET_EXTENSION);
                break;
            }
            case 6: { // BWT+MTF+Arith
                data = IOHelper.readFile(path);
                String od = outDir(FilePaths.BWT_COMPRESSED_DIRECTORY + "bench_ar/");
                String h = od + base;
                MTFElement mt = MTF.encode(BWT.encode(data));
                ArithmeticIOHelper.write(Arithmetic.encode(toInt(mt.mtf)), h + ".ar");
                IOHelper.writeAlphabet(mt.alphabet, h);
                compBytes = io(h + ".ar") + io(h + FilePaths.ALPHABET_EXTENSION);
                break;
            }
            case 7: { // LZ78+rANS
                data = IOHelper.readFile(path);
                String od = outDir(FilePaths.OUTPUT_DIRECTORY + "LZ78/bench_rans/");
                String idxPath = od + base + ".rans.idx";
                String charPath = od + base + ".rans.char";
                LZ78Element lz78 = LZ78.encode(data);
                rANSIOHelper.write(rANS.encode(toInt(lz78.dictIndex)), idxPath);
                rANSIOHelper.write(rANS.encode(toInt(HelperFunctions.charsToInts(lz78.nextChar))), charPath);
                compBytes = io(idxPath) + io(charPath);
                break;
            }
            case 8: { // LZ78+Arith
                data = IOHelper.readFile(path);
                String od = outDir(FilePaths.OUTPUT_DIRECTORY + "LZ78/bench_ar/");
                String idxPath = od + base + ".ar.idx";
                String charPath = od + base + ".ar.char";
                LZ78Element lz78 = LZ78.encode(data);
                ArithmeticIOHelper.write(Arithmetic.encode(toInt(lz78.dictIndex)), idxPath);
                ArithmeticIOHelper.write(Arithmetic.encode(toInt(HelperFunctions.charsToInts(lz78.nextChar))), charPath);
                compBytes = io(idxPath) + io(charPath);
                break;
            }
            case 9: { // LZW+rANS
                data = IOHelper.readFile(path);
                String od = outDir(FilePaths.OUTPUT_DIRECTORY + "LZW/bench_rans/");
                String outPath = od + base + ".rans";
                int[] codes = LZW.getCodes(data);
                rANSIOHelper.write(rANS.encode(codes), outPath);
                compBytes = io(outPath);
                break;
            }
            case 10: { // LZW+Arith
                data = IOHelper.readFile(path);
                String od = outDir(FilePaths.OUTPUT_DIRECTORY + "LZW/bench_ar/");
                String outPath = od + base + ".ar";
                int[] codes = LZW.getCodes(data);
                ArithmeticIOHelper.write(Arithmetic.encode(codes), outPath);
                compBytes = io(outPath);
                break;
            }
            default: throw new IllegalStateException("Unknown scheme index: " + idx);
        }

        double sec = (System.currentTimeMillis() - t0) / 1000.0;
        double ratio = (double) origBytes / compBytes;
        double bpp = (compBytes * 8.0) / origBytes;
        System.out.printf("  %-25s %8d -> %8d bytes  (%.2fx, %.2f bpp, %.3fs)%n",
            name, origBytes, compBytes, ratio, bpp, sec);
        return new Result(base, origBytes, compBytes, ratio, bpp, sec);
    }
}
