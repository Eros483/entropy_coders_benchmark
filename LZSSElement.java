import java.util.ArrayList;

public class LZSSElement {

    public String identifier;
    public ArrayList<Integer> delta;
    public ArrayList<Integer> length;
    public ArrayList<Character> next;

    public LZSSElement(String identifier, ArrayList<Integer> delta, ArrayList<Integer> length, ArrayList<Character> next) {
        this.identifier = identifier;
        this.delta = delta;
        this.length = length;
        this.next = next;
    }

    public LZSSElement() {
    }

    public String toString() {
        StringBuilder out = new StringBuilder();
        for (int i = 0, d = 0, n = 0; i < identifier.length(); i++)
            if (identifier.charAt(i) == '0') {
                out.append(String.format("<%c> ", next.get(n)));
                n++;
            }
            else {
                out.append(String.format("<%d,%d,%c> ", delta.get(d), length.get(d), next.get(n)));
                n++;
                d++;
            }
        return out.toString();
    }
}
