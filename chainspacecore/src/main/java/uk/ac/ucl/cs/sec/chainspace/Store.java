package uk.ac.ucl.cs.sec.chainspace;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;


/**
 *
 * Simple key-value store.
 */
class Store extends HashMap<String, String>{


    /**
     * fromJson
     * Generates a Store object from its JSONObject representation.
     * @param json JSONObject
     * @return Store
     */
    static Store fromJson(JSONObject json) {
        Gson gson = new Gson();
        return gson.fromJson(json.toString(), Store.class);
    }





    /*

    // instance variables
    private Pair[] array;



    private Store(Pair[] array)
    {

        this.array = array;
    }



    String getValueFromKey(String key) {

        for (int i = 0; i < this.getArray().length ; i++) {
            if (this.getArray()[i].getKey().equals(key)) {
                return (String) this.getArray()[i].getValue();
            }
        }

        return null;
    }



    Pair[] getArray() {

        return array;
    }


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

    */




}
