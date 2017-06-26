package uk.ac.ucl.cs.sec.chainspace;


import java.security.NoSuchAlgorithmException;
import java.sql.*;


/**
 *
 *
 */
class SQLiteConnector extends DatabaseConnector {

    // instance variables
    private Connection connection;

    /**
     * constructor
     * Initialise the database connection and create a table to store object (if it does not already exist).
     */
    SQLiteConnector(int nodeID) throws SQLException, ClassNotFoundException {

        // create database connection
        Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection("jdbc:sqlite:node" + nodeID + ".sqlite");
        Statement statement = connection.createStatement();

        if (! Main.DEBUG_ALLOW_REPEAT) {
            // create table to store objects
            String sql = "CREATE TABLE IF NOT EXISTS data (" +
                    "object_id CHAR(32) NOT NULL UNIQUE," +
                    "object TEXT NOT NULL," +
                    "status INTEGER NOT NULL)";
            statement.executeUpdate(sql);

            // create table to store logs
            sql = "CREATE TABLE IF NOT EXISTS logs (" +
                    "transaction_id CHAR(32) NOT NULL UNIQUE," +
                    "transactionJson TEXT NOT NULL)";
            statement.executeUpdate(sql);

            // create table to store logs head
            sql = "CREATE TABLE IF NOT EXISTS head (" +
                    "ID INTEGER PRIMARY KEY," +
                    "digest CHAR(32) NOT NULL UNIQUE)";
            statement.executeUpdate(sql);
        }
        else {
            // removed all unique constraints for debug mode
            // create table to store objects
            String sql = "CREATE TABLE IF NOT EXISTS data (" +
                    "object_id CHAR(32) NOT NULL," +
                    "object TEXT NOT NULL," +
                    "status INTEGER NOT NULL)";
            statement.executeUpdate(sql);

            // create table to store logs
            sql = "CREATE TABLE IF NOT EXISTS logs (" +
                    "transaction_id CHAR(32) NOT NULL," +
                    "transactionJson TEXT NOT NULL)";
            statement.executeUpdate(sql);

            // create table to store logs head
            sql = "CREATE TABLE IF NOT EXISTS head (" +
                    "ID INTEGER PRIMARY KEY," +
                    "digest CHAR(32) NOT NULL)";
            statement.executeUpdate(sql);
        }

        // close statement
        statement.close();
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
     * Debug method that blindly insert an object into the database if it does not already exist.
     */
    // TODO: optimise requests (only one executeUpdate)
    public void saveObject(String transactionID, String[] objects) throws AbortTransactionException {

        String sql = "INSERT INTO data (object_id, object, status) VALUES (?, ?, 1)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String object : objects) {
                String objectID = this.generateObjectID(transactionID, object);
                statement.setString(1, objectID);
                statement.setString(2, object);
                statement.executeUpdate();

                if (Main.VERBOSE) {
                    System.out.println("\nNew object has been created:");
                    System.out.println("\tID: " + objectID);
                    System.out.println("\tObject: " + object);
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
                        System.err.println("FAILLED");
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
        String sql = "INSERT INTO logs (transaction_id, transactionJson) VALUES (?, ?)";
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
            newHead = this.generateHead(resultSet.getString("digest"), transactionJson);
        }
        // if not, simply had a hash of the transaction
        else {
            if ( Main.VERBOSE) { System.out.println("\tOld head: NONE");}
            newHead = this.generateHead(transactionJson);
        }
        statement.setString(1, newHead);
        statement.executeUpdate();
        statement.close();
        if ( Main.VERBOSE) { System.out.println("\tNew head: "+newHead); }

    }
}
