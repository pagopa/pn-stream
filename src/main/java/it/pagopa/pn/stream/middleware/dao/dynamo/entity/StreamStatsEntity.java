package it.pagopa.pn.stream.middleware.dao.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAtomicCounter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * Entity WebhookStats
 */
@DynamoDbBean
@Data
@NoArgsConstructor
public class StreamStatsEntity {

    public static final String COL_PK = "pk";
    public static final String COL_SK = "sk";
    public static final String COL_COUNTER = "counter";
    public static final String COL_TTL = "ttl";

    public StreamStatsEntity(String pk, String sk) {
        this.pk = pk;
        this.sk = sk;
    }

    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)}))
    private String pk;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))
    private String sk;

    @Setter
    @Getter(onMethod = @__({@DynamoDbAtomicCounter(startValue = 1), @DynamoDbAttribute(COL_COUNTER)}))
    private Long counter;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TTL)}))
    private long ttl;
}