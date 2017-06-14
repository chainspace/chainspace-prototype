package uk.ac.ucl.cs.sec.chainspace;

import java.sql.SQLException;



public class Main {

    public static void main(String[] args) {

        try {

            // run chainspace service
            new NodeService(1);
            new NodeService(2);

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

}
