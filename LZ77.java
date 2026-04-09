import java.util.ArrayList;

public class LZ77 {

    private static final int LOOKAHEAD_BUFFER = 128;
    private static final int WINDOW = 1024;

    public static LZ77Element encode(ArrayList<Character> msg) {
        ArrayList<Integer> deltas = new ArrayList<>();
        ArrayList<Integer> lengths = new ArrayList<>();
        ArrayList<Character> nextChars = new ArrayList<>();

        deltas.add(0);
        lengths.add(0);
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

            // add triplet
            deltas.add(delta);
            lengths.add(length);
            nextChars.add(msg.get(c + length));

            // jump to next unmatched character
            c += 1 + length;
        }
        return new LZ77Element(deltas, lengths, nextChars);
    }

    public static ArrayList<Character> decode(LZ77Element lz77) {
        ArrayList<Character> msg = new ArrayList<>();
        int numTriplets = lz77.length.size();
        for (int t = 0; t < numTriplets; t++) {
            int length = lz77.length.get(t);
            for (int j = 1; j <= length; j++) {
                // starting from delta characters behind, append length many characters from the decoded message
                char c = msg.get(msg.size() - lz77.delta.get(t));
                msg.add(c);
            }
            msg.add(lz77.next.get(t));
        }
        return msg;
    }
}
