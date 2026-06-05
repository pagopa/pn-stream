package it.pagopa.pn.stream.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.stream.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.StreamActionsEventHandler;
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
public class StreamActionConsumer {

    private final StreamActionsEventHandler streamActionsEventHandler;

    @SqsListener(value = "${pn.stream.topics.scheduled-actions}")
    public void consume(Message<StreamAction> message) {
        final String processName = "STREAM ACTION";
        setMdc(message);
        try {
            MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.STREAM_KEY);
            HandleEventUtils.addIunToMdc(
                    message.getPayload().getTimelineElementInternal() != null
                            ? message.getPayload().getTimelineElementInternal().getIun()
                            : message.getPayload().getIun()
            );
            log.logStartingProcess(processName);
            streamActionsEventHandler.handleEvent(message.getPayload());
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