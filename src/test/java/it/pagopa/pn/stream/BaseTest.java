package it.pagopa.pn.stream;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.SortEvent;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.StreamEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;


@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class BaseTest {


    @Slf4j
    @SpringBootTest
    @ActiveProfiles("test")
    @Import(LocalStackTestConfig.class)
    public static class WithLocalStack {
        @MockitoBean(name = "sortEventsActionProducer")
        private MomProducer<SortEvent> sortEventsActionProducer;

        @MockitoBean(name = "streamActionsEventProducer")
        private MomProducer<StreamEvent> webhookActionsEventProducer;

        @MockitoBean
        private SqsAsyncClient sqsAsyncClient;
    }


}
