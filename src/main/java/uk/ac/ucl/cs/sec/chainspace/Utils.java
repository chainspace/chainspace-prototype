package uk.ac.ucl.cs.sec.chainspace;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Utils {

    static String hash(String input) throws NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(input.getBytes());
        byte[] hash = digest.digest();

        return String.format("%064x", new java.math.BigInteger(1, hash));
    }


    public static boolean verifyHash(String object, String hashedValue) throws NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(object.getBytes());
        byte[] hash = digest.digest();
        String hexhash = String.format("%064x", new java.math.BigInteger(1, hash));

        return hexhash.equals(hashedValue);

    }
}
