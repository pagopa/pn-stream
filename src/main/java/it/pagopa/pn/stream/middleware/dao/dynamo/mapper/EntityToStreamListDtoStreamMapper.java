package it.pagopa.pn.stream.middleware.dao.dynamo.mapper;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamListElement;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EntityToStreamListDtoStreamMapper {
    
    private EntityToStreamListDtoStreamMapper(){
        
    }

    public static StreamListElement entityToDto(StreamEntity entity ) {
        StreamListElement streamListElement = new StreamListElement();
        streamListElement.setStreamId(UUID.fromString(entity.getStreamId()));
        streamListElement.setTitle(entity.getTitle());
        return streamListElement;
    }

}