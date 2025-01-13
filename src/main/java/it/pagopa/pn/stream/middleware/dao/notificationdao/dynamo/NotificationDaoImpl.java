package it.pagopa.pn.stream.middleware.dao.notificationdao.dynamo;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.notification.NotificationInternal;
import it.pagopa.pn.stream.middleware.dao.notificationdao.BaseDao;
import it.pagopa.pn.stream.middleware.dao.notificationdao.NotificationDao;
import it.pagopa.pn.stream.middleware.dao.notificationdao.dynamo.entity.NotificationEntity;
import it.pagopa.pn.stream.middleware.dao.notificationdao.dynamo.mapper.DtoToEntityNotificationMapper;
import it.pagopa.pn.stream.middleware.dao.notificationdao.dynamo.mapper.EntityToDtoNotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import static it.pagopa.pn.stream.middleware.dao.notificationdao.dynamo.entity.NotificationEntity.COL_PK;

@Slf4j
@Repository
public class NotificationDaoImpl extends BaseDao<NotificationEntity> implements NotificationDao {

    protected NotificationDaoImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, PnStreamConfigs config) {
        super(dynamoDbEnhancedAsyncClient, config.getDao().getWebhookNotificationTable(), NotificationEntity.class);
    }

    @Override
    public Mono<NotificationEntity> putItem(NotificationInternal entity) {
        PutItemEnhancedRequest<NotificationEntity> putItemEnhancedRequest = PutItemEnhancedRequest
                .builder(NotificationEntity.class)
                .item(DtoToEntityNotificationMapper.dto2Entity(entity))
                .conditionExpression(Expression.builder().expression(buildExistingConditionExpression(false, COL_PK)).build())
                .build();

        return putItem(putItemEnhancedRequest, entity.getHashkey());
    }

    @Override
    public Mono<NotificationInternal> findByHashkey(String hashkey) {
        return getItem(hashkey)
                .map(EntityToDtoNotificationMapper::entity2Dto);
    }
}

