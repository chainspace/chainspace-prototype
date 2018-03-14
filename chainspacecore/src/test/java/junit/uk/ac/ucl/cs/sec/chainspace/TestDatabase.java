package uk.ac.ucl.cs.sec.chainspace;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static java.lang.String.valueOf;
import static uk.ac.ucl.cs.sec.chainspace.SQLiteConnector.initialiseDbSchema;
import static uk.ac.ucl.cs.sec.chainspace.SQLiteConnector.openConnection;

/**
 * For more background about the datastructures in here see https://arxiv.org/pdf/1708.03778.pdf
 */
public class TestDatabase {
    private static final String OBJECT_ID_A = "a9bde7fac83d70a4c6811d74d3cb218abc6c0f69e0dc5a77f0097be61faf79c7";
    private static final String OBJECT_ID_B = "404d5c43cf0f34857b65405cb2cdcf015cbe792ee0327157f1cd66ea8b340411";
    private static final String OBJECT_ID_C = "cfe08a142af2899b9177adeb85e30b613cf0d36bf7e492a4146fa1faf71330bc";
    private static final String TX_ID_A = "f0f110bd77f6b3603c8386b02a866dbc10d26b2ae75c371fe268dc20ee3a57ff";
    private static final String TX_ID_B = "f7f147b2c2e6506e1e26b0b3186de9e859c7db60efcc3c80dfe9e312ae26cfcd";
    private static final String TX_ID_C = "9005f118a214269dddef6d56ee859449a551c687970d9a1da71f82587216ca9a";
    private static final String DIGEST_A = "f0f110bd77f6b3603c8386b02a866dbc10d26b2ae75c371fe268dc20ee3a57ff";
    private static final String DIGEST_B = "f936ebf57688424fdf70c4e77c86c86dc1224da200eb4ccb8d0c337a979deb73";
    private static final String DIGEST_C = "2c6bfef3f92f1f64dfb77dee1de30e31fb7f04a43855f5fd1f59320e48af4e60";

    public static void populateTestData(Connection conn) throws SQLException {
        insertObject(conn, OBJECT_ID_A, "0", 0);
        insertObject(conn, OBJECT_ID_B, "1", 0);
        insertObject(conn, OBJECT_ID_C, "2", 1);

        insertTransaction(conn, TX_ID_A, "addition", "init", null, "0");
        insertTransaction(conn, TX_ID_B, "addition", "increment", OBJECT_ID_A, "1");
        insertTransaction(conn, TX_ID_C, "addition", "increment", OBJECT_ID_B, "2");

        insertHeadDigest(conn, 1, DIGEST_A);
        insertHeadDigest(conn, 2, DIGEST_B);
        insertHeadDigest(conn, 3, DIGEST_C);


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

    /**
     * The digest is the beginning of the implementation of Node Hash Chains as described in the chainspace paper
     *
     */
    private static void insertHeadDigest(Connection conn, int id, String digest) throws SQLException {
        String sql = "INSERT INTO head (ID, digest) VALUES (?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, digest);
            statement.executeUpdate();
        }
    }

    public static void initialise() throws SQLException, ClassNotFoundException {
        File databaseFile = new File("testdatabase.sqlite");
        if (databaseFile.exists()) {
            databaseFile.delete();
        }

        try (Connection connection = openConnection("testdatabase")) {
            initialiseDbSchema(connection);
            populateTestData(connection);
        }
    }
}
