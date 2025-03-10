package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventsQuarantineEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Objects;

import static it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventsQuarantineEntity.STREAMID_INDEX;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

@Component
@Slf4j
public class EventsQuarantineEntityDaoImpl implements EventsQuarantineEntityDao {

    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient;
    private final DynamoDbAsyncTable<EventsQuarantineEntity> tableQuarantine;
    private final DynamoDbAsyncTable<EventEntity> tableEvents;

    public EventsQuarantineEntityDaoImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnStreamConfigs cfg) {
        this.tableQuarantine = dynamoDbEnhancedClient.table(cfg.getDao().getEventsQuarantineTableName(), TableSchema.fromBean(EventsQuarantineEntity.class));
        this.tableEvents = dynamoDbEnhancedClient.table(cfg.getDao().getEventsTableName(), TableSchema.fromBean(EventEntity.class));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    }

    @Override
    public Mono<Page<EventsQuarantineEntity>> findByPk(String pk, Map<String, AttributeValue> lastEvaluateKey, int limit) {
        Key key = Key.builder().partitionValue(pk).build();
        QueryConditional queryByHashKey = keyEqualTo(key);

        QueryEnhancedRequest.Builder queryEnhancedRequest = QueryEnhancedRequest
                .builder()
                .limit(limit)
                .queryConditional(queryByHashKey);

        if (!CollectionUtils.isEmpty(lastEvaluateKey)) {
            queryEnhancedRequest.exclusiveStartKey(lastEvaluateKey);
        }

        return Mono.from(tableQuarantine.query(queryEnhancedRequest.build()));
    }

    @Override
    public Mono<EventsQuarantineEntity> putItem(EventsQuarantineEntity entity) {
        return Mono.fromFuture(tableQuarantine.putItem(entity)).thenReturn(entity);
    }

    @Override
    public Mono<EventEntity> saveAndClearElement(EventsQuarantineEntity entity, EventEntity eventEntity) {

        if (Objects.isNull(entity) || Objects.isNull(eventEntity)) {
            return Mono.empty();
        }

        TransactWriteItemsEnhancedRequest.Builder transactWriteItemsEnhancedRequest = TransactWriteItemsEnhancedRequest.builder();
        transactWriteItemsEnhancedRequest.addPutItem(tableEvents, eventEntity);
        transactWriteItemsEnhancedRequest.addDeleteItem(tableQuarantine, entity);
        return Mono.fromFuture(dynamoDbEnhancedClient.transactWriteItems(transactWriteItemsEnhancedRequest.build()))
                .thenReturn(eventEntity);
    }

    @Override
    public Mono<Page<EventsQuarantineEntity>> findByStreamId(String streamId, Map<String, AttributeValue> lastEvaluateKey, int limit) {
        Key key = Key.builder().partitionValue(streamId).build();
        QueryConditional queryByHashKey = keyEqualTo(key);
        QueryEnhancedRequest.Builder queryEnhancedRequest = QueryEnhancedRequest
                .builder()
                .limit(limit)
                .queryConditional(queryByHashKey);

        if (!CollectionUtils.isEmpty(lastEvaluateKey)) {
            queryEnhancedRequest.exclusiveStartKey(lastEvaluateKey);
        }

        return Mono.from(tableQuarantine.index(STREAMID_INDEX).query(queryEnhancedRequest.build()));
    }
}