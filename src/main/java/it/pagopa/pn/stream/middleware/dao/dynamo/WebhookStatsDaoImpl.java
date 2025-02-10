package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.stats.WebhookStatsDto;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.WebhookStatsEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.mapper.DtoToEntityWebhookStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

@Slf4j
@Repository
public class WebhookStatsDaoImpl implements WebhookStatsDao {
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient;
    private final DynamoDbAsyncTable<WebhookStatsEntity> table;

    public WebhookStatsDaoImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnStreamConfigs cfg) {
        this.table = dynamoDbEnhancedClient.table(cfg.getDao().getWebhookStatsTable(), TableSchema.fromBean(WebhookStatsEntity.class));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    }

    public Mono<Void> putItemIfAbsent(WebhookStatsDto webhookStats) {
        PutItemEnhancedRequest<WebhookStatsEntity> putItemEnhancedRequest = PutItemEnhancedRequest.builder(WebhookStatsEntity.class)
                .item(DtoToEntityWebhookStats.toEntity(webhookStats))
                .conditionExpression(
                        Expression.builder()
                                .expression("attribute_not_exists(pk)")
                                .build()
                )
                .build();

        return Mono.fromFuture(table.putItem(putItemEnhancedRequest))
                .onErrorResume(e -> {
                    log.error("Error putting item if absent", e);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<WebhookStatsEntity> getItem(WebhookStatsEntity webhookStats) {
        log.debug("getItem {}", webhookStats);
        GetItemEnhancedRequest getItemEnhancedRequest = GetItemEnhancedRequest.builder()
                .key(k -> k.partitionValue(webhookStats.getPk()).sortValue(webhookStats.getSk()))
                .build();
        return Mono.fromFuture(table.getItem(getItemEnhancedRequest))
                .onErrorResume(e -> {
                    log.error("Error getting item", e);
                    return Mono.empty();
                });
    }
}
