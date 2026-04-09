
import java.util.*;

public final class BWT {

    /**
     * The last char in str is assumed to be EOF '\0'.
     */
	public static ArrayList<Character> encode(ArrayList<Character> str) {
    // init BWT and SA
    ArrayList<Character> BWT = new ArrayList<>();
    int SA[] = SuffixArray.compute(str);

    // calculate BWT from SA
    for (int i = 0; i < str.size(); i++)
    {
      if (SA[i] > 0)
        BWT.add(str.get(SA[i]-1));
      else
        BWT.add(str.get(str.size()-1));
    }

    return BWT;
	}

    /**
     * Assumes EOF '\0' as the unique last char in the original string.
     */
	public static ArrayList<Character> decode(ArrayList<Character> bwt) {

    // initialize R and C
    int R[] = new int[bwt.size()];
    HashMap<Character, Integer> C = new HashMap<>();

    // populating R
    TreeMap<Character, Integer> freq = new TreeMap<>();
    for (int i = 0; i < bwt.size(); i++)
    {
      freq.merge(bwt.get(i), 1, Integer::sum);
      R[i] = freq.get(bwt.get(i));
    }

    // populating C
    int counter = 0;
    for (Map.Entry<Character, Integer> entry : freq.entrySet())
    {
      C.put(entry.getKey(), counter);
      counter =  counter + entry.getValue();
    }

    // recreating original string
    int x = 0;
    for (int i = 0; i < bwt.size(); i++)
    {
      if (bwt.get(i) == '\0')
      {
        x = i;
        break;
      }
    }

    // allocate container for OG string
    char original[] = new char[bwt.size()];
    for (int i = bwt.size()-1; i >= 0; i--)
    {
      original[i] = bwt.get(x);
      x = C.get(bwt.get(x)) + R[x] - 1;
    }
    ArrayList<Character> og_arrlist = new ArrayList<>(original.length);
    for (char c : original) og_arrlist.add(c);

    return og_arrlist;
	}
}
