import java.util.ArrayList;

public class LZ77Element {

    public ArrayList<Integer> delta;
    public ArrayList<Integer> length;
    public ArrayList<Character> next;

    public LZ77Element(ArrayList<Integer> delta, ArrayList<Integer> length, ArrayList<Character> next) {
        this.delta = delta;
        this.length = length;
        this.next = next;
    }

    public String toString() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < delta.size(); i++)
            out.append(String.format("<%d,%d,%c> ", delta.get(i), length.get(i), next.get(i)));
        return out.toString();
    }
}
