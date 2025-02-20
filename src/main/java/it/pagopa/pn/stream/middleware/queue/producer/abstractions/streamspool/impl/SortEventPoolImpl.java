package it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventPool;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
@Slf4j
public class SortEventPoolImpl implements SortEventPool {

    private final MomProducer<SortEvent> sortActionsQueue;

    private final Clock clock;


    public SortEventPoolImpl(MomProducer<SortEvent> sortActionsQueue,
                             Clock clock ) {
        this.sortActionsQueue = sortActionsQueue;
        this.clock = clock;
    }


    @Override
    public void scheduleFutureAction(SortEventAction action, SortEventType sortEventType) {
        addSortEventAction(action, sortEventType);
    }

    private void addSortEventAction(SortEventAction action, SortEventType sortEventType) {
        sortActionsQueue.push( SortEvent.builder()
                .header( StandardEventHeader.builder()
                        .publisher("pn-stream")
                        .eventId(UUID.randomUUID().toString())
                        .createdAt( clock.instant() )
                        .eventType(sortEventType.name())
                        .iun( action.getEventKey().split("_")[0] )
                        .build()
                )
                .payload( action )
                .build(),
                action.getDelaySeconds()
        );
    }
}