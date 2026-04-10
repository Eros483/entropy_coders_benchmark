import java.io.IOException;
import java.util.HashMap;

public class HuffmanIOHelper {

	/**
	 * Writes the Huffman object into a file.
	 * <p>
	 * The encoding part is written as a byte file. So, the bits need to packed
	 * first using BitPacker/pack method.
	 * <p>
	 * The map is written using the writeMap function.
	 */
	public static void writeHuffman(HuffmanElement obj, String filePath) throws IOException {
		IOHelper.writeBytes(BitPacker.pack(obj.encoding), filePath + FilePaths.HUFFMAN_ENCODING_EXTENSION);
		IOHelper.writeMap(obj.mappingTable, filePath + FilePaths.HUFFMAN_MAP_EXTENSION);
	}

	/**
	 * Reads the Huffman object from the file.
	 * <p>
	 * The encoding part is read from the byte file; note that the byte file needs
	 * to be unpacked using BitPacker/unpack method.
	 * <p>
	 * The map is read using the readMap function.
	 */
	public static HuffmanElement readHuffman(String filePath) throws IOException {
		byte[] bytes = IOHelper.readBytes(filePath + FilePaths.HUFFMAN_ENCODING_EXTENSION);
		String decompressed = BitPacker.unpack(bytes);
		HashMap<Integer, String> decodedHuffMap = IOHelper.readMap(filePath + FilePaths.HUFFMAN_MAP_EXTENSION);
		HuffmanElement decodedHuffObj = new HuffmanElement(decompressed, decodedHuffMap);
		return decodedHuffObj;
	}
}
