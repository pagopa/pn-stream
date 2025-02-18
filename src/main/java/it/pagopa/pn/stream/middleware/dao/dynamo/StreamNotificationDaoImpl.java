package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamNotificationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Slf4j
@Repository
public class StreamNotificationDaoImpl implements StreamNotificationDao {

    private final DynamoDbAsyncTable<StreamNotificationEntity> table;

    public StreamNotificationDaoImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnStreamConfigs cfg) {
        this.table = dynamoDbEnhancedClient.table(cfg.getDao().getStreamNotificationTableName(), TableSchema.fromBean(StreamNotificationEntity.class));
    }

    @Override
    public Mono<StreamNotificationEntity> findByIun(String iun) {
        log.debug("findByIun {}", iun);
        Key key = Key.builder().partitionValue(iun).build();
        return Mono.fromFuture(table.getItem(key));
    }

    @Override
    public Mono<StreamNotificationEntity> putItem(StreamNotificationEntity streamNotification) {
        return Mono.fromFuture(table.putItem(streamNotification).thenApply(r -> streamNotification));
    }
}

