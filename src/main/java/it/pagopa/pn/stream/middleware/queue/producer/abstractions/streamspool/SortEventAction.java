package it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.*;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;

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

    public String getStreamId(){
        String[] splittedEventKey = eventKey.split("_");
        if(splittedEventKey.length == 2) {
            return splittedEventKey[0];
        }
        throw new PnInternalException(String.format("Error during retrieve streamId from eventKey: [%s]", eventKey), ERROR_CODE_PN_GENERIC_ERROR);
    }

    public String getIun(){
        String[] splittedEventKey = eventKey.split("_");
        if(splittedEventKey.length == 2) {
            return splittedEventKey[0];
        }
        throw new PnInternalException(String.format("Error during retrieve IUN from eventKey: [%s]", eventKey), ERROR_CODE_PN_GENERIC_ERROR);
    }
}
