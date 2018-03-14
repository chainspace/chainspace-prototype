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
import static uk.ac.ucl.cs.sec.chainspace.TestTransactionQuery.ChainspaceObject.ObjectStatus.objectStatusFrom;

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

    public static class ChainspaceObject {

        public enum ObjectStatus {
            ACTIVE(0), INACTIVE(1);

            public final int value;

            ObjectStatus(int value) {
                this.value = value;
            }

            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("status: ")
                        .append(this.name())
                        .append(" (").append(value).append(")");
                return sb.toString();
            }

            public static ObjectStatus objectStatusFrom(int value) {
                switch (value) {
                    case 0:
                        return ACTIVE;
                    case 1:
                        return INACTIVE;
                    default:
                        throw new RuntimeException("Unmapped object status of " + value);
                }
            }

        }


        public final String id;
        public final String value;
        public final ObjectStatus status;

        public ChainspaceObject(String id, String value, ObjectStatus status) {
            this.id = id;
            this.value = value;
            this.status = status;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("CS Object: ")
                    .append(id).append(" || ")
                    .append(value).append(" || ")
                    .append(status);

            return sb.toString();
        }
    }

    @Test
    public void retrieve_all_transaction_log_entries() throws SQLException {

        List<TransactionLogEntry> transactionLogEntries = retrieveTransactionLogEntries();

        for (TransactionLogEntry entry : transactionLogEntries) {
            System.out.println(entry.toString());
        }


        assertThat(transactionLogEntries.size(), is(3));


    }

    @Test
    public void retrieve_all_objects() throws SQLException {

        List<ChainspaceObject> chainspaceObjects = retrieveObjects();

        for (ChainspaceObject object : chainspaceObjects) {
            System.out.println(object.toString());
        }

        assertThat(chainspaceObjects.size(), is(3));

    }

    private List<ChainspaceObject> retrieveObjects() throws SQLException {
        String sql = "SELECT * from data";

        List<ChainspaceObject> chainspaceObjects = new ArrayList<>();


        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            System.out.println("Executing SQL query " + sql);
            ResultSet rs = statement.executeQuery();

            final int COL_ID = 1;
            final int COL_VALUE = 2;
            final int COL_STATUS = 3;

            while (rs.next()) {
                chainspaceObjects.add(
                        new ChainspaceObject(
                                rs.getString(COL_ID),
                                rs.getString(COL_VALUE),
                                objectStatusFrom(rs.getInt(COL_STATUS))));
            }


            System.out.println("Retrieved " + chainspaceObjects.size() + " Rows.");
        }
        return chainspaceObjects;
    }

    private List<TransactionLogEntry> retrieveTransactionLogEntries() throws SQLException {
        String sql = "SELECT * from logs";

        List<TransactionLogEntry> transactionLogEntries = new ArrayList<>();


        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            System.out.println("Executing SQL query " + sql);
            ResultSet rs = statement.executeQuery();

            final int COL_TIME_STAMP = 1;
            final int COL_TRANSACTION_ID = 2;
            final int COL_TRANSACTION_JSON = 3;

            while (rs.next()) {
                transactionLogEntries.add(
                        new TransactionLogEntry(
                                rs.getString(COL_TIME_STAMP),
                                rs.getString(COL_TRANSACTION_ID),
                                rs.getString(COL_TRANSACTION_JSON)));
            }


            System.out.println("Retrieved " + transactionLogEntries.size() + " Rows.");
        }
        return transactionLogEntries;
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
