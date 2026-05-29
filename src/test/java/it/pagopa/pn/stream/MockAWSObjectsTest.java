package it.pagopa.pn.stream;

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.stream.middleware.queue.consumer.PnEventInboundService;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.SortEvent;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.StreamEvent;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class, ContextFunctionCatalogAutoConfiguration.class})
public abstract class MockAWSObjectsTest{

    @MockitoBean(name = "sortEventsActionProducer")
    private MomProducer<SortEvent> sortEventsActionProducer;

    @MockitoBean(name = "streamActionsEventProducer")
    private MomProducer<StreamEvent> webhookActionsEventProducer;

    @MockitoBean
    private PnEventInboundService pnEventInboundService;

    @MockitoBean
    private DynamoDbClient dynamoDbClient;
}
