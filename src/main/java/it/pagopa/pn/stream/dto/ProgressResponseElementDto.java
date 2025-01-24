package it.pagopa.pn.stream.dto;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.ProgressResponseElementV26;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProgressResponseElementDto {
    private List<ProgressResponseElementV26> progressResponseElementList;
    private int retryAfter;
}
