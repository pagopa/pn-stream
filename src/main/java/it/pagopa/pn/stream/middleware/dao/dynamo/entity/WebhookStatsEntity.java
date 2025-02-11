package it.pagopa.pn.stream.middleware.dao.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
public class WebhookStatsEntity {

    public static final String COL_PK = "pk";
    public static final String COL_SK = "sk";
    public static final String COL_VALUE = "value";
    public static final String COL_TTL = "ttl";

    public WebhookStatsEntity(String pk, String sk) {
        this.pk = pk;
        this.sk = sk;
        this.value = 0L;
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)})) private String pk;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)})) private String sk;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_VALUE)})) private Long value;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TTL)})) private long ttl;
}