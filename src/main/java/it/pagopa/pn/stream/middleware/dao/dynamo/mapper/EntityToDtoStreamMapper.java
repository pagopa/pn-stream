package it.pagopa.pn.stream.middleware.dao.dynamo.mapper;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamMetadataResponseV28;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class EntityToDtoStreamMapper {
    private static String firstVersion;
    public EntityToDtoStreamMapper(PnStreamConfigs pnStreamConfigs){
        firstVersion = pnStreamConfigs.getFirstVersion();
    }

    public static StreamMetadataResponseV28 entityToDto(StreamEntity entity ) {
        StreamMetadataResponseV28 streamMetadataResponse = new StreamMetadataResponseV28();
        streamMetadataResponse.setStreamId(UUID.fromString(entity.getStreamId()));
        streamMetadataResponse.setActivationDate(entity.getActivationDate());
        streamMetadataResponse.setEventType(StreamMetadataResponseV28.EventTypeEnum.valueOf(entity.getEventType()));
        streamMetadataResponse.setTitle(entity.getTitle());
        streamMetadataResponse.setFilterValues(List.copyOf(Objects.requireNonNullElse(entity.getFilterValues(), new HashSet<>())));
        streamMetadataResponse.setGroups(entity.getGroups());
        streamMetadataResponse.setVersion(entity.getVersion() != null ? entity.getVersion() : firstVersion);
        streamMetadataResponse.setDisabledDate(entity.getDisabledDate());
        streamMetadataResponse.setWaitForAccepted(entity.getSorting());
        return streamMetadataResponse;
    }

}