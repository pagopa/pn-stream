package it.pagopa.pn.stream.middleware.dao.dynamo;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.StringUtils;

@Getter
@ToString
@Builder
public class EventsQuarantinePageable implements Pageable {
    private Integer limit;
    private String lastEvaluatedKey;
    private String lastEvaluatedEventId;

    @Override
    public boolean isPage() {
        return StringUtils.hasText(lastEvaluatedKey);
    }

    @Override
    public boolean hasLimit() {
        return limit != null && limit > 0;
    }
}
