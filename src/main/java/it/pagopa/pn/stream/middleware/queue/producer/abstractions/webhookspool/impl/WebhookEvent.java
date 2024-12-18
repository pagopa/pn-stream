package it.pagopa.pn.stream.middleware.queue.producer.abstractions.webhookspool.impl;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.webhookspool.WebhookAction;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class WebhookEvent implements GenericEvent<StandardEventHeader, WebhookAction> {

    private StandardEventHeader header;

    private WebhookAction payload;
}
