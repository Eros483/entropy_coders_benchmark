package core;

import java.util.ArrayList;

public class LZ78Element {
    public ArrayList<Integer> dictIndex;
    public ArrayList<Character> nextChar;

    public LZ78Element(ArrayList<Integer> dictIndex, ArrayList<Character> nextChar) {
        this.dictIndex = dictIndex;
        this.nextChar = nextChar;
    }

    public LZ78Element() {
        dictIndex = new ArrayList<>();
        nextChar = new ArrayList<>();
    }

    public String toString() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < dictIndex.size(); i++)
            out.append(String.format("<%d,%c> ", dictIndex.get(i), nextChar.get(i)));
        return out.toString();
    }
}