package it.pagopa.pn.stream.middleware.dao.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
@Data
@NoArgsConstructor
public class EventsQuarantineEntity {

    private static final String COL_PK = "pk";
    private static final String COL_EVENTID = "eventId";
    private static final String COL_EVENT = "event";
    public static final String STREAMID_INDEX = "streamId-index";
    private static final String COL_STREAMID = "streamId";

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)})) private String pk;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_EVENTID)}))  private String eventId;
    @Getter(onMethod=@__({@DynamoDbSecondaryPartitionKey(indexNames = STREAMID_INDEX), @DynamoDbAttribute(COL_STREAMID)}))  private String streamId;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_EVENT)})) private String event;

    public EventsQuarantineEntity(String streamId, String iun, String eventId) {
        this.pk = streamId + "_" + iun;
        this.eventId = eventId;
    }
}
