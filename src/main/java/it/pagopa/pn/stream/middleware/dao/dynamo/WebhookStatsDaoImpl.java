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

@Slf4j
@Repository
public class WebhookStatsDaoImpl implements WebhookStatsDao {
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient;
    private final DynamoDbAsyncTable<WebhookStatsEntity> table;

    public WebhookStatsDaoImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnStreamConfigs cfg) {
        this.table = dynamoDbEnhancedClient.table(cfg.getDao().getWebhookStatsTable(), TableSchema.fromBean(WebhookStatsEntity.class));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    }


    @Override
    public Mono<WebhookStatsEntity> getItem(String pk) {
        log.info("get pk={}", pk);
        Key key = Key.builder().partitionValue(pk).build();
        return Mono.fromFuture(table.getItem(key));
    }

    @Override
    public Mono<WebhookStatsEntity> updateItem(WebhookStatsEntity entity) {

        UpdateItemEnhancedRequest<WebhookStatsEntity> updateItemEnhancedRequest =
                UpdateItemEnhancedRequest.builder(WebhookStatsEntity.class)
                        .item(entity)
                        .ignoreNulls(true)
                        .build();

        log.info("update webhook stats entity={}", entity);
        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest).thenApply(r -> entity));
    }
}
