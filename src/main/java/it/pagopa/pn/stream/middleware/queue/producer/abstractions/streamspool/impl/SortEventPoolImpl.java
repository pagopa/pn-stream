package it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventPool;
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
    public void scheduleFutureAction(SortEventAction action) {
        // prevedere la gestione del delay passato nella action in fase di inserimento
        addSortEventAction(action);
    }

    private void addSortEventAction(SortEventAction action ) {
        sortActionsQueue.push( SortEvent.builder()
                .header( StandardEventHeader.builder()
                        .publisher("stream")
                        .iun( action.getEventKey() )
                        .eventId(UUID.randomUUID().toString())
                        .createdAt( clock.instant() )
                        .eventType( SortEventActionType.SORT_ACTION_GENERIC.name())
                        .build()
                )
                .payload( action )
                .build()
        );
    }
}
