package uk.ac.ucl.cs.sec.chainspace;

import org.json.JSONArray;


/**
 *
 *
 */
class Store {

    private Pair[] array;

    private Store(Pair[] array)
    {
        this.array = array;
    }

    Pair[] getArray() {
        return array;
    }

    String getValueFromKey(String key) {

        for (int i = 0; i < this.getArray().length ; i++) {
            if (this.getArray()[i].getKey().equals(key)) {
                return this.getArray()[i].getValue();
            }
        }

        return null;
    }




    static Store fromJson(JSONArray jsonArray) {

        Pair[] pairArray = new Pair[jsonArray.length()];

        for (int i = 0; i < jsonArray.length(); i++) {
            pairArray[i] = new Pair(
                jsonArray.getJSONObject(i).getString("key"),
                jsonArray.getJSONObject(i).getJSONObject("value").toString()
            );
        }

        return new Store(pairArray);
    }

    /*
    String toJson() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }
    */

    private static class Pair
    {
        String key, value;

        Pair(String key, String value)
        {
            this.key = key;
            this.value = value;
        }

        String getKey() {
            return key;
        }

        String getValue() {
            return value;
        }
    }
}
