package uk.ac.ucl.cs.sec.chainspace;

import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static uk.ac.ucl.cs.sec.chainspace.TransactionQuery.ChainspaceObject.ObjectStatus.objectStatusFrom;

public class TransactionQuery {

    private final Connection connection;

    public TransactionQuery(Connection connection) {
        this.connection = connection;
    }


    public  List<TransactionDigest> retrieveDigests() throws SQLException {
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

    public  List<ChainspaceObject> retrieveObjects() throws SQLException {
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

    public  List<TransactionLogEntry> retrieveTransactionLogEntries() throws SQLException {
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

    public static class TransactionLogEntry {
        public final String timestamp;
        public final String transactionId;
        public final String transactionJson;
        private final LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        public TransactionLogEntry(String timestamp, String transactionId, String transactionJson) {
            this.timestamp = timestamp;
            this.transactionId = transactionId;
            this.transactionJson = transactionJson;

            map.put("timestamp", timestamp);
            map.put("transactionId", transactionId);
            map.put("transactionJson", new JSONObject(transactionJson));
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Transaction: ")
                    .append(timestamp).append(" || ")
                    .append(transactionId).append(" || ")
                    .append(transactionJson);
            return sb.toString();
        }

        public Map<String, Object> asMap() {
            return map;
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
            INACTIVE(0), ACTIVE(1) ;

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
                        return INACTIVE;
                    case 1:
                        return ACTIVE;
                    default:
                        throw new RuntimeException("Unmapped object status of " + value);
                }
            }

        }



        public final String id;
        public final String value;
        public final ObjectStatus status;
        private final Map<String, Object> map = new LinkedHashMap<>();

        public ChainspaceObject(String id, String value, ObjectStatus status) {
            this.id = id;
            this.value = value;
            this.status = status;

            map.put("id", id);

            if (isJson(value)) {
                map.put("value", new JSONObject(value));
            } else {
                map.put("value", value);
            }
            map.put("status", status.name().toLowerCase());

        }

        private boolean isJson(String value) {
            return value.startsWith("{") && value.endsWith("}");
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("CS Object: ")
                    .append(id).append(" || ")
                    .append(value).append(" || ")
                    .append(status);

            return sb.toString();
        }

        public Map<String, Object> asMap() {
            return map;
        }

    }
}
