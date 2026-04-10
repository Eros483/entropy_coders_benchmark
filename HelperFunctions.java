import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.TreeSet;

public class HelperFunctions {

	/**
	 * Converts a bit-string to decimal
	 */
	public static int binaryToDecimal(String str) {
		int dec = 0;
		for (int i = str.length() - 1, k = 1; i >= 0; i--, k = k << 1)
			if (str.charAt(i) == '1')
				dec += k;
		return dec;
	}

	/**
	 * Converts n into a binary string of length b.
	 */
	public static String decimalToBinary(int n, int b) {
		StringBuilder bin = new StringBuilder(b);
		for (int i = 0; i < b; i++)
			bin.append("0");
		b--;
		while (n > 0) {
			bin.replace(b, b + 1, "" + n % 2);
			n = n >> 1;
			b--;
		}
		return bin.toString();
	}

	/**
	 * Returns the distinct characters in sorted order.
	 */
	public static <T> ArrayList<T> getSortedAlphabet(ArrayList<T> str) {
		TreeSet<T> sigma = new TreeSet<>();
		for (T c : str)
			sigma.add(c);
		ArrayList<T> sigmaAsArray = new ArrayList<>(sigma.size());
		for (T c : sigma)
			sigmaAsArray.add(c);
		return sigmaAsArray;
	}

	/**
	 * Returns frequency of each character.
	 */
	public static <T> HashMap<T, Integer> getFrequencies(ArrayList<T> str) {
		HashMap<T, Integer> freqMap = new HashMap<>();
		for (T x : str)
			if (freqMap.containsKey(x))
				freqMap.put(x, freqMap.get(x) + 1);
			else
				freqMap.put(x, 1);
		return freqMap;
	}

	/**
	 * Segregates each character into a string arraylist
	 */
	public static ArrayList<Character> stringToArrayList(String str) {
		ArrayList<Character> stringAsList = new ArrayList<>();
		for (int i = 0; i < str.length(); i++)
			stringAsList.add(str.charAt(i));
		return stringAsList;
	}

	/**
	 * Concatenates all characters into a string.
	 */
	public static <T> String arrayListToString(ArrayList<T> list) {
		StringBuilder sb = new StringBuilder(list.size());
		for (int i = 0; i < list.size(); i++)
			sb.append(list.get(i));
		return sb.toString();
	}

	/**
	 * Verifies if the content of both arraylists are the same.
	 */
	public static <T> void verifyEquality(ArrayList<T> arg1, ArrayList<T> arg2) throws Exception {
		if (arg1.size() == arg2.size()) {
			for (int i = 0; i < arg1.size(); i++)
				if (!arg1.get(i).equals(arg2.get(i)))
					throw new Exception("Something is wrong!");
		} else
			throw new Exception("Something is wrong!");

	}

	/**
	 * Converts an arraylist of characters to an arraylist of integers
	 */
	public static ArrayList<Integer> charsToInts(ArrayList<Character> arg) {
		ArrayList<Integer> out = new ArrayList<>(arg.size());
		for (int c : arg)
			out.add(c);
		return out;
	}

	/**
	 * Converts an arraylist of integers to an arraylist of characters.
	 * CAUTION: Conversion is only possible for integers in [0, 255]
	 */
	public static ArrayList<Character> intsToChars(ArrayList<Integer> arg) {
		ArrayList<Character> out = new ArrayList<>(arg.size());
		for (int c : arg) {
			if (c < 256 && c >= 0)
				out.add((char) c);
			else throw new InputMismatchException(c + "is not in the range [0, 255]");
		}
		return out;
	}
}
