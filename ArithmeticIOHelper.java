import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Serializes and deserializes ArithmeticElement to/from disk.
 *
 * File format:
 *   4 bytes: originalLength (int)
 *   4 bytes: totalBits (int)
 *   4 bytes: encodedBytes length m (int)
 *   m bytes: encoded data
 *   4 bytes: alphabet length n (int)
 *   4*n bytes: alphabet entries (int)
 *   4 bytes: cumFreqs length (int)
 *   4*(n+1) bytes: cumFreqs entries (int)
 */
public class ArithmeticIOHelper {

    public static void write(ArithmeticElement el, String path) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(path)))) {

            out.writeInt(el.originalLength);
            out.writeInt((int) el.totalBits);
            out.writeInt(el.encodedBytes.length);
            out.write(el.encodedBytes);
            out.writeInt(el.alphabet.length);
            for (int s : el.alphabet) out.writeInt(s);
            out.writeInt(el.cumFreqs.length);
            for (int c : el.cumFreqs) out.writeInt(c);
        }
    }

    public static ArithmeticElement read(String path) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(path))) {
            int origLen = in.readInt();
            int totalBits = in.readInt();
            int byteLen = in.readInt();
            byte[] bytes = new byte[byteLen];
            in.readFully(bytes);
            int alphaLen = in.readInt();
            int[] alphabet = new int[alphaLen];
            for (int i = 0; i < alphaLen; i++) alphabet[i] = in.readInt();
            int cumLen = in.readInt();
            int[] cumFreqs = new int[cumLen];
            for (int i = 0; i < cumLen; i++) cumFreqs[i] = in.readInt();
            return new ArithmeticElement(bytes, totalBits, alphabet, cumFreqs, origLen);
        }
    }
}
