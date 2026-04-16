package core;

/**
 * Data carrier for arithmetic coding results.
 */
public class ArithmeticElement {

    public byte[] encodedBytes;
    public long totalBits;
    public int[] alphabet;
    public int[] cumFreqs;
    public int originalLength;

    public ArithmeticElement() {}

    public ArithmeticElement(byte[] encodedBytes, long totalBits,
                              int[] alphabet, int[] cumFreqs, int originalLength) {
        this.encodedBytes = encodedBytes;
        this.totalBits = totalBits;
        this.alphabet = alphabet;
        this.cumFreqs = cumFreqs;
        this.originalLength = originalLength;
    }
}
