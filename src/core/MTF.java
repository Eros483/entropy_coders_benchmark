package core;

import java.util.ArrayList;
import java.util.Collections;

public class MTF {

	public static MTFElement encode(ArrayList<Character> str) {
    // get sorted alphabet
    ArrayList<Character> alphabet = HelperFunctions.getSortedAlphabet(str);

    MTFElement mtf = new MTFElement();
    mtf.alphabet = new ArrayList<>(alphabet);

    for (int i = 0; i < str.size(); i++)
    {
      int index = alphabet.indexOf(str.get(i));
      mtf.mtf.add(index);
      alphabet.add(0, alphabet.remove(index));
    }

    return mtf;
	}

	public static ArrayList<Character> decode(MTFElement mtf) {
    // init decoded array
    ArrayList<Character> str = new ArrayList<Character>();

    for (int i = 0; i < mtf.mtf.size(); i++)
    {
      int index = mtf.mtf.get(i);
      str.add(mtf.alphabet.get(index));
      mtf.alphabet.add(0, mtf.alphabet.remove(index));
    }

    return str;
	}
}
