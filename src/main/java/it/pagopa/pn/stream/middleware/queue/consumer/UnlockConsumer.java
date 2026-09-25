package it.pagopa.pn.stream.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.stream.exceptions.PnStreamException;
import it.pagopa.pn.stream.middleware.queue.consumer.utils.HandleEventUtils;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventType;
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
public class UnlockConsumer {

    private final StreamScheduleEventHandler streamScheduleEventHandler;

    @SqsListener(value = "${pn.stream.topics.event-schedule}")
    public void consumeUnlock(Message<SortEventAction> message) {
        String eventTypeStr = message.getHeaders().get("eventType", String.class);
        SortEventType eventType = eventTypeStr != null ? SortEventType.valueOf(eventTypeStr) : null;

        if(eventType == null) {
            throw new PnStreamException("Missing required eventType header", 500, "PN_STREAM_MISSING_EVENT_TYPE");
        }

        if(eventType == SortEventType.UNLOCK_ALL_EVENTS) {
            consumeUnlockAllEvents(message);
        } else if(eventType == SortEventType.UNLOCK_EVENTS) {
            consumeUnlockEvents(message);
        }
    }

    public void consumeUnlockEvents(Message<SortEventAction> message) {
        final String processName = "UNLOCK EVENTS ACTION";
        setMdc(message);
        log.debug("Handle action pnStreamUnlockEventsConsumer, with content {}", message);
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

    public void consumeUnlockAllEvents(Message<SortEventAction> message) {
        final String processName = "UNLOCK ALL EVENTS ACTION";
        setMdc(message);
        log.debug("Handle action pnStreamUnlockAllEventsConsumer, with content {}", message);
        try {
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