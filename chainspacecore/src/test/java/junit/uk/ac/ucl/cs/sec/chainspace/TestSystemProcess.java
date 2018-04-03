package uk.ac.ucl.cs.sec.chainspace;

import org.junit.Test;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TestSystemProcess {

    @Test
    public void writes_proccess_id_to_file() {
        File idFile = new File("test.process.id");

        SystemProcess.writeProcessIdToFile(idFile.getAbsolutePath());

        assertThat(idFile.exists(), is(true));
    }
}
