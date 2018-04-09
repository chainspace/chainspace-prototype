package uk.ac.ucl.cs.sec.chainspace.bft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;
import uk.ac.ucl.cs.sec.chainspace.CSTransaction;

public class TestTransaction {


    @Test
    public void parse_cs_tx_arr_in_parameters() {
        String arr_in_arr = "{\"parameters\":[\"[0,1]\", \"foo\", [\"A\", \"b\"]]}";

        Gson gson = new GsonBuilder().create();
        gson.fromJson(arr_in_arr, CSTransaction.class);

    }
}

