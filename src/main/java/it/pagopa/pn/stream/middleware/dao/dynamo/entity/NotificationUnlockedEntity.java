package it.pagopa.pn.stream.middleware.dao.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Data
@NoArgsConstructor
public class NotificationUnlockedEntity {

    private static final String COL_PK = "pk";
    private static final String COL_TTL = "ttl";

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)})) private String pk;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TTL)})) private long ttl;

}
