package it.pagopa.pn.stream.middleware.queue.consumer.handler;


import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.stream.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.StreamActionsEventHandler;
import it.pagopa.pn.stream.utils.MdcKey;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;


@Configuration
@AllArgsConstructor
@CustomLog
public class ActionHandler {
    private final StreamActionsEventHandler streamActionsEventHandler;

    @Bean
    public Consumer<Message<StreamAction>> pnStreamActionConsumer() {
        final String processName = "STREAM ACTION";

        return message -> {
            try {
                MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.STREAM_KEY);

                log.debug("Handle action pnStreamActionConsumer, with content {}", message);log.debug("pnStreamActionConsumer, message={}", message);
                StreamAction action = message.getPayload();
                HandleEventUtils.addIunToMdc(action.getTimelineElementInternal().getIun());

                log.logStartingProcess(processName);
                streamActionsEventHandler.handleEvent(action);
                log.logEndingProcess(processName);

                MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
