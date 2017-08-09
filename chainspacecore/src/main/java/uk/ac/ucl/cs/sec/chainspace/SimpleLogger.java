package uk.ac.ucl.cs.sec.chainspace;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by mus on 05/08/17.
 */
public class SimpleLogger {
    private String file;
    private PrintWriter writer;

    public SimpleLogger() {
        this("simplelog");
    }

    public SimpleLogger(String file) {
        this.file = file;

        try {
            this.writer = new PrintWriter(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(String message) {
        this.writer.println(System.currentTimeMillis() + " " + message);
        this.writer.flush();
    }
}
