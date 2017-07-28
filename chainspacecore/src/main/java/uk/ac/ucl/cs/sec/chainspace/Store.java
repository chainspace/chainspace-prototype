package uk.ac.ucl.cs.sec.chainspace;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;


/**
 *
 * Simple key-value store.
 */
public class Store extends HashMap<String, String> implements Serializable {

    /**
     * fromJson
     * Generates a Store object from its JSONObject representation.
     * @param json JSONObject
     * @return Store
     */
    public static Store fromJson(JSONObject json) {
        Gson gson = new Gson();
        return gson.fromJson(json.toString(), Store.class);
    }
    
}
