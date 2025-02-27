package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.exceptions.PnNotFoundException;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamRetryAfter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.pn.stream.exceptions.PnStreamExceptionCodes.ERROR_CODE_STREAM_STREAMNOTFOUND;
import static it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamRetryAfter.RETRY_PREFIX;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

@Component
@Slf4j
public class StreamEntityDaoImpl implements StreamEntityDao {

    private final DynamoDbAsyncTable<StreamEntity> table;
    private final DynamoDbAsyncTable<StreamRetryAfter> tableRetry;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient;
    private final PnStreamConfigs pnStreamConfigs;

    public StreamEntityDaoImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, DynamoDbAsyncClient dynamoDbAsyncClient, PnStreamConfigs cfg) {
        this.table = dynamoDbEnhancedClient.table(cfg.getDao().getStreamsTableName(), TableSchema.fromBean(StreamEntity.class));
        this.tableRetry = dynamoDbEnhancedClient.table(cfg.getDao().getStreamsTableName(), TableSchema.fromBean(StreamRetryAfter.class));
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.pnStreamConfigs = cfg;
    }

    @Override
    public Flux<StreamEntity> findByPa(String paId) {
        log.debug("findByPa paId={}", paId);
        Key hashKey = Key.builder().partitionValue(paId).build();
        QueryConditional queryByHashKey = keyEqualTo(hashKey);
        return Flux.from(table.query(queryByHashKey)
                .flatMapIterable(Page::items))
                .filter(entity -> !entity.getStreamId().startsWith(RETRY_PREFIX));
    }

    @Override
    public Mono<StreamEntity> get(String paId, String streamId) {
        log.info("get paId={} streamId={}", paId, streamId);
        Key hashKey = Key.builder().partitionValue(paId).sortValue(streamId).build();
        return Mono.fromFuture(table.getItem(hashKey));
    }

    @Override
    public Mono<Tuple2<StreamEntity, Optional<StreamRetryAfter>>> getWithRetryAfter(String paId, String streamId) {
        log.info("getWithRetryAfter paId={} streamId={}", paId, streamId);
        Key hashKey = Key.builder().partitionValue(paId).sortValue(streamId).build();
        Key retryHashKey = Key.builder().partitionValue(paId).sortValue(StreamRetryAfter.RETRY_PREFIX + streamId).build();

        ReadBatch streamEntityBatch = ReadBatch.builder(StreamEntity.class)
                .mappedTableResource(table)
                .addGetItem(hashKey)
                .build();
        ReadBatch streamRetryEntityBatch = ReadBatch.builder(StreamRetryAfter.class)
                .mappedTableResource(tableRetry)
                .addGetItem(retryHashKey)
                .build();

        return Mono.from(dynamoDbEnhancedClient.batchGetItem(BatchGetItemEnhancedRequest.builder()
                        .readBatches(streamEntityBatch, streamRetryEntityBatch)
                        .build())
                .map(batchGetResultPage ->
                        Tuples.of(
                                batchGetResultPage.resultsForTable(table).stream()
                                        .filter(entity -> !entity.getStreamId().startsWith(StreamRetryAfter.RETRY_PREFIX))
                                        .findFirst()
                                        .orElseThrow(() -> new PnNotFoundException("Not found"
                                                , String.format("Stream %s non found for Pa %s", streamId, paId)
                                                , ERROR_CODE_STREAM_STREAMNOTFOUND)),
                                batchGetResultPage.resultsForTable(tableRetry).stream()
                                        .filter(entity -> entity.getStreamId().startsWith(StreamRetryAfter.RETRY_PREFIX) && Objects.nonNull(entity.getRetryAfter()))
                                        .findFirst())));
    }

    @Override
    public Mono<Void> delete(String paId, String streamId) {
        log.info("delete paId={} streamId={}", paId, streamId);
        Key hashKey = Key.builder().partitionValue(paId).sortValue(streamId).build();
        return Mono.fromFuture(table.deleteItem(hashKey))
                .flatMap(entity -> Mono.fromFuture(table.deleteItem(Key.builder().partitionValue(paId).sortValue(RETRY_PREFIX + streamId).build())))
                .then();
    }

    @Override
    public Mono<StreamEntity> save(StreamEntity entity) {
        log.info("save entity={}", entity);
        return Mono.fromFuture(table.putItem(entity).thenApply(r -> entity));
    }

    @Override
    public Mono<StreamEntity> update(StreamEntity entity) {

        UpdateItemEnhancedRequest<StreamEntity> updateItemEnhancedRequest =
                UpdateItemEnhancedRequest.builder(StreamEntity.class)
                        .item(entity)
                        .ignoreNulls(true)
                        .build();

        log.info("update stream entity={}", entity);
        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest).thenApply(r -> entity));
    }

    @Override
    public Mono<Long> updateAndGetAtomicCounter(StreamEntity streamEntity) {
        log.info("updateAndGetAtomicCounter paId={} streamId={} counter={}", streamEntity.getPaId(), streamEntity.getStreamId(), streamEntity.getEventAtomicCounter());
        // il metodo utilizza le primitive base di dynamodbclient per poter eseguire l'update
        // atomico tramite l'action "ADD" e facendosi ritornare il nuovo valore
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(StreamEntity.COL_PK, AttributeValue.builder().s(streamEntity.getPaId()).build());
        key.put(StreamEntity.COL_SK, AttributeValue.builder().s(streamEntity.getStreamId()).build());

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(table.tableName())
                .key(key)
                .returnValues(ReturnValue.UPDATED_NEW)
                .updateExpression("ADD " + StreamEntity.COL_EVENT_CURRENT_COUNTER + " :v")
                .expressionAttributeValues(Map.of(":v", AttributeValue.builder().n("1").build()))
                .conditionExpression("attribute_exists(" + StreamEntity.COL_PK + ")")
                .build();


        return Mono.fromFuture(dynamoDbAsyncClient.updateItem(updateRequest))
                .map(resp -> {
                    Long newcounter = Long.parseLong(resp.attributes().get(StreamEntity.COL_EVENT_CURRENT_COUNTER).n());
                    log.info("updateAndGetAtomicCounter done paId={} streamId={} newcounter={}", streamEntity.getPaId(), streamEntity.getStreamId(), newcounter);
                    return newcounter;
                }).onErrorResume(ConditionalCheckFailedException.class, e -> {
                    log.warn("updateAndGetAtomicCounter conditional failed, not updating counter and retourning -1");
                    return Mono.just(-1L);
                });
    }


    @Override
    public Mono<StreamEntity> replaceEntity(StreamEntity replacedEntity, StreamEntity newEntity) {

        TransactUpdateItemEnhancedRequest<StreamEntity> updateRequest = TransactUpdateItemEnhancedRequest.builder(StreamEntity.class)
                .item(disableStream(replacedEntity))
                .ignoreNulls(true)
                .build();

        TransactPutItemEnhancedRequest<StreamEntity> createRequest = TransactPutItemEnhancedRequest.builder(StreamEntity.class)
                .item(newEntity)
                .build();


        TransactWriteItemsEnhancedRequest transactWriteItemsEnhancedRequest = TransactWriteItemsEnhancedRequest.builder()
                .addUpdateItem(table, updateRequest)
                .addPutItem(table, createRequest)
                .build();

        var f = dynamoDbEnhancedClient.transactWriteItems(transactWriteItemsEnhancedRequest);
        return Mono.fromFuture(f.thenApply(r -> newEntity));
    }

    @Override
    public Mono<StreamEntity> disable(StreamEntity entity) {
        return update(disableStream(entity));
    }

    @Override
    public Mono<Void> updateStreamRetryAfter(StreamRetryAfter entity) {
        log.info("updateStreamRetryAfter entity={}", entity);
        return Mono.fromFuture(tableRetry.putItem(entity));
    }

    private StreamEntity disableStream(StreamEntity streamEntity) {
        streamEntity.setDisabledDate(Instant.now());
        streamEntity.setTtl(Instant.now().plus(pnStreamConfigs.getDisableTtl()).atZone(ZoneId.systemDefault()).toEpochSecond());
        streamEntity.setEventAtomicCounter(null);
        return streamEntity;
    }
}
