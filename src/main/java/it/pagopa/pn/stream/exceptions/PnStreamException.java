package it.pagopa.pn.stream.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import lombok.Getter;

@Getter
public class PnStreamException extends PnRuntimeException {

    private final String code;

    public PnStreamException(String message, int status, String code){
        super(message, message, status, code, null, null);
        this.code = code;
    }

    public PnStreamException(String message, int status, String code, String detail){
        super(message, message, status, code, null, detail);
        this.code = code;
    }

}
