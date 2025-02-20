package it.pagopa.pn.stream.middleware.dao.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Data
@NoArgsConstructor
public class EventsQuarantineEntity {

    private static final String COL_PK = "pk";
    private static final String COL_EVENTID = "eventId";
    private static final String COL_EVENT = "event";

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)})) private String pk;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_EVENTID)}))  private String eventId;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_EVENT)})) private String event;

    public EventsQuarantineEntity(String streamId, String iun, String eventId) {
        this.pk = streamId + "_" + iun;
        this.eventId = eventId;
    }
}
