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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Repository
public class StreamStatsDaoImpl implements StreamStatsDao {
    private final DynamoDbAsyncTable<StreamStatsEntity> table;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;

    public StreamStatsDaoImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnStreamConfigs cfg, DynamoDbAsyncClient dynamoDbAsyncClient) {
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.table = dynamoDbEnhancedClient.table(cfg.getDao().getStreamStatsTableName(), TableSchema.fromBean(StreamStatsEntity.class));
    }


    @Override
    public Mono<StreamStatsEntity> getItem(Key key) {
        return Mono.fromFuture(table.getItem(key))
                .doOnNext(item -> log.info("Retrieved item: {}", item.getPk()))
                .doOnError(error -> log.error("Failed to retrieve item with key={}", key, error));
    }

    @Override
    public Mono<StreamStatsEntity> updateAtomicCounterStats(StreamStatsEntity entity) {
        return Mono.fromFuture(table.updateItem(entity))
                .doOnNext(response -> log.info("Successfully updated atomic counter stats for pk={}, sk={}", entity.getPk(), entity.getSk()))
                .doOnError(error -> log.warn("Failed to update atomic counter stats for pk={}, sk={}",entity.getPk(), entity.getSk(), error))
                .onErrorResume(error -> Mono.empty());
    }

    @Override
    public Mono<UpdateItemResponse> updateCustomCounterStats(String pk, String sk, Integer increment, Duration ttlDuration) {

        Long ttl = LocalDateTime.now().plus(ttlDuration).atZone(ZoneOffset.UTC).toEpochSecond();

        Map<String, AttributeValue> key = new HashMap<>();
        key.put(StreamStatsEntity.COL_PK, AttributeValue.builder().s(pk).build());
        key.put(StreamStatsEntity.COL_SK, AttributeValue.builder().s(sk).build());

        Map<String, AttributeValue> attributeValue = new HashMap<>();
        attributeValue.put(":counter", AttributeValue.builder().n(String.valueOf(increment)).build());
        attributeValue.put(":t", AttributeValue.builder().n(String.valueOf(ttl)).build());

        Map<String, String> attributeName = new HashMap<>();
        attributeName.put("#counter", StreamStatsEntity.COL_COUNTER);
        attributeName.put("#ttl", StreamStatsEntity.COL_TTL);

        String updateExpression = "ADD #counter :counter SET #ttl = :t";

        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                .tableName(table.tableName())
                .key(key)
                .updateExpression(updateExpression)
                .expressionAttributeNames(attributeName)
                .expressionAttributeValues(attributeValue)
                .build();

        return Mono.fromFuture(dynamoDbAsyncClient.updateItem(updateItemRequest))
                .doOnNext(response -> log.info("Successfully updated custom counter stats for pk={}, sk={}", pk, sk))
                .doOnError(error -> log.warn("Failed to update custom counter stats for pk={}, sk={}", pk, sk, error))
                .onErrorResume(error -> Mono.empty());
    }



}
