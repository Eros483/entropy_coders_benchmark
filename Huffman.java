import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

class BinaryTreeNode implements Comparable<BinaryTreeNode> {

	int id;
	int value;
	BinaryTreeNode left, right;

	BinaryTreeNode(int id, int value) {
		this.id = id;
		this.value = value;
		this.left = null;
		this.right = null;
	}

	@Override
	public int compareTo(BinaryTreeNode that) {
		return this.value - that.value;
	}

	public String toString() {
		String out = "<" + String.valueOf(value) + (id == -1 ? "" : ", " + id);
		out += ", " + (left == null ? "null" : left.value);
		out += ", " + (right == null ? "null" : right.value);
		out += ">";
		return out;
	}
}

public class Huffman {

	private static void preOrder(BinaryTreeNode root) {
		if (root == null)
			return;
		System.out.println(root);
		preOrder(root.left);
		preOrder(root.right);
	}

	public static HuffmanElement encode(ArrayList<Integer> msg) throws Exception {
		if (msg.size() == 0)
			return new HuffmanElement();
		ArrayList<Integer> alphabet = HelperFunctions.getSortedAlphabet(msg);
		if (alphabet.size() == 1) {
			return new HuffmanElement(alphabet.get(0), msg.size());
		}
		HashMap<Integer, Integer> freqMap = HelperFunctions.getFrequencies(msg);
		PriorityQueue<BinaryTreeNode> pq = new PriorityQueue<>();
		int id = 0;
		for (Integer c : alphabet) {
			BinaryTreeNode btNode = new BinaryTreeNode(id++, freqMap.get(c));
			pq.offer(btNode);
		}
		while (pq.size() > 1) {
			BinaryTreeNode min = pq.remove();
			BinaryTreeNode secondMin = pq.remove();
			BinaryTreeNode parent = new BinaryTreeNode(-1, min.value + secondMin.value);
			parent.left = min;
			parent.right = secondMin;
			pq.offer(parent);
		}

		HashMap<Integer, String> mappingTable = new HashMap<>();
		// preOrder(pq.peek());
		createTable(mappingTable, alphabet, pq.remove(), "");
		StringBuilder bitString = new StringBuilder();
		for (Integer x : msg)
			bitString.append(mappingTable.get(x));
		return new HuffmanElement(bitString.toString(), mappingTable);
	}

	private static void createTable(HashMap<Integer, String> mappingTable, ArrayList<Integer> alphabet, BinaryTreeNode node, String encoding) {
		if (node.left == null && node.right == null)
			mappingTable.put(alphabet.get(node.id), encoding);
		else {
			if (node.left != null)
				createTable(mappingTable, alphabet, node.left, encoding + "0");
			if (node.right != null)
				createTable(mappingTable, alphabet, node.right, encoding + "1");
		}
	}

	public static ArrayList<Integer> decode(HuffmanElement huffObj) throws Exception {
		HashMap<String, Integer> reverseMap = new HashMap<>();
		HashMap<Integer, String> mappingTable = huffObj.mappingTable;
		for (int x : mappingTable.keySet())
			reverseMap.put(mappingTable.get(x), x);
		ArrayList<Integer> decodedMsg = new ArrayList<>();
		StringBuilder encode = new StringBuilder();
		for (int i = 0; i < huffObj.encoding.length(); i++) {
			encode.append(huffObj.encoding.charAt(i));
			if (reverseMap.containsKey(encode.toString())) {
				decodedMsg.add(reverseMap.get(encode.toString()));
				encode = new StringBuilder();
			}
		}
		return decodedMsg;
	}
}
