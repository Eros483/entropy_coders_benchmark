import java.util.ArrayList;
import java.util.HashMap;

public class LZ78 {
    private static final int MAX_DICT_SIZE = 4096;

    public static LZ78Element encode(ArrayList<Character> msg) {
        ArrayList<Integer> dictIndices = new ArrayList<>();
        ArrayList<Character> nextChars = new ArrayList<>();
        
        HashMap<String, Integer> dictionary = new HashMap<>();
        dictionary.put("", 0);
        int nextIndex = 1;
        
        int i = 0;
        while (i < msg.size()) {
            int matchIndex = 0;
            int matchLength = 0;
            
            // Find the longest match in the dictionary
            for (int len = 1; i + len <= msg.size(); len++) {
                StringBuilder sb = new StringBuilder();
                for (int k = i; k < i + len; k++) {
                    sb.append(msg.get(k));
                }
                String substr = sb.toString();
                
                if (dictionary.containsKey(substr)) {
                    matchIndex = dictionary.get(substr);
                    matchLength = len;
                } else {
                    if (nextIndex < MAX_DICT_SIZE) {
                        dictionary.put(substr, nextIndex++);
                    }
                    break;
                }
            }
            
            dictIndices.add(matchIndex);
            
            // Append the next character, or null-terminator if at end
            if (i + matchLength < msg.size()) {
                nextChars.add(msg.get(i + matchLength));
            } else {
                nextChars.add('\0'); 
            }
            
            // Increment pointer by match length + 1 (the new character)
            i += matchLength + 1;
        }
        
        return new LZ78Element(dictIndices, nextChars);
    }

    public static ArrayList<Character> decode(LZ78Element lz78) {
        ArrayList<Character> msg = new ArrayList<>();
        ArrayList<String> dictionary = new ArrayList<>();
        
        dictionary.add("");
        
        for (int t = 0; t < lz78.dictIndex.size(); t++) {
            int index = lz78.dictIndex.get(t);
            char next = lz78.nextChar.get(t);
            
            String entry = dictionary.get(index);
            
            if (next != '\0') {
                entry += next;
            }
            
            for (char c : entry.toCharArray()) {
                msg.add(c);
            }
            
            if (dictionary.size() < MAX_DICT_SIZE) {
                dictionary.add(entry);
            }
        }
        
        return msg;
    }
}