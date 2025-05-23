package it.pagopa.pn.stream.middleware.dao.dynamo.mapper;

import it.pagopa.pn.stream.config.PnStreamConfigs;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamCreationRequestV28;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamRequestV28;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class DtoToEntityStreamMapper {

    private static String currentVersion;

    public DtoToEntityStreamMapper(PnStreamConfigs pnStreamConfigs){
        currentVersion = pnStreamConfigs.getCurrentVersion();
    }

    public static StreamEntity dtoToEntity(String paId, String streamId, String version, StreamCreationRequestV28 dto) {
        StreamEntity streamEntity = new StreamEntity(paId, streamId);
        streamEntity.setEventType(dto.getEventType().getValue());
        streamEntity.setTitle(dto.getTitle());
        streamEntity.setVersion(version != null ? version : currentVersion);
        streamEntity.setGroups(dto.getGroups());
        if (dto.getFilterValues() != null && !dto.getFilterValues().isEmpty())
            streamEntity.setFilterValues(Set.copyOf(dto.getFilterValues()));
        else
            streamEntity.setFilterValues(null);
        streamEntity.setSorting(dto.getWaitForAccepted());
        return streamEntity;
    }

    public static StreamEntity dtoToEntity(String paId, String streamId, String version, StreamRequestV28 dto) {
        StreamCreationRequestV28 creationRequestv23 = new StreamCreationRequestV28();
        BeanUtils.copyProperties(dto, creationRequestv23);
        creationRequestv23.setEventType(StreamCreationRequestV28.EventTypeEnum.fromValue(dto.getEventType().getValue()));
        return dtoToEntity(paId, streamId, version, creationRequestv23);
    }
}
