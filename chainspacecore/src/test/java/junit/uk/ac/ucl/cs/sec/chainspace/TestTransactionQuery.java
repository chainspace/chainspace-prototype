package uk.ac.ucl.cs.sec.chainspace;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.ac.ucl.cs.sec.chainspace.SQLiteConnector.openConnection;

/**
 * For more background about the datastructures in here see https://arxiv.org/pdf/1708.03778.pdf
 */
public class TestTransactionQuery {

    private Connection connection;
    private TransactionQuery transactionQuery;

    @BeforeClass
    public static void initialise_test_db() throws Exception {
        TestDatabase.initialise();
    }

    @Before
    public void open_connection() throws Exception {
        this.connection = openConnection("testdatabase");
        this.transactionQuery = new TransactionQuery(connection);
    }

    @After
    public void close_connection() throws Exception {
        connection.close();
    }


    @Test
    public void retrieve_all_transaction_log_entries() throws SQLException {

        List<TransactionQuery.TransactionLogEntry> transactionLogEntries = transactionQuery.retrieveTransactionLogEntries();

        for (TransactionQuery.TransactionLogEntry entry : transactionLogEntries) {
            System.out.println(entry.toString());
        }


        assertThat(transactionLogEntries.size(), is(3));


    }

    @Test
    public void retrieve_all_objects() throws SQLException {

        List<TransactionQuery.ChainspaceObject> chainspaceObjects = transactionQuery.retrieveObjects();

        for (TransactionQuery.ChainspaceObject object : chainspaceObjects) {
            System.out.println(object.toString());
        }

        assertThat(chainspaceObjects.size(), is(3));

    }

    @Test
    public void retrieve_all_digests() throws SQLException {

        List<TransactionQuery.TransactionDigest> transactionDigests = transactionQuery.retrieveDigests();

        for (TransactionQuery.TransactionDigest object : transactionDigests) {
            System.out.println(object.toString());
        }

        assertThat(transactionDigests.size(), is(3));

    }


}
