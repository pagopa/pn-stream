package it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl;

import static it.pagopa.pn.stream.exceptions.PnStreamExceptionCodes.ERROR_CODE_WEBHOOK_EVENTFAILED;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamEventType;
import it.pagopa.pn.stream.service.StreamEventsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamActionsEventHandler {

    private final StreamEventsService webhookService;

    public void handleEvent(StreamAction evt ) {
        log.info( "Received WEBHOOK-ACTION actionType={}", evt.getType());
        try {
            switch (evt.getType()) {
                case REGISTER_EVENT -> doHandleRegisterEvent(evt);
                case PURGE_STREAM_OLDER_THAN, PURGE_STREAM -> doHandlePurgeEvent(evt);
                default ->
                        throw new PnInternalException("Error handling webhook event", ERROR_CODE_WEBHOOK_EVENTFAILED);
            }
        } catch (Exception e) {
            log.error("error handling event", e);
            throw new PnInternalException("Error handling webhook event", ERROR_CODE_WEBHOOK_EVENTFAILED, e);
        }

    }

    private void doHandlePurgeEvent(StreamAction evt) {
        log.debug("[enter] doHandlePurgeEvent evt={}", evt);

        MDCUtils.addMDCToContextAndExecute(
            webhookService
                    .purgeEvents(evt.getStreamId(), evt.getEventId(), evt.getType() == StreamEventType.PURGE_STREAM_OLDER_THAN)
        ).block();
        
        log.debug("[exit] doHandlePurgeEvent evt={}", evt);
    }

    private void doHandleRegisterEvent(StreamAction evt) {
        log.debug("[enter] doHandleRegisterEvent evt={}", evt);

        MDCUtils.addMDCToContextAndExecute(
            webhookService
                    .saveEvent(evt.getTimelineElementInternal(), evt.getType())
        ).block();
        
        log.debug("[exit] doHandleRegisterEvent evt={}", evt);
    }

}
