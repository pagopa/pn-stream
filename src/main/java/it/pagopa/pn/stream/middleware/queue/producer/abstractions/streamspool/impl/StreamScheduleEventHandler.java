package it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventAction;
import it.pagopa.pn.stream.service.StreamScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_CTX_REQUEST_ID;
import static it.pagopa.pn.stream.exceptions.PnStreamExceptionCodes.ERROR_CODE_STREAM_SCHEDULE_EVENTFAILED;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamScheduleEventHandler {

    private final StreamScheduleService scheduleService;

    public void handleUnlockEvents(SortEventAction evt ) {
        log.info( "Received UNLOCK_EVENTS event with eventId={}", evt.getEventKey());
        try {
            log.debug("[enter] unlockEvents evt={}", evt);
            MDC.put(MDC_PN_CTX_REQUEST_ID, evt.getEventKey());
            MDCUtils.addMDCToContextAndExecute(
                    scheduleService.unlockEvents(evt, true)
            ).block();

            log.debug("[exit] unlockEvents evt={}", evt);
        } catch (Exception e) {
            log.error("error handling event", e);
            throw new PnInternalException("Error handling unlock events event", ERROR_CODE_STREAM_SCHEDULE_EVENTFAILED, e);
        }
    }

    public void handleUnlockAllEvents(SortEventAction evt ) {
        log.info( "Received UNLOCK_ALL_EVENTS event with eventId={}", evt.getEventKey());
        try {
            log.debug("[enter] unlockAllEvents evt={}", evt);
            MDC.put(MDC_PN_CTX_REQUEST_ID, evt.getEventKey());
            MDCUtils.addMDCToContextAndExecute(
                    scheduleService.unlockAllEvents(evt)
            ).block();

            log.debug("[exit] unlockAllEvents evt={}", evt);
        } catch (Exception e) {
            log.error("error handling event", e);
            throw new PnInternalException("Error handling unlock all events event", ERROR_CODE_STREAM_SCHEDULE_EVENTFAILED, e);
        }
    }
}