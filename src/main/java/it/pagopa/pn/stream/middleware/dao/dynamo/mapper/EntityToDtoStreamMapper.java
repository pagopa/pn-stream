package it.pagopa.pn.stream.middleware.dao.dynamo.mapper;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamMetadataResponseV26;
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

    public static StreamMetadataResponseV26 entityToDto(StreamEntity entity ) {
        StreamMetadataResponseV26 streamMetadataResponse = new StreamMetadataResponseV26();
        streamMetadataResponse.setStreamId(UUID.fromString(entity.getStreamId()));
        streamMetadataResponse.setActivationDate(entity.getActivationDate());
        streamMetadataResponse.setEventType(StreamMetadataResponseV26.EventTypeEnum.valueOf(entity.getEventType()));
        streamMetadataResponse.setTitle(entity.getTitle());
        streamMetadataResponse.setFilterValues(List.copyOf(Objects.requireNonNullElse(entity.getFilterValues(), new HashSet<>())));
        streamMetadataResponse.setGroups(entity.getGroups());
        streamMetadataResponse.setVersion(entity.getVersion() != null ? entity.getVersion() : firstVersion);
        streamMetadataResponse.setDisabledDate(entity.getDisabledDate());
        return streamMetadataResponse;
    }

}