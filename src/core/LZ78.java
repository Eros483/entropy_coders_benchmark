package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LZ78 {
    private static final int MAX_DICT_SIZE = 4096;

    private static class TrieNode {
        int index;
        Map<Character, TrieNode> children = new HashMap<>();
        TrieNode(int index) { this.index = index; }
    }

    public static LZ78Element encode(ArrayList<Character> msg) {
        ArrayList<Integer> dictIndices = new ArrayList<>();
        ArrayList<Character> nextChars = new ArrayList<>();
        TrieNode root = new TrieNode(0);
        int nextIndex = 1;

        int i = 0;
        while (i < msg.size()) {
            TrieNode current = root;
            int matchLength = 0;

            while (i + matchLength < msg.size()) {
                char c = msg.get(i + matchLength);
                if (current.children.containsKey(c)) {
                    current = current.children.get(c);
                    matchLength++;
                } else {
                    break;
                }
            }

            dictIndices.add(current.index);
            char next = (i + matchLength < msg.size()) ? msg.get(i + matchLength) : '\0';
            nextChars.add(next);

            if (nextIndex < MAX_DICT_SIZE && next != '\0') {
                current.children.put(next, new TrieNode(nextIndex++));
            }
            i += matchLength + 1;
        }
        return new LZ78Element(dictIndices, nextChars);
    }

    public static ArrayList<Character> decode(LZ78Element lz78) {
        ArrayList<Character> msg = new ArrayList<>();
        ArrayList<String> dictionary = new ArrayList<>();
        dictionary.add("");

        for (int t = 0; t < lz78.dictIndex.size(); t++) {
            String entry = dictionary.get(lz78.dictIndex.get(t));
            if (lz78.nextChar.get(t) != '\0') entry += lz78.nextChar.get(t);
            
            for (char c : entry.toCharArray()) msg.add(c);
            if (dictionary.size() < MAX_DICT_SIZE) dictionary.add(entry);
        }
        return msg;
    }
}