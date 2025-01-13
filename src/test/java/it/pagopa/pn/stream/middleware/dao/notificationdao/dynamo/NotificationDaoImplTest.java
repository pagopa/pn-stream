package it.pagopa.pn.stream.middleware.dao.notificationdao.dynamo;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.notification.NotificationInternal;
import it.pagopa.pn.stream.middleware.dao.notificationdao.dynamo.entity.NotificationEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationDaoImplTest {

    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient = Mockito.mock(DynamoDbEnhancedAsyncClient.class);
    private NotificationDaoImpl notificationDao;
    DynamoDbAsyncTable<NotificationEntity> tableAsync;

    @BeforeAll
    void setUp() {
        tableAsync = mock(DynamoDbAsyncTable.class);
        PnStreamConfigs pnStreamConfigs = new PnStreamConfigs();
        PnStreamConfigs.Dao dao = new PnStreamConfigs.Dao();
        dao.setWebhookNotificationTable("pn-notification");
        pnStreamConfigs.setDao(dao);
        when(dynamoDbEnhancedAsyncClient.table(anyString(), (TableSchema<NotificationEntity>) any())).thenReturn(tableAsync);
        notificationDao = new NotificationDaoImpl(dynamoDbEnhancedAsyncClient, pnStreamConfigs);
    }

    @Test
    void putItem_withValidNotification() {
        NotificationInternal notification = new NotificationInternal();
        notification.setHashkey("hashkey");
        notification.setGroups(Collections.singletonList("group1"));
        notification.setCreationDate(Instant.now());
        notification.setTtl(3600L);

        CompletableFuture<NotificationInternal> completedFutureEntity = CompletableFuture.completedFuture(notification);
        when(tableAsync.putItem(any(PutItemEnhancedRequest.class))).thenReturn(completedFutureEntity);

        Mono<NotificationEntity> result = notificationDao.putItem(notification);

        assertNotNull(result.block());
    }

    @Test
    void findByHashkey_withExistingHashkey() {
        String hashkey = "existingHashkey";
        NotificationEntity entity = new NotificationEntity();
        entity.setHashkey(hashkey);

        CompletableFuture<NotificationEntity> completedFutureEntity = CompletableFuture.completedFuture(entity);
        when(tableAsync.getItem(any(Key.class))).thenReturn(completedFutureEntity);

        Mono<NotificationInternal> result = notificationDao.findByHashkey(hashkey);

        assertEquals(hashkey, result.block().getHashkey());
    }

    @Test
    void findByHashkey_withNonExistingHashkey() {
        String hashkey = "nonExistingHashkey";

        CompletableFuture<NotificationEntity> completedFutureEntity = CompletableFuture.completedFuture(null);
        when(tableAsync.getItem(any(Key.class))).thenReturn(completedFutureEntity);

        Mono<NotificationInternal> result = notificationDao.findByHashkey(hashkey);

        assertNull(result.block());
    }
}