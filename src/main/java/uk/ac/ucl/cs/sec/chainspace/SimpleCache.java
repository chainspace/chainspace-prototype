package uk.ac.ucl.cs.sec.chainspace;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 *
 */
// TODO: use something more efficient that an ArrayList (tab, tree, linkedList)
class SimpleCache extends ArrayList<String> implements Cache {

    // instance variables
    private int cacheDepth;

    SimpleCache(int cacheDepth) {

        this.cacheDepth = cacheDepth;

    }

    /**
     * isInCache
     * Verify whether the transaction has recenlty been processed. This could happens due to multiple broadcasts between
     * nodes and shards.
     */
    public boolean isInCache(String input) {

        // compute digest
        String digest;
        try {
            digest = Utils.hash(input);
        } catch (NoSuchAlgorithmException e) {
            digest = input;
        }

        // if the transaction is already in the cash, return true
        // NOTE: method toArray() as complexity O(n), making the binarySearch O(log n) useless here
        if( Arrays.binarySearch(this.toArray(), digest) >= 0 ) {return true;}

        // otherwise update cache and return false
        updateCache(digest);
        return false;

    }

    /**
     * updateCache
     * Update the values in the cache (suppress the oldest entry and add the new one)
     */
    private void updateCache(String digest) {

        if (this.size() < this.cacheDepth) {
            this.add(digest);
        }
        else {
            this.remove(0);
            this.add(digest);
        }

    }
}
