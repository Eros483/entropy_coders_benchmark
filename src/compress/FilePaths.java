package compress;

public final class FilePaths {

	public static final String DATA_DIRECTORY = "Data/";
	public static final String OUTPUT_DIRECTORY = DATA_DIRECTORY + "OutputJava/";
	public static final String HUFFMAN_IO_TEST = DATA_DIRECTORY + "testHuffmanIO";
	public static final String BWT_COMPRESSED_DIRECTORY = OUTPUT_DIRECTORY + "BWT/";
	public static final String LZ77_COMPRESSED_DIRECTORY = OUTPUT_DIRECTORY + "LZ77/";
	public static final String LZSS_COMPRESSED_DIRECTORY = OUTPUT_DIRECTORY + "LZSS/";

	public static final String DATA[] = {
				"C.elegans.fna",
				"E.Coli.fna",
				"H.pylori.fna",
				"lcet10.txt",
				"L.monocytogenes.fna",
				"N.crassa.fna",
				"S.cerevisiae.fna",
				"T.nigroviridis.fna",
				"alice29.txt",
				"aliceinwonderland.txt",
				"asyoulik.txt",
				"bible.txt",
				"cp.html",
				"medium.txt",
				"tiny.txt",
				"xargs.1",
				"fields.c",
				"plrabn12.txt",
		};

		public static final int NUM_DATA = 17;

	public static final String ALPHABET_EXTENSION = ".alpha";

	public static final String HUFFMAN_ENCODING_EXTENSION = ".huffman.encoding";
	public static final String HUFFMAN_MAP_EXTENSION = ".huffman.map";

	public static final String LZ77_DELTA_EXTENSION = ".delta";
	public static final String LZ77_LENGTH_EXTENSION = ".length";
	public static final String LZ77_NEXT_EXTENSION = ".next";

	public static final String LZSS_IDENTIFIER_EXTENSION = ".identifier";
	public static final String LZSS_DELTA_EXTENSION = ".delta";
	public static final String LZSS_LENGTH_EXTENSION = ".length";
	public static final String LZSS_NEXT_EXTENSION = ".next";
}
