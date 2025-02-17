package it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl;

import it.pagopa.pn.api.dto.events.IEventType;

public enum SortEventActionType implements IEventType {
    SORT_ACTION_GENERIC( SortEvent.class );

    private final Class<?> eventClass;

    SortEventActionType(Class<?> eventClass) {
        this.eventClass = eventClass;
    }

    public Class<?> getEventJavaClass() {
        return eventClass;
    }

}
