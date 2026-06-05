package it.pagopa.pn.stream.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.stream.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.StreamScheduleEventHandler;
import it.pagopa.pn.stream.utils.MdcKey;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static it.pagopa.pn.stream.utils.MdcUtils.setMdc;

@Component
@AllArgsConstructor
@CustomLog
public class StreamDeliveryPushConsumer {

    private final StreamScheduleEventHandler streamScheduleEventHandler;

    @SqsListener("${pn.stream.topics.event}")
    public void consumeUnlockAllEvents(Message<SortEventAction> message) {
        final String processName = "UNLOCK ALL EVENTS ACTION";
        try {
            setMdc(message);
            MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.UNLOCK_ALL_EVENTS_KEY);
            log.logStartingProcess(processName);
            streamScheduleEventHandler.handleUnlockAllEvents(message.getPayload());
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage(), ex);
            HandleEventUtils.handleException(message.getHeaders(), ex);
            throw ex;
        } finally {
            MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);
        }
    }
}