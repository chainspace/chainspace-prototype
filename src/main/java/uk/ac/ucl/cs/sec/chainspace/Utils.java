package uk.ac.ucl.cs.sec.chainspace;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Utils
 *
 * Some general purpose utilities.
 */
class Utils {


    /**
     * hash
     * Compute the SHA-256 hash of the input string.
     *
     * @param input the string to hash
     * @return the input's SHA-256 digest
     * @throws NoSuchAlgorithmException This exception should never happens since the algorithm is hardcoded
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
     * @throws NoSuchAlgorithmException This exception should never happens since the algorithm is hardcoded
     */
    static boolean verifyHash(String object, String hashedValue) throws NoSuchAlgorithmException {

        return hash(object).equals(hashedValue);

    }


    /**
     * makePostRequest
     * Make a simple post request
     *
     * @param url to url where to make the request
     * @param postData the post data representing a json string
     * @return the string response of the server
     * @throws IOException general IO exception, thrown if anything goes bad
     */
    static String makePostRequest(String url, String postData) throws IOException {

        // prepare post request
        HttpClient httpClient = HttpClientBuilder.create().build();
        StringEntity postingString = new StringEntity(postData);
        HttpPost post = new HttpPost(url);
        post.setEntity(postingString);
        post.setHeader("Content-type", "application/json");

        // execute
        HttpResponse response   = httpClient.execute(post);

        // return string response
        return new BasicResponseHandler().handleResponse(response);

    }


    /**
     * printHeader
     * Nicely display a header to the console.
     * @param title the title to print to the console
     */
    static void printHeader(String title) {
        System.out.println("\n----------------------------------------------------------------------------------");
        System.out.println("\t" + title);
        System.out.println("----------------------------------------------------------------------------------");
    }

    /**
     * printStacktrace
     * Nicely print the exception's stack trace to the console.
     * @param e the exception from with to print the stack trace
     */
    static void printStacktrace(Exception e) {
        System.out.println();
        e.printStackTrace();
        System.out.println();
    }

    /**
     * printLine
     * Draw a simple line to the console.
     */
    static void printLine() {
        System.out.println("\n----------------------------------------------------------------------------------");
        System.out.println("\n");
    }

}
