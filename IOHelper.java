import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

public class IOHelper {

	/**
	 * Reads the data. Appends EOF '\0' at the end.
	 */
	static ArrayList<Character> readFile(String filePath) throws IOException {
		BufferedReader fileReader = new BufferedReader(new FileReader(filePath));
		int val = 0;
		ArrayList<Character> content = new ArrayList<>();
		while ((val = fileReader.read()) != -1) {
			content.add((char) val);
		}
		content.add('\0');
		fileReader.close();
		return content;
	}

	/**
	 * Writes a byte array to the file.
	 */
	public static void writeBytes(byte[] bytes, String filePath) throws IOException {
		OutputStream os = new FileOutputStream(filePath);
		os.write(bytes);
		os.close();
	}

	/**
	 * Reads bytes from the file into an array.
	 */
	public static byte[] readBytes(String filePath) throws IOException {
		File file = new File(filePath);
		byte[] bFile = new byte[(int) file.length()];
		FileInputStream fileInputStream = new FileInputStream(file);
		fileInputStream.read(bFile);
		fileInputStream.close();
		return bFile;
	}

	/**
	 * Writes an Integer-String map to a file.
	 */
	public static void writeMap(HashMap<Integer, String> map, String filePath) throws IOException {
		StringBuilder sb = new StringBuilder();
		Iterator<Integer> it = map.keySet().iterator();
		int key;
		String value;
		if (it.hasNext()) {
			key = it.next();
			sb.append(String.format("%d:%s", key, map.get(key)));
		}
		while (it.hasNext()) {
			key = it.next();
			sb.append(String.format("\n%d:%s", key, map.get(key)));
		}
		BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
		bw.write(sb.toString());
		bw.close();
	}

	/**
	 * Reads an Integer-String map from a file.
	 */
	public static HashMap<Integer, String> readMap(String filePath) throws FileNotFoundException {
		HashMap<Integer, String> map = new HashMap<>();
		Scanner in = new Scanner(new FileReader(filePath));
		while (in.hasNextLine()) {
			String line = in.nextLine();
			String[] tokens = line.split(":");
			map.put(Integer.parseInt(tokens[0]), tokens[1]);
		}
		in.close();
		return map;
	}

	public static void writeAlphabet(ArrayList<Character> alphabet, String filePath) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (Character c : alphabet){
			sb.append((int) c);
			sb.append("\n");
		}
		sb.deleteCharAt(sb.length() - 1);
		BufferedWriter bw = new BufferedWriter(new FileWriter(filePath + FilePaths.ALPHABET_EXTENSION));
		bw.write(sb.toString());
		bw.close();
	}

	public static ArrayList<Character> readAlphabet(String filePath) throws FileNotFoundException {
		ArrayList<Character> alphabet = new ArrayList<>();
		Scanner in = new Scanner(new FileReader(filePath + FilePaths.ALPHABET_EXTENSION));
		while (in.hasNextLine()) {
			int ascii = Integer.parseInt(in.nextLine());
			alphabet.add((char)ascii);
		}
		in.close();
		return alphabet;
	}

	public static long filesize(String filePath) {
		File file = new File(filePath);
		long size = file.length();
		return size;
	}
}
