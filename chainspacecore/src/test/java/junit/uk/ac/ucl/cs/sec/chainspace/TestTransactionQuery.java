package uk.ac.ucl.cs.sec.chainspace;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
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
import static uk.ac.ucl.cs.sec.chainspace.SQLiteConnector.initialiseDbSchema;
import static uk.ac.ucl.cs.sec.chainspace.SQLiteConnector.openConnection;
import static uk.ac.ucl.cs.sec.chainspace.TestTransactionQuery.ChainspaceObject.ObjectStatus.objectStatusFrom;

/**
 * For more background about the datastructures in here see https://arxiv.org/pdf/1708.03778.pdf
 */
public class TestTransactionQuery {

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

    public static class TransactionDigest {
        public final int id;
        public final String digest;

        public TransactionDigest(int id, String digest) {
            this.id = id;
            this.digest = digest;
        }


        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("TransactionDigest: ")
                    .append(id).append(" || ")
                    .append(digest);
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

    @Test
    public void retrieve_all_digests() throws SQLException {

        List<TransactionDigest> transactionDigests= retrieveDigests();

        for (TransactionDigest object : transactionDigests) {
            System.out.println(object.toString());
        }

        assertThat(transactionDigests.size(), is(3));

    }

    private List<TransactionDigest> retrieveDigests() throws SQLException {
        String sql = "SELECT * from head";

        List<TransactionDigest> transactionDigests = new ArrayList<>();


        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            System.out.println("Executing SQL query " + sql);
            ResultSet rs = statement.executeQuery();

            final int COL_ID = 1;
            final int COL_DIGEST = 2;

            while (rs.next()) {
                transactionDigests.add(
                        new TransactionDigest(
                                rs.getInt(COL_ID),
                                rs.getString(COL_DIGEST)));
            }


            System.out.println("Retrieved " + transactionDigests.size() + " Rows.");
        }
        return transactionDigests;
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

    @BeforeClass
    public static void initialise_test_db() throws Exception {
        File databaseFile = new File("testdatabase.sqlite");
        if (databaseFile.exists()) {
            databaseFile.delete();
        }

        try (Connection connection = openConnection("testdatabase")) {
            initialiseDbSchema(connection);
            TestDatabase.populateTestData(connection);
        }
    }


    @Before
    public void open_connection() throws Exception {
        this.connection = openConnection("testdatabase");
    }

    @After
    public void close_connection() throws Exception {
        connection.close();
    }



}
