package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.WebhookStatsEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Repository
public class WebhookStatsDaoImpl implements WebhookStatsDao {
    private final DynamoDbAsyncTable<WebhookStatsEntity> table;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;

    public WebhookStatsDaoImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnStreamConfigs cfg, DynamoDbAsyncClient dynamoDbAsyncClient) {
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.table = dynamoDbEnhancedClient.table(cfg.getDao().getWebhookStatsTable(), TableSchema.fromBean(WebhookStatsEntity.class));
    }


    @Override
    public Mono<WebhookStatsEntity> getItem(Key key) {
        log.info("get key={}", key);
        return Mono.fromFuture(table.getItem(key))
                .doOnSuccess(item -> log.info("Retrieved item: {}", item))
                .doOnError(error -> log.error("Failed to retrieve item with key={}", key, error));
    }

    @Override
    public Mono<WebhookStatsEntity> updateAtomicCounterStats(WebhookStatsEntity entity) {
        log.info("update webhook stats entity={}", entity);

        UpdateItemEnhancedRequest<WebhookStatsEntity> updateItemEnhancedRequest =
                UpdateItemEnhancedRequest.builder(WebhookStatsEntity.class)
                        .item(entity)
                        .ignoreNulls(true)
                        .build();
        log.info("update stream entity={}", entity);
        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest).thenApply(r -> entity));
    }

    @Override
    public Mono<WebhookStatsEntity> updateCustomCounterStats(String pk, String sk, String increment) {
        log.info("update custom counter stats for pk={}, sk={}, increment={}", pk, sk, increment);

        Map<String, AttributeValue> key = new HashMap<>();
        key.put(WebhookStatsEntity.COL_PK, AttributeValue.builder().s(pk).build());
        key.put(WebhookStatsEntity.COL_SK, AttributeValue.builder().s(sk).build());

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(table.tableName())
                .key(key)
                .updateExpression("ADD #val :v")
                //utilizzo e mappo #val come alias per l'attributo value nell'espressione di aggiornamento per evitare il conflitto
                .expressionAttributeNames(Map.of("#val", WebhookStatsEntity.COL_VALUE))
                .expressionAttributeValues(Map.of(":v", AttributeValue.builder().n(increment).build()))
                .build();

        return Mono.fromFuture(dynamoDbAsyncClient.updateItem(updateRequest))
                .doOnSuccess(response -> log.info("Successfully updated custom counter stats for pk={}, sk={}", pk, sk))
                .doOnError(error -> log.error("Failed to update custom counter stats for pk={}, sk={}", pk, sk, error))
                .then(Mono.just(new WebhookStatsEntity(pk, sk)));
    }
}
