package it.pagopa.pn.stream.middleware.dao.dynamo.entity;

import it.pagopa.pn.stream.dto.stats.TimeUnitEnum;
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
    public static final String COL_TIMEUNIT = "timeUnit";
    public static final String COL_VALUE = "value";
    public static final String COL_SPANUNIT = "spanUnit";
    public static final String COL_TTL = "ttl";

    public WebhookStatsEntity(String pk, String sk, TimeUnitEnum timeUnit, Number value, Number ttl,String spanUnit) {
        this.pk = pk;
        this.sk = sk;
        this.timeUnit = timeUnit;
        this.value = value;
        this.spanUnit = spanUnit;
        this.ttl = ttl;
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)})) private String pk;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)})) private String sk;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TIMEUNIT)})) private TimeUnitEnum timeUnit;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_SPANUNIT)})) private String spanUnit;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_VALUE)})) private Number value;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TTL)})) private Number ttl;
}