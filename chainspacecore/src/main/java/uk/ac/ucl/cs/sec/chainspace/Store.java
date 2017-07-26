package uk.ac.ucl.cs.sec.chainspace;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;


/**
 *
 * Simple key-value store.
 */
class Store extends HashMap<String, String> {

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
    
}
