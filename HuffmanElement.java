import java.util.HashMap;

public class HuffmanElement {

	public String encoding;
	public HashMap<Integer, String> mappingTable;

	public HuffmanElement(String encoding, HashMap<Integer, String> mappingTable) {
		this.encoding = encoding;
		this.mappingTable = mappingTable;
	}

	public HuffmanElement() {
		encoding = "";
		mappingTable = new HashMap<>();
	}

	public HuffmanElement(int character, int length) {
		StringBuilder out = new StringBuilder(length);
		for (int i = 0; i < length; i++)
			out.append('0');
		encoding = out.toString();
		mappingTable = new HashMap<>();
		mappingTable.put(character, "0");
	}

	public int size() {
		int _size = 0;
		for (int i : mappingTable.keySet())
			_size += String.valueOf(i).length() + 1 + mappingTable.get(i).length();
		_size += mappingTable.size() - 1; // for newline characters
		return (int) (1 + Math.ceil(encoding.length() / 8.0) + _size); // num_bytes = ceil(num_bits/8)
	}
}
