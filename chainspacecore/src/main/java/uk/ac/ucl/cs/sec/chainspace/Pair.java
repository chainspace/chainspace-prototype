package uk.ac.ucl.cs.sec.chainspace;

/**
 *
 *
 */
public class Pair<K, V> {

    private K key;
    private V value;

    Pair(K key, V value) {
        this.key   = key;
        this.value = value;
    }

    K getKey() {
        return key;
    }

    V getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }
}
