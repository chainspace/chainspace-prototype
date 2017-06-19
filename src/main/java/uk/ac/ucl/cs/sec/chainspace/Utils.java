package uk.ac.ucl.cs.sec.chainspace;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 *
 *
 */
class Utils {

    /**
     * hash
     * Compute the SHA-256 hash of the input string.
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


    /**
     * verifyHash
     * Verify a hash image against a SHA-256 digest.
     *
     * @param object the hash image
     * @param hashedValue the digest
     * @return whether the digest matches the hash image
     * @throws NoSuchAlgorithmException This exception should never happens since the algorithm is hardcoded.
     */
    static boolean verifyHash(String object, String hashedValue) throws NoSuchAlgorithmException {

        return hash(object).equals(hashedValue);

    }


}
