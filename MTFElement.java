import java.util.ArrayList;

public class MTFElement {

    public ArrayList<Character> alphabet;
    public ArrayList<Integer> mtf;

    public MTFElement(ArrayList<Character> alphabet, ArrayList<Integer> mtf) {
        this.alphabet = alphabet;
        this.mtf = mtf;
    }

    public MTFElement() {
      mtf = new ArrayList<>();
    }
}
