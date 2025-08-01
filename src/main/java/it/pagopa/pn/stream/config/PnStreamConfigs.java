package it.pagopa.pn.stream.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Configuration
@ConfigurationProperties( prefix = "pn.stream")
@Data
@Import({SharedAutoConfiguration.class})
public class PnStreamConfigs {

    private Dao dao;
    private Topics topics;
    private String externalRegistryBaseUrl;
    private String dataVaultBaseUrl;
    private String deliveryBaseUrl;
    private Long scheduleInterval;
    private Integer maxLength;
    private Integer maxStreams;
    private Integer purgeDeletionWaittime;
    private Integer readBufferDelay;
    private Integer deltaCounter;
    private Duration ttl;
    private Duration disableTtl;
    private String firstVersion;
    private String currentVersion;
    private String retryParameterPrefix;
    private String paConfigurationsPrefix;
    private Boolean retryAfterEnabled;
    private Long streamNotificationTtl;
    private List<String> listCategoriesPa;
    private Integer sortEventDelaySeconds;
    private Duration unlockedEventTtl;
    private Duration notificationSla;
    private Integer maxWrittenCounter;
    private Integer queryEventQuarantineLimit;
    public Integer saveEventMaxConcurrency;

    @Data
    public static class Dao {
        private String streamsTableName;
        private String eventsTableName;
        private String streamNotificationTableName;
        private String notificationUnlockedTableName;
        private String eventsQuarantineTableName;
    }

    @Data
    public static class Topics {
        private String scheduledActions;
        private String event;
        private String eventSchedule;
    }

    public Duration getMaxTtl() {
        if (Objects.isNull(notificationSla)) {
            return unlockedEventTtl;
        } else if (Objects.isNull(unlockedEventTtl)) {
            return notificationSla;
        } else {
            return notificationSla.compareTo(unlockedEventTtl) >= 0 ? notificationSla : unlockedEventTtl;
        }
    }
}
