package it.pagopa.pn.stream.middleware.queue.producer.stream.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.SortEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsSortEventsProducer extends AbstractSqsMomProducer<SortEvent> {

    public SqsSortEventsProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper ) {
        super(sqsClient, topic, objectMapper, SortEvent.class );
    }
}