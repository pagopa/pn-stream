package it.pagopa.pn.stream.middleware.dao.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@DynamoDbBean
@Data
@NoArgsConstructor
public class StreamRetryAfter {

    private static final String COL_PK = "hashKey";
    private static final String COL_SK = "sortKey";
    private static final String COL_RETRY_AFTER = "retryAfter";
    private static final String COL_TTL = "ttl";
    public static final String RETRY_PREFIX = "RETRY#";

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)})) private String paId;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))  private String streamId;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_RETRY_AFTER)})) private Instant retryAfter;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TTL)})) private long ttl;

    public void setStreamId(String streamId) {
        if(!streamId.startsWith(RETRY_PREFIX)) {
            this.streamId = RETRY_PREFIX + streamId;
        }else {
            this.streamId = streamId;
        }
    }

}
