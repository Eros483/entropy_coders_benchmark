import java.util.ArrayList;

public class LZSS {

    private static final int LOOKAHEAD_BUFFER = 128;
    private static final int WINDOW = 1024;
    private static final int LENGTH_THRESHOLD = 4;

    public static LZSSElement encode(ArrayList<Character> msg) {
        // add identifier column
        StringBuilder identifier = new StringBuilder();
        ArrayList<Integer> deltas = new ArrayList<>();
        ArrayList<Integer> lengths = new ArrayList<>();
        ArrayList<Character> nextChars = new ArrayList<>();

        // first index is always a character
        identifier.append(0);
        nextChars.add(msg.get(0));

        int c = 1; // current position in message
        while (c < msg.size()) {
            int length = 0, delta = 0;
            for (int j = c - 1; j >= 0; j--) {
                if (j < c - WINDOW) // don't look behind the left boundary of the window
                    break;
                int l = 0;
                // Find the longest match of the prefix starting at within the window.
                // However, don't look beyond lookahead buffer for finding the match.
                while (c + l < msg.size() && l < LOOKAHEAD_BUFFER && msg.get(c + l) == msg.get(j + l))
                    l++;
                if (l > length) {
                    length = l;
                    delta = c - j;
                }
            }
            if (length == LOOKAHEAD_BUFFER) {
                // if a complete match was found within the lookahead buffer with a position j in the window,
                // then extend the match until a mismatch occurs
                int j = c - delta;
                while (c + length < msg.size() && msg.get(c + length) == msg.get(j + length))
                    length++;
            }

            // add entry according to LZSS logic
            if(length > LENGTH_THRESHOLD)
            {
              identifier.append(1);
              deltas.add(delta);
              lengths.add(length);
              nextChars.add(msg.get(c + length));
            }
            else
            {
              for(int j = 0; j <= length; j++)
              {
                identifier.append(0);
                nextChars.add(msg.get(c+j));
              }
            }

            // jump to next unmatched character
            c += 1 + length;
        }
        return new LZSSElement(identifier.toString(), deltas, lengths, nextChars);
    }

    public static ArrayList<Character> decode(LZSSElement lzss) {
        ArrayList<Character> msg = new ArrayList<>();
        int numIdentifiers = lzss.identifier.length();
        int d = 0;
        for (int t = 0; t < numIdentifiers; t++) {
            if (lzss.identifier.charAt(t) == '1')
            {
              for (int j = 0; j < lzss.length.get(d); j++)
              {
                msg.add(msg.get(msg.size()-lzss.delta.get(d)));
              }
              d++;
            }
            msg.add(lzss.next.get(t));
        }
        return msg;
    }
}
