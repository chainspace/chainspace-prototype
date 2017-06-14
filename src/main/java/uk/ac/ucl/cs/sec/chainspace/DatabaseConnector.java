package uk.ac.ucl.cs.sec.chainspace;


import java.security.NoSuchAlgorithmException;
import java.sql.*;


/**
 *
 *
 */
class DatabaseConnector {

    // instance variables
    private Connection connection;

    /**
     * constructor
     * Initialise the database connection and create a table to store object (if it does not already exist).
     */
    DatabaseConnector(int nodeID) throws SQLException, ClassNotFoundException {

        // create database connection
        Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection("jdbc:sqlite:node" + nodeID + ".sqlite");
        Statement statement = connection.createStatement();

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

        // close statement
        statement.close();
    }


    /**
     * close
     * Gently shut down the database connection
     */
    void close() throws SQLException {
        this.connection.close();
    }


    /**
     * registerObject
     * Debug method that blindly insert an object into the database if it does not already exist.
     */
    void saveObject(String object) throws NoSuchAlgorithmException {

        String sql = "INSERT INTO data (object_id, object, status) VALUES (?, ?, 1)";
        PreparedStatement statement;
        try {
            statement = connection.prepareStatement(sql);
            statement.setString(1, Utils.hash(object));
            statement.setString(2, object);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException ignored) {} // ignore: happens if object already exists

    }

    /**
     * getObject
     * retrieve an a given object from the database.
     */
    String getObject(String objectID) throws SQLException {
        // prepare query
        String sql = "SELECT object FROM data WHERE object_id = ? LIMIT 1";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, objectID);
        ResultSet resultSet = statement.executeQuery();

        // check if the object is in the database.
        if (resultSet.isBeforeFirst()) {
            return resultSet.getString("object");
        }
        // if it's not, ask other shards
        else {
            return null;
        }
    }

    /**
     * setObjectInactive
     * Make object inactive (the object is now consumed).
     */
    void setObjectInactive(String objectID) throws SQLException {
        String sql = "UPDATE data SET status = 0 WHERE object_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, objectID);
        statement.executeUpdate();
        statement.close();
    }

    /**
     * logTransaction
     * Add transaction to the logs.
     */
    // TODO: optimise requests (only one executeUpdate)
    void logTransaction(String transactionJson) throws NoSuchAlgorithmException, SQLException {

        // add transaction to the logs
        String sql = "INSERT INTO logs (transaction_id, transactionJson) VALUES (?, ?)";
        PreparedStatement statement;
        statement = connection.prepareStatement(sql);
        statement.setString(1, Utils.hash(transactionJson));
        statement.setString(2, transactionJson);
        statement.executeUpdate();
        statement.close();

        // update head
        updateHead(transactionJson);

    }

    /**
     * updateHead
     * Update the logs' head.
     */
    private void updateHead(String transactionJson) throws SQLException, NoSuchAlgorithmException {

        // get the previous head
        String sql = "SELECT digest FROM head ORDER BY ID DESC LIMIT 1";
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();
        statement.close();

        // insert new head
        sql = "INSERT INTO head (ID, digest) VALUES (null, ?)";
        statement = connection.prepareStatement(sql);

        // check if there is at least one head in the table
        if (resultSet.isBeforeFirst()) {
            String newHead = Utils.hash( resultSet.getString("digest") + "|" + transactionJson);
            statement.setString(1, newHead);
        }
        // if not, simply had a hash of the transaction
        else {
            statement.setString(1, Utils.hash(transactionJson));
        }
        statement.executeUpdate();
        statement.close();

    }
}
