package compress;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import core.rANSElement;

/** Binary serialization for rANSElement. */
public class rANSIOHelper {

    public static void write(rANSElement el, String path) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(path))) {
            out.writeInt(el.originalLength);
            out.writeInt(el.encodedBytes.length);
            out.write(el.encodedBytes);
            out.writeInt(el.alphabet.length);
            for (int s : el.alphabet) out.writeInt(s);
            out.writeInt(el.cumFreqs.length);
            for (int c : el.cumFreqs) out.writeInt(c);
        }
    }

    public static rANSElement read(String path) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(path))) {
            int orig = in.readInt();
            int bLen = in.readInt();
            byte[] bytes = new byte[bLen];
            in.readFully(bytes);
            int aLen = in.readInt();
            int[] alphabet = new int[aLen];
            for (int i = 0; i < aLen; i++) alphabet[i] = in.readInt();
            int cLen = in.readInt();
            int[] cumFreqs = new int[cLen];
            for (int i = 0; i < cLen; i++) cumFreqs[i] = in.readInt();
            return new rANSElement(bytes, alphabet, cumFreqs, orig);
        }
    }
}
