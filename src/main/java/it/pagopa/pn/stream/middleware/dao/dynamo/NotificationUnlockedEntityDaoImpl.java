package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.NotificationUnlockedEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Slf4j
@Repository
public class NotificationUnlockedEntityDaoImpl implements NotificationUnlockedEntityDao {

    private final DynamoDbAsyncTable<NotificationUnlockedEntity> table;

    public NotificationUnlockedEntityDaoImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnStreamConfigs cfg) {
        this.table = dynamoDbEnhancedClient.table(cfg.getDao().getNotificationUnlockedTableName(), TableSchema.fromBean(NotificationUnlockedEntity.class));
    }

    @Override
    public Mono<NotificationUnlockedEntity> findByPk(String pk) {
        log.debug("findByPk key : {}", pk);
        Key key = Key.builder().partitionValue(pk).build();
        return Mono.fromFuture(table.getItem(key));
    }

    @Override
    public Mono<NotificationUnlockedEntity> putItem(NotificationUnlockedEntity entity) {
        return Mono.fromFuture(table.putItem(entity).thenApply(r -> entity));
    }
}