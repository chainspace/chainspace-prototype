package uk.ac.ucl.cs.sec.chainspace;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.ac.ucl.cs.sec.chainspace.SQLiteConnector.initialiseDb;
import static uk.ac.ucl.cs.sec.chainspace.SQLiteConnector.openConnection;

public class TestTransactionQuery {

    public static final String OBJECT_ID_A = "a9bde7fac83d70a4c6811d74d3cb218abc6c0f69e0dc5a77f0097be61faf79c7";
    public static final String OBJECT_ID_B = "404d5c43cf0f34857b65405cb2cdcf015cbe792ee0327157f1cd66ea8b340411";
    public static final String OBJECT_ID_C = "cfe08a142af2899b9177adeb85e30b613cf0d36bf7e492a4146fa1faf71330bc";
    public static final String TX_ID_A = "f0f110bd77f6b3603c8386b02a866dbc10d26b2ae75c371fe268dc20ee3a57ff";
    public static final String TX_ID_B = "f7f147b2c2e6506e1e26b0b3186de9e859c7db60efcc3c80dfe9e312ae26cfcd";
    public static final String TX_ID_C = "9005f118a214269dddef6d56ee859449a551c687970d9a1da71f82587216ca9a";
    private Connection connection;


    public static class TransactionLogEntry {
        public final String timestamp;
        public final String transactionId;
        public final String transactionJson;

        public TransactionLogEntry(String timestamp, String transactionId, String transactionJson) {
            this.timestamp = timestamp;
            this.transactionId = transactionId;
            this.transactionJson = transactionJson;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Transaction: ")
                    .append(timestamp).append(" || ")
                    .append(transactionId).append(" || ")
                    .append(transactionJson);
            return sb.toString();
        }
    }

    @Test
    public void retrieve_sequence_of_txs_given_an_initial_object() throws SQLException {


        String sql = "SELECT * from logs";

        List<TransactionLogEntry> transactionLogEntries = new ArrayList<>();

        int rowCount;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            System.out.println("Executing SQL query " + sql);
            ResultSet rs = statement.executeQuery();

            final int COL_TIME_STAMP = 1;
            final int COL_TRANSACTION_ID = 2;
            final int COL_TRANSACTION_JSON = 3;

            int rowId = 0;
            while (rs.next()) {
                transactionLogEntries.add(
                        new TransactionLogEntry(
                                rs.getString(COL_TIME_STAMP),
                                rs.getString(COL_TRANSACTION_ID),
                                rs.getString(COL_TRANSACTION_JSON)));
                rowId++;
            }

            rowCount = rowId++;
            System.out.println("Retrieved " + rowCount + " Rows.");
        }

        for (TransactionLogEntry entry : transactionLogEntries) {
            System.out.println(entry.toString());
        }


        assertThat(rowCount, is(3));


    }

    @Before
    public void initialise_test_db() throws Exception {
        File databaseFile = new File("testdatabase.sqlite");
        if (databaseFile.exists()) {
            databaseFile.delete();
        }
        this.connection = openConnection("testdatabase");
        initialiseDb(connection);

        populateTestData(connection);
    }

    @After
    public void close_connection() throws Exception {
        connection.close();
    }

    private static void populateTestData(Connection conn) throws SQLException {
        insertObject(conn, OBJECT_ID_A, "0", 0);
        insertObject(conn, OBJECT_ID_B, "1", 0);
        insertObject(conn, OBJECT_ID_C, "2", 1);

        insertTransaction(conn, TX_ID_A, "addition", "init", null, "0");
        insertTransaction(conn, TX_ID_B, "addition", "increment", OBJECT_ID_A, "1");
        insertTransaction(conn, TX_ID_C, "addition", "increment", OBJECT_ID_B, "2");
    }

    private static void insertObject(Connection conn, String objectId, String value, int status) throws SQLException {

        String sql = "INSERT INTO data (object_id, object, status) VALUES (?, ?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, objectId);
            statement.setString(2, value);
            statement.setString(3, valueOf(status));
            statement.executeUpdate();
        }
    }

    private static void insertTransaction(Connection conn, String transactionId, String contractId,
                                          String contractMethod, String inputObjectId,
                                          String output) throws SQLException {

        String inputIds = (inputObjectId == null) ? "[]" : "[\"" + inputObjectId + "\"]";
        String transactionJson = "{\"contractID\":\"" + contractId + "\"," +
                "\"dependencies\":[],\"inputIDs\":" + inputIds + "," +
                "\"methodID\":\"" + contractMethod + "\"," +
                "\"outputs\":[\"" + output + "\"],\"parameters\":[],\"referenceInputIDs\":[],\"returns\":[]}";

        String sql = "INSERT INTO logs (transaction_id, transaction_json) VALUES (?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, transactionId);
            statement.setString(2, transactionJson);
            statement.executeUpdate();
        }

    }


}
