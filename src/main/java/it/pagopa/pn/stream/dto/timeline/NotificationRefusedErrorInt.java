package it.pagopa.pn.stream.dto.timeline;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationRefusedErrorInt {
    private String errorCode;
    private String detail;
}
