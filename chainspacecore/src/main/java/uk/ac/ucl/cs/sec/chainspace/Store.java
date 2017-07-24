package uk.ac.ucl.cs.sec.chainspace;

import org.json.JSONArray;


/**
 *
 * Simple key-value store.
 */
class Store {

    // instance variables
    private Pair[] array;


    /**
     * constructor
     *
     */
    private Store(Pair[] array)
    {

        this.array = array;
    }


    /**
     * getValueFromKey
     * Get a value in the store from its associated key.
     */
    String getValueFromKey(String key) {

        for (int i = 0; i < this.getArray().length ; i++) {
            if (this.getArray()[i].getKey().equals(key)) {
                return (String) this.getArray()[i].getValue();
            }
        }

        return null;
    }


    /*
        Getters
     */
    Pair[] getArray() {

        return array;
    }


    /**
     * fromJson
     * Convert a JSONArray representing the sore into a proper Store Java object.
     */
    static Store fromJson(JSONArray jsonArray) {

        Pair[] pairArray = new Pair[jsonArray.length()];

        for (int i = 0; i < jsonArray.length(); i++) {
            pairArray[i] = new Pair<>(
                jsonArray.getJSONObject(i).getString("key"),
                jsonArray.getJSONObject(i).get("value").toString()
            );
        }

        return new Store(pairArray);
    }




}
