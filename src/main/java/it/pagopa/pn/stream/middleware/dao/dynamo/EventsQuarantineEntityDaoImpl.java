package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventsQuarantineEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;

import java.util.Objects;

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
    public Mono<Page<EventsQuarantineEntity>> findByPk(String pk) {
        log.info("findByPk pk={}", pk);
        Key key = Key.builder().partitionValue(pk).build();
        QueryConditional queryByHashKey = keyEqualTo(key);
        return Mono.from(tableQuarantine.query(queryByHashKey));
    }

    @Override
    public Mono<EventsQuarantineEntity> putItem(EventsQuarantineEntity entity) {
        log.info("putItem entity={}", entity);
        return Mono.fromFuture(tableQuarantine.putItem(entity).thenApply(r -> entity));
    }

    @Override
    public Mono<Void> saveAndClearElement(EventsQuarantineEntity entity, EventEntity eventEntity) {
        log.info("save and delete items entity={} eventEntity={}", entity, eventEntity);

        if (Objects.isNull(entity) || Objects.isNull(eventEntity)) {
            return Mono.empty();
        }

        TransactWriteItemsEnhancedRequest.Builder transactWriteItemsEnhancedRequest = TransactWriteItemsEnhancedRequest.builder();
        transactWriteItemsEnhancedRequest.addPutItem(tableEvents, eventEntity);
        transactWriteItemsEnhancedRequest.addDeleteItem(tableQuarantine, entity);
        return Mono.fromFuture(dynamoDbEnhancedClient.transactWriteItems(transactWriteItemsEnhancedRequest.build()));
    }
}