package uk.ac.ucl.cs.sec.chainspace;

/**
 *
 *
 */
class Pair<V, W> {

    private V value1;
    private W value2;

    Pair(V value1, W value2) {
        this.value1 = value1;
        this.value2 = value2;
    }


    /*
        Getters
     */

    V getValue1() {
        return value1;
    }

    W getValue2() {
        return value2;
    }

}
