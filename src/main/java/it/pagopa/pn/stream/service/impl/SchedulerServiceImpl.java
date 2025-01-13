package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamEventType;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamsPool;
import it.pagopa.pn.stream.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class SchedulerServiceImpl implements SchedulerService {
    private final StreamsPool streamsPool;

    @Override
    public void scheduleWebhookEvent(String streamId, String eventId, Integer delay, StreamEventType actionType) {
        StreamAction action = StreamAction.builder()
                .streamId(streamId)
                .eventId(eventId)
                .iun("nd")
                .delay(delay)
                .type(actionType)
                .build();

        this.streamsPool.scheduleFutureAction(action);
    }
}
