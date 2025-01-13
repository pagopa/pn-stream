package it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool;

import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class StreamAction {
    private String streamId;

    private String eventId;

    private String iun;

    private Integer delay;

    private TimelineElementInternal timelineElementInternal;

    private StreamEventType type;
}
