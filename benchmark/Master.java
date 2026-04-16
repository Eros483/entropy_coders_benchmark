package benchmark;

import java.util.ArrayList;

import core.*;
import compress.*;

public class Master {

	public static void lz77() throws Exception {
		lz77_compress();
		lz77_expand();
	}

	public static void lzss() throws Exception {
		lzss_compress();
		lzss_expand();
	}

	public static void bwt() throws Exception {
		bwt_compress();
		bwt_expand();
	}

	public static void lzw() throws Exception {
		lzw_compress();
		lzw_expand();
	}

	public static void lz78() throws Exception {
		lz78_compress();
		lz78_expand();
	}

	private static void lz78_compress() throws Exception {
		for (int i = 0; i < FilePaths.NUM_DATA; i++) {
			long startTime = System.currentTimeMillis();
			String path = FilePaths.DATA_DIRECTORY + FilePaths.DATA[i];
			String outDir = FilePaths.OUTPUT_DIRECTORY + "LZ78/";
			new java.io.File(outDir).mkdirs();
			String idxPath = outDir + FilePaths.DATA[i] + ".idx";
			String charPath = outDir + FilePaths.DATA[i] + ".char";

			System.out.println("\n**********************");
			System.out.println("\nReading file: " + path);
			ArrayList<Character> data = IOHelper.readFile(path);

			System.out.println("Data -> LZ78 Phase...");
			LZ78Element lz78 = LZ78.encode(data);

			System.out.println("LZ78 -> Huffman Phase...");
			HuffmanElement huff_Idx = Huffman.encode(lz78.dictIndex);
			HuffmanElement huff_Char = Huffman.encode(HelperFunctions.charsToInts(lz78.nextChar));

			System.out.println("Writing compressed files...");
			HuffmanIOHelper.writeHuffman(huff_Idx, idxPath);
			HuffmanIOHelper.writeHuffman(huff_Char, charPath);

			System.out.printf("\nCompression took %.2f seconds\n", (System.currentTimeMillis() - startTime) / 1000d);
			long filesize = IOHelper.filesize(path);
			long compressedSize = IOHelper.filesize(idxPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
					IOHelper.filesize(idxPath + FilePaths.HUFFMAN_ENCODING_EXTENSION) +
					IOHelper.filesize(charPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
					IOHelper.filesize(charPath + FilePaths.HUFFMAN_ENCODING_EXTENSION);
			System.out.println("Original File Size = " + filesize + " bytes");
			System.out.println("Compressed File Size = " + compressedSize + " bytes");
			System.out.printf("Compression Ratio = %.2f\n", 1.00 * filesize / compressedSize);
		}
	}

	public static void lz78_expand() throws Exception {
		for (int i = 0; i < FilePaths.NUM_DATA; i++) {
			System.out.println("\n**********************");
			String path = FilePaths.DATA_DIRECTORY + FilePaths.DATA[i];
			ArrayList<Character> data = IOHelper.readFile(path);

			String outDir = FilePaths.OUTPUT_DIRECTORY + "LZ78/";
			String idxPath = outDir + FilePaths.DATA[i] + ".idx";
			String charPath = outDir + FilePaths.DATA[i] + ".char";

			long startTime = System.currentTimeMillis();
			System.out.println("Reading compressed files...");
			HuffmanElement huff_Idx = HuffmanIOHelper.readHuffman(idxPath);
			HuffmanElement huff_Char = HuffmanIOHelper.readHuffman(charPath);

			System.out.println("Huffman -> LZ78 Phase...");
			ArrayList<Integer> decoded_Idx = Huffman.decode(huff_Idx);
			ArrayList<Character> decoded_Char = HelperFunctions.intsToChars(Huffman.decode(huff_Char));

			System.out.println("LZ78 -> Decoded Data Phase...");
			LZ78Element decodedTriplet = new LZ78Element(decoded_Idx, decoded_Char);
			ArrayList<Character> decodedData = LZ78.decode(decodedTriplet);

			System.out.printf("\nDecompression took %.2f seconds\n", (System.currentTimeMillis() - startTime) / 1000d);
			HelperFunctions.verifyEquality(data, decodedData);
		}
	}

	private static void lzw_compress() throws Exception {
		for (int i = 0; i < FilePaths.NUM_DATA; i++) {
			long startTime = System.currentTimeMillis();
			String path = FilePaths.DATA_DIRECTORY + FilePaths.DATA[i];

			String lzwOutputPath = FilePaths.OUTPUT_DIRECTORY + "LZW/" + FilePaths.DATA[i] + ".lzw";
			new java.io.File(FilePaths.OUTPUT_DIRECTORY + "LZW/").mkdirs();

			System.out.println("\n**********************");
			System.out.println("\nReading file: " + path);
			ArrayList<Character> data = IOHelper.readFile(path);

			System.out.println("Data -> LZW Phase...");
			byte[] compressedBytes = LZW.encode(data);

			System.out.println("Writing compressed file...");
			IOHelper.writeBytes(compressedBytes, lzwOutputPath);

			System.out.printf("\nCompression took %.2f seconds\n", (System.currentTimeMillis() - startTime) / 1000d);
			long filesize = IOHelper.filesize(path);
			long compressedSize = IOHelper.filesize(lzwOutputPath);

			System.out.println("\nOriginal File Size = " + filesize + " bytes");
			System.out.println("Compressed File Size = " + compressedSize + " bytes");
			System.out.printf("Compression Ratio = %.2f\n", 1.00 * filesize / compressedSize);
		}
	}

	private static void lzw_expand() throws Exception {
		for (int i = 0; i < FilePaths.NUM_DATA; i++) {
			System.out.println("\n**********************");
			String path = FilePaths.DATA_DIRECTORY + FilePaths.DATA[i];
			ArrayList<Character> originalData = IOHelper.readFile(path);

			String lzwOutputPath = FilePaths.OUTPUT_DIRECTORY + "LZW/" + FilePaths.DATA[i] + ".lzw";
			long startTime = System.currentTimeMillis();

			System.out.println("Reading compressed file...");
			byte[] compressedBytes = IOHelper.readBytes(lzwOutputPath);

			System.out.println("LZW -> Decoded Data Phase...");
			ArrayList<Character> decodedData = LZW.decode(compressedBytes);

			System.out.printf("\nDecompression took %.2f seconds\n", (System.currentTimeMillis() - startTime) / 1000d);

			HelperFunctions.verifyEquality(originalData, decodedData);
		}
	}

	private static void lz77_compress() throws Exception {
		for (int i = 0; i < FilePaths.NUM_DATA; i++) {

			long startTime = System.currentTimeMillis();

			String path = FilePaths.DATA_DIRECTORY + FilePaths.DATA[i];
			String lz77DeltaPath = FilePaths.LZ77_COMPRESSED_DIRECTORY + FilePaths.DATA[i]
					+ FilePaths.LZ77_DELTA_EXTENSION;
			String lz77LengthPath = FilePaths.LZ77_COMPRESSED_DIRECTORY + FilePaths.DATA[i]
					+ FilePaths.LZ77_LENGTH_EXTENSION;
			String lz77NextPath = FilePaths.LZ77_COMPRESSED_DIRECTORY + FilePaths.DATA[i]
					+ FilePaths.LZ77_NEXT_EXTENSION;

			System.out.println("\n**********************");
			System.out.println("\nReading file: " + path);

			ArrayList<Character> data = IOHelper.readFile(path);

			System.out.println("File loaded.");

			System.out.println("Data -> LZ77 Phase...");
			LZ77Element lz77 = LZ77.encode(data);

			System.out.println("LZ77 -> Huffman Phase...");
			HuffmanElement huff_Delta = Huffman.encode(lz77.delta);
			HuffmanElement huff_Length = Huffman.encode(lz77.length);
			HuffmanElement huff_Next = Huffman.encode(HelperFunctions.charsToInts(lz77.next));

			System.out.println("Writing compressed files...");
			HuffmanIOHelper.writeHuffman(huff_Delta, lz77DeltaPath);
			HuffmanIOHelper.writeHuffman(huff_Length, lz77LengthPath);
			HuffmanIOHelper.writeHuffman(huff_Next, lz77NextPath);

			System.out.printf("\nCompression took %.2f seconds\n", (System.currentTimeMillis() - startTime) / 1000d);
			long filesize = IOHelper.filesize(path);
			System.out.println("\nOriginal File Size = " + filesize + " bytes");
			long compressedSize = IOHelper.filesize(lz77DeltaPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
					IOHelper.filesize(lz77DeltaPath + FilePaths.HUFFMAN_ENCODING_EXTENSION) +
					IOHelper.filesize(lz77LengthPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
					IOHelper.filesize(lz77LengthPath + FilePaths.HUFFMAN_ENCODING_EXTENSION) +
					IOHelper.filesize(lz77NextPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
					IOHelper.filesize(lz77NextPath + FilePaths.HUFFMAN_ENCODING_EXTENSION);
			System.out.println("Compressed File Size = " + compressedSize + " bytes");
			System.out.printf("Compression Ratio = %.2f\n", 1.00 * filesize / compressedSize);
		}
	}

	public static void lz77_expand() throws Exception {
		for (int i = 0; i < FilePaths.NUM_DATA; i++) {

			System.out.println("\n**********************");
			String path = FilePaths.DATA_DIRECTORY + FilePaths.DATA[i];
			System.out.println("\nReading data for verification: " + path);
			ArrayList<Character> data = IOHelper.readFile(path);
			System.out.println("File loaded.");

			String lz77DeltaPath = FilePaths.LZ77_COMPRESSED_DIRECTORY + FilePaths.DATA[i]
					+ FilePaths.LZ77_DELTA_EXTENSION;
			String lz77LengthPath = FilePaths.LZ77_COMPRESSED_DIRECTORY + FilePaths.DATA[i]
					+ FilePaths.LZ77_LENGTH_EXTENSION;
			String lz77NextPath = FilePaths.LZ77_COMPRESSED_DIRECTORY + FilePaths.DATA[i]
					+ FilePaths.LZ77_NEXT_EXTENSION;

			long startTime = System.currentTimeMillis();

			System.out.println("Reading compressed files...");
			HuffmanElement huffFromFile_Delta = HuffmanIOHelper.readHuffman(lz77DeltaPath);
			HuffmanElement huffFromFile_Length = HuffmanIOHelper.readHuffman(lz77LengthPath);
			HuffmanElement huffFromFile_Next = HuffmanIOHelper.readHuffman(lz77NextPath);

			System.out.println("Huffman -> LZ77 Phase...");
			ArrayList<Integer> decoded_Delta = Huffman.decode(huffFromFile_Delta);
			ArrayList<Integer> decoded_Length = Huffman.decode(huffFromFile_Length);
			ArrayList<Character> decoded_Next = HelperFunctions.intsToChars(Huffman.decode(huffFromFile_Next));

			System.out.println("LZ77 -> Decoded Data Phase...");
			LZ77Element decodedTriplet = new LZ77Element(decoded_Delta, decoded_Length, decoded_Next);
			ArrayList<Character> decodedData = LZ77.decode(decodedTriplet);

			System.out.printf("\nDecompression took %.2f seconds\n", (System.currentTimeMillis() - startTime) / 1000d);

			HelperFunctions.verifyEquality(data, decodedData);
		}
	}

	private static void lzss_compress() throws Exception {
		for (int i = 0; i < FilePaths.NUM_DATA; i++) {

			long startTime = System.currentTimeMillis();

			String path = FilePaths.DATA_DIRECTORY + FilePaths.DATA[i];
			String lzssIdentifierPath = FilePaths.LZSS_COMPRESSED_DIRECTORY + FilePaths.DATA[i]
					+ FilePaths.LZSS_IDENTIFIER_EXTENSION;
			String lzssDeltaPath = FilePaths.LZSS_COMPRESSED_DIRECTORY + FilePaths.DATA[i]
					+ FilePaths.LZSS_DELTA_EXTENSION;
			String lzssLengthPath = FilePaths.LZSS_COMPRESSED_DIRECTORY + FilePaths.DATA[i]
					+ FilePaths.LZSS_LENGTH_EXTENSION;
			String lzssNextPath = FilePaths.LZSS_COMPRESSED_DIRECTORY + FilePaths.DATA[i]
					+ FilePaths.LZSS_NEXT_EXTENSION;


			System.out.println("\n**********************");
			System.out.println("\nReading file: " + path);

			ArrayList<Character> data = IOHelper.readFile(path);

			System.out.println("File loaded.");

			System.out.println("Data -> LZSS Phase...");
			LZSSElement lzss = LZSS.encode(data);

			System.out.println("LZSS -> Huffman Phase...");
			HuffmanElement huff_Delta = Huffman.encode(lzss.delta);
			HuffmanElement huff_Length = Huffman.encode(lzss.length);
			HuffmanElement huff_Next = Huffman.encode(HelperFunctions.charsToInts(lzss.next));

			System.out.println("Writing compressed files...");
			byte[] compressedIdentifiers = BitPacker.pack(lzss.identifier);
			IOHelper.writeBytes(compressedIdentifiers, lzssIdentifierPath);
			HuffmanIOHelper.writeHuffman(huff_Delta, lzssDeltaPath);
			HuffmanIOHelper.writeHuffman(huff_Length, lzssLengthPath);
			HuffmanIOHelper.writeHuffman(huff_Next, lzssNextPath);

			System.out.printf("\nCompression took %.2f seconds\n", (System.currentTimeMillis() - startTime) / 1000d);
			long filesize = IOHelper.filesize(path);
			System.out.println("\nOriginal File Size = " + filesize + " bytes");
			long compressedSize = IOHelper.filesize(lzssIdentifierPath) +
					IOHelper.filesize(lzssDeltaPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
					IOHelper.filesize(lzssDeltaPath + FilePaths.HUFFMAN_ENCODING_EXTENSION) +
					IOHelper.filesize(lzssLengthPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
					IOHelper.filesize(lzssLengthPath + FilePaths.HUFFMAN_ENCODING_EXTENSION) +
					IOHelper.filesize(lzssNextPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
					IOHelper.filesize(lzssNextPath + FilePaths.HUFFMAN_ENCODING_EXTENSION);
			System.out.println("Compressed File Size = " + compressedSize + " bytes");
			System.out.printf("Compression Ratio = %.2f\n", 1.00 * filesize / compressedSize);
		}
	}

	private static void lzss_expand() throws Exception {
		for (int i = 0; i < FilePaths.NUM_DATA; i++) {

			System.out.println("\n**********************");
			String path = FilePaths.DATA_DIRECTORY + FilePaths.DATA[i];
			System.out.println("\nReading file for verification: " + path);
			ArrayList<Character> data = IOHelper.readFile(path);
			System.out.println("File loaded.");

			String lzssIdentifierPath = FilePaths.LZSS_COMPRESSED_DIRECTORY + FilePaths.DATA[i]
					+ FilePaths.LZSS_IDENTIFIER_EXTENSION;
			String lzssDeltaPath = FilePaths.LZSS_COMPRESSED_DIRECTORY + FilePaths.DATA[i]
					+ FilePaths.LZSS_DELTA_EXTENSION;
			String lzssLengthPath = FilePaths.LZSS_COMPRESSED_DIRECTORY + FilePaths.DATA[i]
					+ FilePaths.LZSS_LENGTH_EXTENSION;
			String lzssNextPath = FilePaths.LZSS_COMPRESSED_DIRECTORY + FilePaths.DATA[i]
					+ FilePaths.LZSS_NEXT_EXTENSION;

			long startTime = System.currentTimeMillis();

			System.out.println("Reading compressed files...");
			String identifiersFromFile = BitPacker.unpack(IOHelper.readBytes(lzssIdentifierPath));
			HuffmanElement huffFromFile_Delta = HuffmanIOHelper.readHuffman(lzssDeltaPath);
			HuffmanElement huffFromFile_Length = HuffmanIOHelper.readHuffman(lzssLengthPath);
			HuffmanElement huffFromFile_Next = HuffmanIOHelper.readHuffman(lzssNextPath);

			System.out.println("Huffman -> LZSS Phase...");
			ArrayList<Integer> decoded_Delta = Huffman.decode(huffFromFile_Delta);
			ArrayList<Integer> decoded_Length = Huffman.decode(huffFromFile_Length);
			ArrayList<Character> decoded_Next = HelperFunctions.intsToChars(Huffman.decode(huffFromFile_Next));

			System.out.println("LZSS -> Decoded Data Phase...");
			LZSSElement decodedTriplet = new LZSSElement(identifiersFromFile, decoded_Delta, decoded_Length, decoded_Next);
			ArrayList<Character> decodedData = LZSS.decode(decodedTriplet);

			System.out.printf("\nDecompression took %.2f seconds\n", (System.currentTimeMillis() - startTime) / 1000d);

			HelperFunctions.verifyEquality(data, decodedData);
		}
	}

	private static void bwt_compress() throws Exception {
		for (int i = 0; i < FilePaths.NUM_DATA; i++) {

			long startTime = System.currentTimeMillis();

			String path = FilePaths.DATA_DIRECTORY + FilePaths.DATA[i];
			String huffmanPath = FilePaths.BWT_COMPRESSED_DIRECTORY + FilePaths.DATA[i];
			String alphaPath = FilePaths.BWT_COMPRESSED_DIRECTORY + FilePaths.DATA[i];

			System.out.println("\n**********************");
			System.out.println("\nReading file: " + path);

			ArrayList<Character> data = IOHelper.readFile(path);
			System.out.println("File loaded.");

			System.out.println("Data -> BWT Phase...");
			ArrayList<Character> bwt = BWT.encode(data);

			System.out.println("BWT -> MTF Phase...");
			MTFElement mtf = MTF.encode(bwt);

			System.out.println("MTF -> Huffman Phase...");
			HuffmanElement huff = Huffman.encode(mtf.mtf);

			System.out.println("Writing compressed files...");
			HuffmanIOHelper.writeHuffman(huff, huffmanPath);
			IOHelper.writeAlphabet(mtf.alphabet, alphaPath);

			System.out.printf("\nCompression took %.2f seconds\n", (System.currentTimeMillis() - startTime) / 1000d);
			long filesize = IOHelper.filesize(path);
			System.out.println("\nOriginal File Size = " + filesize + " bytes");
			long compressedSize = IOHelper.filesize(huffmanPath + FilePaths.HUFFMAN_MAP_EXTENSION) +
					IOHelper.filesize(huffmanPath + FilePaths.HUFFMAN_ENCODING_EXTENSION) +
					IOHelper.filesize(huffmanPath + FilePaths.ALPHABET_EXTENSION);
			System.out.println("Compressed File Size = " + compressedSize + " bytes");
			System.out.printf("Compression Ratio = %.2f\n", 1.00 * filesize / compressedSize);
		}
	}

	private static void bwt_expand() throws Exception {
		for (int i = 0; i < FilePaths.NUM_DATA; i++) {

			System.out.println("\n**********************");
			String path = FilePaths.DATA_DIRECTORY + FilePaths.DATA[i];
			System.out.println("\nReading file for verification: " + path);
			ArrayList<Character> data = IOHelper.readFile(path);
			System.out.println("File loaded.");

			String huffmanPath = FilePaths.BWT_COMPRESSED_DIRECTORY + FilePaths.DATA[i];
			String alphaPath = FilePaths.BWT_COMPRESSED_DIRECTORY + FilePaths.DATA[i];

			long startTime = System.currentTimeMillis();

			System.out.println("Reading compressed files...");
			HuffmanElement huffFromFile = HuffmanIOHelper.readHuffman(huffmanPath);

			System.out.println("Huffman -> MTF Phase...");
			ArrayList<Integer> decodedMTF = Huffman.decode(huffFromFile);

			System.out.println("MTF -> BWT Phase...");
			ArrayList<Character> alphabet = IOHelper.readAlphabet(alphaPath);
			ArrayList<Character> decodedBWT = MTF.decode(new MTFElement(alphabet, decodedMTF));

			System.out.println("BWT -> Decoded Data Phase...");
			ArrayList<Character> decodedData = BWT.decode(decodedBWT);

			System.out.printf("\nDecompression took %.2f seconds\n", (System.currentTimeMillis() - startTime) / 1000d);

			HelperFunctions.verifyEquality(data, decodedData);
		}
	}
}