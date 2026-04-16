package core;

import java.lang.Math;

public class BitPacker {

	/**
	 * Packs 8 bits at a time into a byte. bits must contain only 1 & 0. The total
	 * number of bytes = 1 + ceil(bits.length/8). Last byte indicates length of the
	 * last packed byte.
	 */
	public static byte[] pack(String bits) {

    // declare byte array
    byte bytearray[] = new byte[1+(int)Math.ceil(bits.length()/8.0)];

    // store length of last few bits
    bytearray[bytearray.length-1] = (bits.length() % 8 == 0) ? (byte) 8 : (byte) (bits.length() % 8);

    // handling empty string
    if(bits.length() == 0) return bytearray;

    // pack 8 bits at a time to byte array 
    for (int i = 0; i < bytearray.length-2; i++)
    {
      bytearray[i] = (byte) HelperFunctions.binaryToDecimal(bits.substring(i*8, (i+1)*8));
    }

    // pack last bits
    bytearray[bytearray.length-2] = (byte) HelperFunctions.binaryToDecimal(bits.substring((bytearray.length-2) * 8,(bytearray.length-2) * 8 + (bytearray[bytearray.length-1] & 0xFF)));

		return bytearray;
	}

	/**
	 * Unpacks each byte into a bit representation. All representations are in 8
	 * bits, except possibly the last one.
	 */
	public static String unpack(byte[] bytes) {

    // handle empty byte array
    if(bytes.length == 1) return "";

    StringBuilder bits = new StringBuilder();

    // unpack the first n-1 bytes
    for (int i = 0; i < bytes.length-2; i++)
    {
      bits.append(HelperFunctions.decimalToBinary(bytes[i] & 0xFF, 8));
    }

    // handle last byte
    bits.append(HelperFunctions.decimalToBinary(bytes[bytes.length-2] & 0xFF, bytes[bytes.length-1] & 0xFF));

    return bits.toString();
	}
}
