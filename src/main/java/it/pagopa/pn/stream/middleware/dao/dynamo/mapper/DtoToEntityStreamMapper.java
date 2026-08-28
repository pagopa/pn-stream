package it.pagopa.pn.stream.middleware.dao.dynamo.mapper;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.CommunicationType;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamCreationRequestV30;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamRequestV30;
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

    public static StreamEntity dtoToEntity(String paId, String streamId, String version, StreamCreationRequestV30 dto) {
        StreamEntity streamEntity = new StreamEntity(paId, streamId);
        streamEntity.setEventType(dto.getEventType().getValue());
        streamEntity.setCommunicationType(dto.getCommunicationType() != null ? CommunicationType.valueOf(dto.getCommunicationType().getValue()) : null);
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

    public static StreamEntity dtoToEntity(String paId, String streamId, String version, StreamRequestV30 dto) {
        StreamCreationRequestV30 creationRequestv30 = new StreamCreationRequestV30();
        BeanUtils.copyProperties(dto, creationRequestv30);
        creationRequestv30.setEventType(StreamCreationRequestV30.EventTypeEnum.fromValue(dto.getEventType().getValue()));
        if (dto.getCommunicationType() != null) {
            creationRequestv30.setCommunicationType(StreamCreationRequestV30.CommunicationTypeEnum.fromValue(dto.getCommunicationType().getValue()));
        }
        return dtoToEntity(paId, streamId, version, creationRequestv30);
    }
}
