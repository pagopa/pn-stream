package it.pagopa.pn.stream.generated.openapi.msclient.datavault.auth;

import it.pagopa.pn.stream.generated.openapi.msclient.datavault_reactive.auth.HttpBasicAuth;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpBasicAuthTest {

    private HttpBasicAuth auth;

    @BeforeEach
    void setUp() {
        auth = new HttpBasicAuth();
        auth.setPassword("password");
        auth.setUsername("user");
    }

    @Test
    void getUsername() {
        String user = auth.getUsername();
        Assertions.assertEquals("user", user);
    }

    @Test
    void getPassword() {
        String password = auth.getPassword();
        Assertions.assertEquals("password", password);
    }
}