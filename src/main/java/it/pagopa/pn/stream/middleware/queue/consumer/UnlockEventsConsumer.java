package it.pagopa.pn.stream.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.stream.middleware.queue.consumer.utils.HandleEventUtils;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.StreamScheduleEventHandler;
import it.pagopa.pn.stream.utils.MdcKey;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import static it.pagopa.pn.stream.utils.MdcUtils.setMdc;

@Configuration
@CustomLog
@RequiredArgsConstructor
public class UnlockEventsConsumer {

    private final StreamScheduleEventHandler streamScheduleEventHandler;

    @SqsListener(value = "${pn.stream.topics.event-schedule}")
    public void consumeUnlockEvents(Message<SortEventAction> message) {
        final String processName = "UNLOCK EVENTS ACTION";
        setMdc(message);
        try {
            MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.UNLOCK_EVENTS_KEY);
            log.logStartingProcess(processName);
            streamScheduleEventHandler.handleUnlockEvents(message.getPayload());
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