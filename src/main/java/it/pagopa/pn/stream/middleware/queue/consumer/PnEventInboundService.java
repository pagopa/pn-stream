package it.pagopa.pn.stream.middleware.queue.consumer;

import it.pagopa.pn.commons.utils.MDCUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Configuration
@Slf4j
public class PnEventInboundService {
    private final EventHandler eventHandler;

    public PnEventInboundService(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }
    @Bean
    public MessageRoutingCallback customRouter() {
        return new MessageRoutingCallback() {
            @Override
            public String routingResult(Message<?> message) {
                setMdc(message);
                return handleMessage(message);
            }
        };
    }

    private void setMdc(Message<?> message) {
        MessageHeaders messageHeaders = message.getHeaders();
        MDCUtils.clearMDCKeys();
        
        if (messageHeaders.containsKey("aws_messageId")){
            String awsMessageId = messageHeaders.get("aws_messageId", String.class);
            MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, awsMessageId);
        }
        
        if (messageHeaders.containsKey("X-Amzn-Trace-Id")){
            String traceId = messageHeaders.get("X-Amzn-Trace-Id", String.class);
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
        } else {
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, String.valueOf(UUID.randomUUID()));
        }

        String iun = (String) message.getHeaders().get("iun");
        if(iun != null){
            MDC.put(MDCUtils.MDC_PN_IUN_KEY, iun);
        }
    }

    private String handleMessage(Message<?> message) {
        String eventType = (String) message.getHeaders().get("eventType");
        log.debug("Received message from customRouter with eventType={}", eventType);
        String handlerName = eventHandler.getHandler().get(eventType);
        if (!StringUtils.hasText(handlerName)) {
            log.error("undefined handler for eventType={}", eventType);
        }
        log.debug("Handler for eventType={} is {}", eventType, handlerName);
        return handlerName;
    }
}
