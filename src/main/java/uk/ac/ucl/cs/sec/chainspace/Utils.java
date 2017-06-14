package uk.ac.ucl.cs.sec.chainspace;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 *
 *
 */
class Utils {

    /**
     *
     * @param input the string to hash
     * @return the input's SHA-256 digest
     * @throws NoSuchAlgorithmException This exception should never happens since the algorithm is hardcoded.
     */
    static String hash(String input) throws NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(input.getBytes());
        byte[] hash = digest.digest();

        return String.format("%064x", new java.math.BigInteger(1, hash));
    }


    /*
    static boolean verifyHash(String object, String hashedValue) throws NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(object.getBytes());
        byte[] hash = digest.digest();
        String hexhash = String.format("%064x", new java.math.BigInteger(1, hash));

        return hexhash.equals(hashedValue);

    }
    */


}
