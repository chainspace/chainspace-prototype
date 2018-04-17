package uk.ac.ucl.cs.sec.chainspace;


import java.security.NoSuchAlgorithmException;
import java.sql.*;

import static uk.ac.ucl.cs.sec.chainspace.Main.DEBUG_ALLOW_REPEAT;
import static uk.ac.ucl.cs.sec.chainspace.SQLiteConnector.Constraints.DISABLE_CONSTRAINTS;
import static uk.ac.ucl.cs.sec.chainspace.SQLiteConnector.Constraints.ENABLE_CONSTRAINTS;


/**
 *
 *
 */
class SQLiteConnector extends DatabaseConnector {

    private Connection connection;

    enum Constraints {
        ENABLE_CONSTRAINTS, DISABLE_CONSTRAINTS
    }

    /**
     * constructor
     * Initialise the database connection and create a table to store object (if it does not already exist).
     */
    SQLiteConnector() throws SQLException, ClassNotFoundException {
        this.connection = openConnection(Main.DATABASE_NAME);
        initialiseDbSchema(this.connection);

    }

    static Connection openConnection(String databaseName) throws ClassNotFoundException, SQLException {

        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:" + databaseName + ".sqlite");

    }

    /**
     * This method used to do this
     *  // don't wait for data to reach disk
     * sql = "PRAGMA synchronous=OFF";
     * statement.execute(sql);
     * But I think that is a performance nehancement and for now want more integrity
     */
    static void initialiseDbSchema(Connection connection) throws ClassNotFoundException, SQLException {

        Constraints constraints = (DEBUG_ALLOW_REPEAT) ? DISABLE_CONSTRAINTS : ENABLE_CONSTRAINTS;

        createTable_data(connection, constraints);

        createTable_logs(connection, constraints);

        createTable_head(connection, constraints);


    }

    private static void createTable_data(Connection connection, Constraints constraints) throws SQLException {
        String constraint = (ENABLE_CONSTRAINTS.equals(constraints)) ? " UNIQUE" : "";

        String sql = "CREATE TABLE IF NOT EXISTS data (" +
                "object_id CHAR(32) NOT NULL" + constraint + "," +
                "object TEXT NOT NULL," +
                "status INTEGER NOT NULL)";

        executeUpdateSql(connection, sql);
    }

    private static void createTable_logs(Connection connection, Constraints constraints) throws SQLException {
        String constraint = (ENABLE_CONSTRAINTS.equals(constraints)) ? " UNIQUE" : "";

        String sql = "CREATE TABLE IF NOT EXISTS logs (" +
                "time_stamp TIMESTAMP DEFAULT (STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW'))," +
                "transaction_id CHAR(32) NOT NULL " + constraint + "," +
                "transaction_json TEXT NOT NULL)";
        executeUpdateSql(connection, sql);

    }

    private static void createTable_head(Connection connection, Constraints constraints) throws SQLException {
        String constraint = (ENABLE_CONSTRAINTS.equals(constraints)) ? " UNIQUE" : "";

        String sql = "CREATE TABLE IF NOT EXISTS head (" +
                "ID INTEGER PRIMARY KEY," +
                "digest CHAR(32) NOT NULL " + constraint + ")";

        executeUpdateSql(connection, sql);

    }


    private static void executeUpdateSql(Connection connection, String sql) throws SQLException {

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }

    }


    /**
     * close
     * Gently shut down the database connection
     */
    public void close() throws SQLException {
        this.connection.close();
    }


    /**
     * registerObject
     * Blindly insert an object into the database if it does not already exist.
     */
    // TODO: optimise requests (only one executeUpdate)
    public void saveObject(String transactionID, String[] objects) throws AbortTransactionException {

        String sql = "INSERT INTO data (object_id, object, status) VALUES (?, ?, 1)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < objects.length; i++) {
                String objectID = Utils.generateObjectID(transactionID, objects[i], i);
                statement.setString(1, objectID);
                statement.setString(2, objects[i]);
                statement.executeUpdate();

                if (Main.VERBOSE) {
                    System.out.println("\nNew object has been created:");
                    System.out.println("\tID: " + objectID);
                    System.out.println("\tObject: " + objects[i]);
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            throw new AbortTransactionException(e.getMessage());
        }

    }


    /**
     * isObjectInactive
     * Return true iff the object is in the database and is inactive.
     */
    // TODO: optimise requests (only one executeQuery)
    public boolean isInactive(String[] objectIDs) throws AbortTransactionException {

        // check all objects
        String sql = "SELECT status FROM data WHERE object_id = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String objectID : objectIDs) {

                // verbose print
                if (Main.VERBOSE) {
                    System.out.println("\nChecking if object is active:");
                    System.out.println("\tID: " + objectID);
                }

                // execute query
                statement.setString(1, objectID);
                ResultSet resultSet = statement.executeQuery();

                // check if the object is in the database and if it is active.
                // if the object is unknown, skip
                if (resultSet.isBeforeFirst() && resultSet.getInt("status") == 0) {
                    if (Main.VERBOSE) {
                        System.out.print("\tStatus: ");
                        System.err.println("FAILED");
                    }
                    return true;
                }
                if (Main.VERBOSE) {
                    System.out.println("\tStatus: OK");
                }
            }
        } catch (SQLException e) {
            throw new AbortTransactionException(e.getMessage());
        }

        // if none is inactive, return false
        return false;

    }


    /**
     * setObjectInactive
     * Make object inactive (the object is now consumed).
     */
    // TODO: optimise requests (only one executeUpdate)
    public void setInactive(String[] objectIDs) throws AbortTransactionException {

        // check all objects

        String sql = "UPDATE data SET status = 0 WHERE object_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String objectID : objectIDs) {
                statement.setString(1, objectID);
                statement.executeUpdate();

                if (Main.VERBOSE) {
                    System.out.println("\nObject set inactive:");
                    System.out.println("\tID: " + objectID);
                }

            }
        } catch (SQLException e) {
            throw new AbortTransactionException(e.getMessage());
        }

    }


    /**
     * logTransaction
     * Add transaction to the logs.
     */
    // TODO: optimise requests (only one executeUpdate)
    public void logTransaction(String transactionID, String transactionJson)
            throws NoSuchAlgorithmException, SQLException
    {

        // add transaction to the logs
        String sql = "INSERT INTO logs (transaction_id, transaction_json) VALUES (?, ?)";
        PreparedStatement statement;
        statement = connection.prepareStatement(sql);
        statement.setString(1, transactionID);
        statement.setString(2, transactionJson);
        statement.executeUpdate();
        statement.close();

        // verbose print
        if ( Main.VERBOSE) {
            System.out.println("\nTransaction added to logs:");
            System.out.println("\tID: "+transactionID);
            System.out.println("\tTransaction: "+transactionJson);
        }

        // update head
        updateHead(transactionJson);

    }

    /**
     * updateHead
     * Update the logs' head.
     */
    private void updateHead(String transactionJson) throws SQLException, NoSuchAlgorithmException {

        // verbose print
        if ( Main.VERBOSE) { System.out.println("\nUpdating log's head:"); }

        // get the previous head
        String sql = "SELECT digest FROM head ORDER BY ID DESC LIMIT 1";
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();

        // insert new head
        sql = "INSERT INTO head (ID, digest) VALUES (null, ?)";
        statement = connection.prepareStatement(sql);

        // check if there is at least one head in the table
        String newHead;
        if (resultSet.isBeforeFirst()) {
            if ( Main.VERBOSE) { System.out.println("\tOld head: "+resultSet.getString("digest"));}
            newHead = Utils.generateHead(resultSet.getString("digest"), transactionJson);
        }
        // if not, simply had a hash of the transaction
        else {
            if ( Main.VERBOSE) { System.out.println("\tOld head: NONE");}
            newHead = Utils.generateHead(transactionJson);
        }
        statement.setString(1, newHead);
        statement.executeUpdate();
        statement.close();
        if ( Main.VERBOSE) { System.out.println("\tNew head: "+newHead); }

    }
}
