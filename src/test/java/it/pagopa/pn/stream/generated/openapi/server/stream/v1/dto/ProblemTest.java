package it.pagopa.pn.stream.generated.openapi.server.stream.v1.dto;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.Problem;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.ProblemError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class ProblemTest {

    private Problem problem;

    @BeforeEach
    void setUp() {
        problem = new Problem();
        problem.setDetail("001");
        problem.setStatus(2);
        problem.setTitle("003");
        problem.setType("004");
        problem.setTraceId("005");
        problem.setErrors(Collections.singletonList(new ProblemError().detail("001")));
    }

    @Test
    void type() {
        Problem expected = new Problem()
                .detail("001")
                .status(2)
                .title("003")
                .type("004")
                .traceId("005")
                .errors(Collections.singletonList(new ProblemError().detail("001")));
        Assertions.assertEquals(expected, problem.type("004"));
    }

    @Test
    void getType() {
        Assertions.assertEquals("004", problem.getType());
    }

    @Test
    void status() {
        Problem expected = new Problem()
                .detail("001")
                .status(2)
                .title("003")
                .type("004")
                .traceId("005")
                .errors(Collections.singletonList(new ProblemError().detail("001")));
        Assertions.assertEquals(expected, problem.status(2));
    }

    @Test
    void getStatus() {
        Assertions.assertEquals(2, problem.getStatus());
    }

    @Test
    void title() {
        Problem expected = new Problem()
                .detail("001")
                .status(2)
                .title("003")
                .type("004")
                .traceId("005")
                .errors(Collections.singletonList(new ProblemError().detail("001")));
        Assertions.assertEquals(expected, problem.title("003"));
    }

    @Test
    void getTitle() {
        Assertions.assertEquals("003", problem.getTitle());
    }

    @Test
    void detail() {
        Problem expected = new Problem()
                .detail("001")
                .status(2)
                .title("003")
                .type("004")
                .traceId("005")
                .errors(Collections.singletonList(new ProblemError().detail("001")));
        Assertions.assertEquals(expected, problem.detail("001"));
    }

    @Test
    void getDetail() {
        Assertions.assertEquals("001", problem.getDetail());
    }

    @Test
    void traceId() {
        Problem expected = new Problem()
                .detail("001")
                .status(2)
                .title("003")
                .type("004")
                .traceId("005")
                .errors(Collections.singletonList(new ProblemError().detail("001")));
        Assertions.assertEquals(expected, problem.traceId("005"));
    }

    @Test
    void getTraceId() {
        Assertions.assertEquals("005", problem.getTraceId());
    }

    @Test
    void errors() {
        Problem expected = new Problem()
                .detail("001")
                .status(2)
                .title("003")
                .type("004")
                .traceId("005")
                .errors(Collections.singletonList(new ProblemError().detail("001")));
        Assertions.assertEquals(expected, problem.errors(Collections.singletonList(new ProblemError().detail("001"))));
    }

    @Test
    void getErrors() {
        Assertions.assertEquals(Collections.singletonList(new ProblemError().detail("001")), problem.getErrors());
    }

    @Test
    void testEquals() {
        Problem expected = new Problem()
                .detail("001")
                .status(2)
                .title("003")
                .type("004")
                .traceId("005")
                .errors(Collections.singletonList(new ProblemError().detail("001")));
        Assertions.assertEquals(Boolean.TRUE, expected.equals(problem));
    }
}