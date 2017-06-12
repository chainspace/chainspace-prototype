package uk.ac.ucl.cs.sec.chainspace;


import java.security.NoSuchAlgorithmException;
import java.sql.*;

class DatabaseConnector {

    // instance variables
    private Connection connection;

    /**
     * constructor
     * Initialise the database connection and create a table to store object (if it does not already exist).
     */
    DatabaseConnector(int nodeID) throws SQLException, ClassNotFoundException {

        //
        Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection("jdbc:sqlite:node" + nodeID + ".sqlite");
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS data (" +
                "object_id CHAR(32) NOT NULL UNIQUE," +
                "object TEXT NOT NULL," +
                "status INTEGER NOT NULL)";
        statement.executeUpdate(sql);
        statement.close();
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
        String sql = "SELECT * FROM data WHERE object_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, objectID);
        ResultSet resultSet = statement.executeQuery();

        // check if the object is in the database.
        if (resultSet.isBeforeFirst()) {
            return resultSet.getString("object");
        }
        // if it's not, ask other shards
        else {
            // TODO: if the current node does not hold the object, ask other nodes for it.
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
}
