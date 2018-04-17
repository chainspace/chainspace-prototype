package uk.ac.ucl.cs.sec.chainspace;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import static org.junit.Assert.fail;

public class TestPythonChecker {

    @Test
    public void test_python_checker_starts() throws Exception {

        PythonChecker checker = new PythonChecker("addition");

        try {
            checker.startChecker();
            fail("Should throw an exception because checker already running");
        } catch (StartCheckerException e) {
            System.out.println("\n" + e.getMessage() + "\n");
        }

    }

    @BeforeClass
    public static void set_checker_test_port() {
        System.setProperty("checker.start.port", "44444");
    }

    @After
    public void kill_any_checkers() throws Exception {
        System.out.println("\nKilling off any checkers that are still running...\n");
        File checkerPIds = new File("./checker.pids");
        System.out.println("Reading pids objectStatusFrom " + checkerPIds);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(checkerPIds));

            while (in.ready()) {
                String pid = in.readLine();
                Process p = Runtime.getRuntime().exec("kill " + pid);

                p.waitFor();
                if (p.exitValue() != 0) {
                    System.out.println("pid " + pid + " not killed");
                } else {
                    System.out.println("pid " + pid + " killed");
                }
            }
            checkerPIds.delete();
        } finally {
            if (in != null) {
                in.close();
            }
        }

    }


}
