/**
 * Data carrier for rANS encoding results.
 *
 * encodedBytes: [norm bytes (LSB-first)] [8-byte final state (big-endian)]
 * cumFreqs: cumulative frequency table, length = alphabet.length + 1
 */
public class rANSElement {

    public byte[] encodedBytes;
    public int[] alphabet;
    public int[] cumFreqs;
    public int originalLength;

    public rANSElement() {}

    public rANSElement(byte[] encodedBytes, int[] alphabet,
                        int[] cumFreqs, int originalLength) {
        this.encodedBytes = encodedBytes;
        this.alphabet = alphabet;
        this.cumFreqs = cumFreqs;
        this.originalLength = originalLength;
    }
}
