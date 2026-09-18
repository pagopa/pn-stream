package it.pagopa.pn.stream.generated.openapi.server.stream.v1.dto;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.ProblemError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProblemErrorTest {

    private ProblemError problemError;

    @BeforeEach
    void setUp() {
        problemError = new ProblemError();
        problemError.setCode("code");
        problemError.setDetail("detail");
    }

    @Test
    void code() {
        ProblemError expected = ProblemError.builder()
                .code("code")
                .detail("detail")
                .build();
        Assertions.assertEquals(expected, problemError.code("code"));
    }

    @Test
    void getCode() {
        Assertions.assertEquals("code", problemError.getCode());
    }
    

    @Test
    void getDetail() {
        Assertions.assertEquals("detail", problemError.getDetail());
    }

    @Test
    void testEquals() {
        ProblemError expected = ProblemError.builder()
                .code("code")
                .detail("detail")
                .build();
        Assertions.assertEquals(Boolean.TRUE, expected.equals(problemError));
    }
    
}