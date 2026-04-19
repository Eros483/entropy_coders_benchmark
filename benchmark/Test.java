package benchmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import core.*;
import compress.*;

public class Test {

	private static void testHuffman(String str) throws Exception {
		System.out.println("Original string: " + str);
		ArrayList<Character> stringAsVector = HelperFunctions.stringToArrayList(str);
		HuffmanElement huff = Huffman.encode(HelperFunctions.charsToInts(stringAsVector));
		String encoding = huff.encoding;
		System.out.println("Mapping:  " + huff.mappingTable);
		System.out.println("Encoding: " + encoding);
		ArrayList<Character> originalMessage = HelperFunctions.intsToChars(Huffman.decode(huff));
		System.out.println("Decoded string:  " + HelperFunctions.arrayListToString(originalMessage) + "\n");
	}

	private static void testManberMyers(String str) {
		ArrayList<Character> stringAsVector = HelperFunctions.stringToArrayList(str);
		System.out.print("Suffix Array for " + str + ": ");
		int[] sa = SuffixArray.manberMyers(stringAsVector);
		System.out.println(Arrays.toString(sa));
	}

	private static void testBruteForceSA(String str) {
		ArrayList<Character> stringAsVector = HelperFunctions.stringToArrayList(str);
		System.out.print("Suffix Array for " + str + ": ");
		int[] sa = SuffixArray.bruteforce(stringAsVector);
		System.out.println(Arrays.toString(sa));
	}

	private static void testBitPacker(String str) {
		System.out.println("Binary String: " + str);
		byte[] bytes = BitPacker.pack(str);
		System.out.println("Packed Bytes:  " + Arrays.toString(bytes));
		String bytesToBits = BitPacker.unpack(bytes);
		System.out.println("Unpacked Bits: " + bytesToBits);
		System.out.println();
	}

	private static void testBWT(String str) {

		System.out.println("String:  " + str);
		ArrayList<Character> data = HelperFunctions.stringToArrayList(str);
		data.add('\0');
		ArrayList<Character> bwt = BWT.encode(data);
		System.out.println("BWT:     " + bwt);
		System.out.println("Decoded: " + HelperFunctions.arrayListToString(BWT.decode(bwt)));
		System.out.println();
	}

	private static void testMTF(String str) {
		System.out.println("Original: " + str);
		ArrayList<Character> strAsVector = HelperFunctions.stringToArrayList(str);
		MTFElement mtf = MTF.encode(strAsVector);
		System.out.println("MTF: " + mtf.mtf);
		ArrayList<Character> decodedStr = MTF.decode(mtf);
		System.out.println("Decoded:  " + HelperFunctions.arrayListToString(decodedStr));
		System.out.println();
	}

	public static void testHuffmanIO(String str) throws IOException, Exception {
		System.out.println("String:         " + str);
		ArrayList<Character> strAsVector = HelperFunctions.stringToArrayList(str);
		HuffmanElement huffObj = Huffman.encode(HelperFunctions.charsToInts(strAsVector));
		HuffmanIOHelper.writeHuffman(huffObj, FilePaths.HUFFMAN_IO_TEST);
		HuffmanElement huffObjFromFile = HuffmanIOHelper.readHuffman(FilePaths.HUFFMAN_IO_TEST);
		ArrayList<Character> decodedHuffmanVector = HelperFunctions.intsToChars(Huffman.decode(huffObjFromFile));
		String decodedStr = HelperFunctions.arrayListToString(decodedHuffmanVector);
		System.out.println("Decoded String: " + decodedStr);
		System.out.println();
	}

	public static void testLZ77(String str) {
		System.out.println("Original String:  " + str);
		ArrayList<Character> data = HelperFunctions.stringToArrayList(str);
		data.add('\0');
		LZ77Element lz77 = LZ77.encode(data);
		System.out.println("Encoding: " + lz77);
		ArrayList<Character> output = LZ77.decode(lz77);
		System.out.println("Decoded String:   " + HelperFunctions.arrayListToString(output));
		System.out.println();
	}
	public static void testLZ78(String str) throws Exception {
	    System.out.println("Original String:  " + str);
	    ArrayList<Character> data = HelperFunctions.stringToArrayList(str);
		
	    // Add EOF terminator as per your project's convention
	    data.add('\0'); 
		
	    // Step 1: Encode
	    LZ78Element lz78 = LZ78.encode(data);
	    System.out.println("Encoding: " + lz78);
		
	    // Step 2: Decode
	    ArrayList<Character> output = LZ78.decode(lz78);
	    System.out.println("Decoded String:   " + HelperFunctions.arrayListToString(output));
		
	    // Step 3: Verify equality
	    HelperFunctions.verifyEquality(data, output);
	    System.out.println("Verification: PASSED\n");
	}

	public static void testLZSS(String str) {
		System.out.println("Original String:  " + str);
		ArrayList<Character> data = HelperFunctions.stringToArrayList(str);
		data.add('\0');
		LZSSElement lzss = LZSS.encode(data);
		System.out.println("Encoding: " + lzss);
		ArrayList<Character> output = LZSS.decode(lzss);
		System.out.println("Decoded String:   " + HelperFunctions.arrayListToString(output));
		System.out.println();
	}

	private static void testCorrectness() throws Exception {
		System.out.println("*** Huffman Test ***");
		testHuffman("Y2XEYX2XYX2CEXEX2Y2ZYX2ZYZE2Y2ZEYXEX2YZYCY2ZEX");

		System.out.println("*** Test Huffman IO ***");
		testHuffmanIO("Y2XEYX2XYX2CEXEX2Y2ZYX2ZYZE2Y2ZEYXEX2YZYCY2ZEX");

		System.out.println("*** Manber Myers Test ***");
		testManberMyers("banana");
		testManberMyers("mississippi");
		testManberMyers("mississippilesslyormississippily");
		System.out.println();

		System.out.println("*** Brute-Force Suffix Array Test ***");
		testBruteForceSA("banana");
		testBruteForceSA("mississippi");
		testBruteForceSA("mississippilesslyormississippily");
		System.out.println();

		System.out.println("*** Packing/Unpacking Test ***");
		testBitPacker("0111001100001001111101110100");
		testBitPacker("01110011000010011111001001");
		testBitPacker("10110011011010011101011101001101");

		System.out.println("*** Test LZ77 ***");
		testLZ77("mississippimissississispsisp");
		
		System.out.println("*** LZ78 Test ***");
    	testLZ78("ABABABA"); // Simple repeating pattern
    	testLZ78("BABAABRRRA"); // Complex pattern

		System.out.println("*** Test LZSS ***");
		testLZSS("mississippimissississispsisp");

		System.out.println("*** BWT Test ***");
		testBWT("banana");
		testBWT("mississippi");
		testBWT("mississippilesslyormississippily");
		testBWT("TAT");

		System.out.println("*** MTF Test ***");
		testMTF("spsssmmipissssiiii");
	}

	private static void loadTest() throws Exception {
		Master.lz77();
		System.out.println("**********************\n");
		Master.lzss();
		System.out.println("**********************\n");
		Master.bwt();
		System.out.println("**********************\n");
	}

	public static void main(String args[]) throws Exception {
		testCorrectness();
		loadTest();
	}
}
