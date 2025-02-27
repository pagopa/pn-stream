package it.pagopa.pn.stream.middleware;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.middleware.queue.producer.stream.sqs.SqsSortEventsProducer;
import it.pagopa.pn.stream.middleware.queue.producer.stream.sqs.SqsStreamProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class PnStreamMiddlewareConfigs {

    private final PnStreamConfigs cfg;

    public PnStreamMiddlewareConfigs(PnStreamConfigs cfg) {
        this.cfg = cfg;
    }

    @Bean
    public SqsStreamProducer streamActionsEventProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsStreamProducer( sqs, cfg.getTopics().getScheduledActions(), objMapper);
    }

    @Bean
    public SqsSortEventsProducer sortEventsActionProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsSortEventsProducer( sqs, cfg.getTopics().getEventSchedule(), objMapper);
    }
}

