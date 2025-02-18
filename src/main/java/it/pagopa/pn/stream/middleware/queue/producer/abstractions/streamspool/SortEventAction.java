package it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class SortEventAction {
    private String eventKey;
    private Integer delaySeconds;
    private Integer writtenCounter;
}
