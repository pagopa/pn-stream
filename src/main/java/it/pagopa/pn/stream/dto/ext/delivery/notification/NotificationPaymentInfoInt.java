package it.pagopa.pn.stream.dto.ext.delivery.notification;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationPaymentInfoInt {
    private PagoPaInt pagoPA;
    private F24Int f24;
}
