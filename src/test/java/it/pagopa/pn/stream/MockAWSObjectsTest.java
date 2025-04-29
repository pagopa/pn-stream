package it.pagopa.pn.stream;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.stream.middleware.queue.consumer.PnEventInboundService;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.SortEvent;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.StreamEvent;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class, ContextFunctionCatalogAutoConfiguration.class})
public abstract class MockAWSObjectsTest{

    @MockBean(name = "sortEventsActionProducer")
    private MomProducer<SortEvent> sortEventsActionProducer;

    @MockBean(name = "streamActionsEventProducer")
    private MomProducer<StreamEvent> webhookActionsEventProducer;

    @MockBean
    private AmazonSQSAsync amazonSQS;

    @MockBean
    private PnEventInboundService pnEventInboundService;

    @MockBean
    private DynamoDbClient dynamoDbClient;
}
