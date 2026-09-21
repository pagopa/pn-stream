package it.pagopa.pn.stream;

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.SortEvent;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.StreamEvent;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class})
public abstract class MockAWSObjectsTest{

    @MockitoBean(name = "sortEventsActionProducer")
    private MomProducer<SortEvent> sortEventsActionProducer;

    @MockitoBean(name = "streamActionsEventProducer")
    private MomProducer<StreamEvent> webhookActionsEventProducer;

    @MockitoBean
    private DynamoDbClient dynamoDbClient;

    @MockitoBean
    private SqsAsyncClient sqsAsyncClient;
}
