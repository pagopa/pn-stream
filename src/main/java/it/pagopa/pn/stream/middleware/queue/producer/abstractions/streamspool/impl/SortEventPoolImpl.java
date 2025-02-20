package it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl;

import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.api.dto.events.MomProducer;
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
        // prevedere la gestione del delay passato nella action in fase di inserimento
        addSortEventAction(action, sortEventType);
    }

    private void addSortEventAction(SortEventAction action, SortEventType sortEventType) {
        sortActionsQueue.push( SortEvent.builder()
                .header( GenericEventHeader.builder()
                        .publisher("pn-stream")
                        .eventId(UUID.randomUUID().toString())
                        .createdAt( clock.instant() )
                        .eventType(sortEventType.name())
                        .build()
                )
                .payload( action )
                .build()
        );
    }
}
