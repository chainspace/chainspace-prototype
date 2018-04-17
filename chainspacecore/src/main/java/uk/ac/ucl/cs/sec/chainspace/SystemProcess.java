package uk.ac.ucl.cs.sec.chainspace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;

import static java.lang.String.format;

public class SystemProcess {


    public SystemProcess() {
    }

    public static void writeProcessIdToFile(String filename) {


        File processIdFile = initialiseProcessIdFile(filename);

        try (FileOutputStream out = new FileOutputStream(processIdFile)) {

            String processName = ManagementFactory.getRuntimeMXBean().getName();
            if (!processName.contains("@")) {
                throw new RuntimeException(format("Process name [%s] is not parsable for the processId!", processName));
            }

            String[] parts = processName.split("@");
            String processId = parts[0];
            String hostName = parts[1];

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
            writer.println(processId);
            writer.flush();
            log(format("Writing PID file for process with PID [%s] on [%s] (file is at [%s])", processId, hostName, processIdFile.getAbsolutePath()));

        } catch (IOException e) {
            throw new RuntimeException(format("Could not create processid file [%s]", processIdFile.getAbsolutePath()));
        }

    }

    private static File initialiseProcessIdFile(String filename) {
        try {
            File processIdFile = new File(filename);
            if (processIdFile.exists()) {
                if (!processIdFile.delete()) {
                    throw new RuntimeException(format("Could not delete old processid file [%s]", processIdFile));
                }
            }

            processIdFile.deleteOnExit();

            if (!processIdFile.createNewFile()) {
                throw new RuntimeException(format("Could not create new file [%s]", processIdFile));
            }
            return processIdFile;
        } catch (IOException e) {
            throw new RuntimeException(format("Could not initialise process Id file [%s] - see cause", filename), e);
        }
    }

    private void closeQuietly(FileOutputStream out) {
        if (out == null) {

        }
    }

    private static void log(String message) {
        System.out.println(message);
    }
}