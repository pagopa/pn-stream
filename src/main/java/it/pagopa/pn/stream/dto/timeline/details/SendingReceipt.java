package it.pagopa.pn.stream.dto.timeline.details;

import lombok.*;

@NoArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class SendingReceipt {
    private String id;
    private String system;
}
