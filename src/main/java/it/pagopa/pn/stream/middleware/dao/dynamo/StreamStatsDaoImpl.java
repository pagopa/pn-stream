package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamStatsEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Repository
public class StreamStatsDaoImpl implements StreamStatsDao {
    private final DynamoDbAsyncTable<StreamStatsEntity> table;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;

    public StreamStatsDaoImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnStreamConfigs cfg, DynamoDbAsyncClient dynamoDbAsyncClient) {
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.table = dynamoDbEnhancedClient.table(cfg.getDao().getStreamStatsTable(), TableSchema.fromBean(StreamStatsEntity.class));
    }


    @Override
    public Mono<StreamStatsEntity> getItem(Key key) {
        log.info("get key={}", key);
        return Mono.fromFuture(table.getItem(key))
                .doOnSuccess(item -> log.info("Retrieved item: {}", item))
                .doOnError(error -> log.error("Failed to retrieve item with key={}", key, error));
    }

    @Override
    public Mono<StreamStatsEntity> updateAtomicCounterStats(StreamStatsEntity entity) {
        log.info("update webhook stats entity={}", entity);
        log.info("update stream entity={}", entity);
        return Mono.fromFuture(table.updateItem(entity));
    }

    @Override
    public Mono<UpdateItemResponse> updateCustomCounterStats(String pk, String sk, String increment) {
        log.info("update custom counter stats for pk={}, sk={}, increment={}", pk, sk, increment);

        Map<String, AttributeValue> key = new HashMap<>();
        key.put(StreamStatsEntity.COL_PK, AttributeValue.builder().s(pk).build());
        key.put(StreamStatsEntity.COL_SK, AttributeValue.builder().s(sk).build());

        Map<String, AttributeValue> attributeValue = new HashMap<>();
        attributeValue.put(":v", AttributeValue.builder().n(increment).build());

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(table.tableName())
                .key(key)
                .updateExpression("ADD "+ StreamStatsEntity.COL_COUNTER + " :v")
                .expressionAttributeValues(attributeValue)
                .build();

        return Mono.fromFuture(dynamoDbAsyncClient.updateItem(updateRequest))
                .doOnSuccess(response -> log.info("Successfully updated custom counter stats for pk={}, sk={}", pk, sk))
                .doOnError(error -> log.error("Failed to update custom counter stats for pk={}, sk={}", pk, sk, error));
    }
}
