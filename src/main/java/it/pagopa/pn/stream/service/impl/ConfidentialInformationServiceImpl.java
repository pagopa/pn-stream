package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import it.pagopa.pn.stream.service.ConfidentialInformationService;
import it.pagopa.pn.stream.service.mapper.ConfidentialTimelineElementDtoMapper;
import it.pagopa.pn.stream.generated.openapi.msclient.datavault.model.ConfidentialTimelineElementId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
public class ConfidentialInformationServiceImpl implements ConfidentialInformationService {
    private final PnDataVaultClientReactive pnDataVaultClientReactive;

    public ConfidentialInformationServiceImpl(PnDataVaultClientReactive pnDataVaultClientReactive) {
        this.pnDataVaultClientReactive = pnDataVaultClientReactive;
    }

    @Override
    public Flux<ConfidentialTimelineElementDtoInt> getTimelineConfidentialInformation(List<TimelineElementInternal> timelineElementInternal) {
        List<ConfidentialTimelineElementId> request = timelineElementInternal.stream().map(this::getConfidentialElementId).toList();
        return this.pnDataVaultClientReactive.getNotificationTimelines(request)
                .map( ConfidentialTimelineElementDtoMapper::externalToInternal);
    }

    @Override
    public Flux<ConfidentialTimelineElementDtoInt> getTimelineConfidentialInformationFromConfidentialElementIds(List<ConfidentialTimelineElementId> request) {
        return this.pnDataVaultClientReactive.getNotificationTimelines(request)
                .map( ConfidentialTimelineElementDtoMapper::externalToInternal);
    }

    private ConfidentialTimelineElementId getConfidentialElementId(TimelineElementInternal internal) {
        return ConfidentialTimelineElementId.builder()
                .iun(internal.getIun())
                .timelineElementId(internal.getTimelineElementId())
                .build();
    }
}
