package uk.ac.ucl.cs.sec.chainspace;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static uk.ac.ucl.cs.sec.chainspace.ClientService.HOST_IP_SYSTEM_PROPERTY_NAME;

public class TestClientService {

    private String systemPropertyHostname;

    @Before
    public void retrieveSystemPropertyHostname() {
        systemPropertyHostname = System.getProperty(HOST_IP_SYSTEM_PROPERTY_NAME);
        System.clearProperty(HOST_IP_SYSTEM_PROPERTY_NAME);
    }

    @After
    public void resetSystemPropertyHostname() {
        if (systemPropertyHostname != null) {
            System.setProperty(HOST_IP_SYSTEM_PROPERTY_NAME, systemPropertyHostname);
        }
    }


    @Test
    public void does_not_result_in_localhost_with_no_system_property() {
        String hostname = ClientService.discoverExternalIPAddrress();

        System.out.println("Host name is " + hostname);
        assertThat(hostname, is(not("localhost")));

    }

    @Test
    public void returns_address_for_localhost() {
        System.setProperty(HOST_IP_SYSTEM_PROPERTY_NAME, "127.6.7.8");
        String hostname = ClientService.discoverExternalIPAddrress();


        assertThat(hostname, is("127.6.7.8"));
    }

}
