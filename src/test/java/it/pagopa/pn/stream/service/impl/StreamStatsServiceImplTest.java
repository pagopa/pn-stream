package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.CustomStatsConfig;
import it.pagopa.pn.stream.dto.StatConfig;
import it.pagopa.pn.stream.dto.stats.StatsTimeUnit;
import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamStatsDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamStatsEntity;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static it.pagopa.pn.stream.dto.stats.StreamStatsEnum.NUMBER_OF_READINGS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class StreamStatsServiceImplTest {
    private StreamUtils streamUtils;
    private StreamStatsDao streamStatsDao;
    private PnStreamConfigs pnStreamConfigs;
    private PnStreamConfigs.Stats pnStreamConfigsStats;
    private StreamStatsServiceImpl streamStatsService;

    @BeforeEach
    void setup() {
        streamUtils = Mockito.mock(StreamUtils.class);
        streamStatsDao = Mockito.mock(StreamStatsDao.class);
        pnStreamConfigs = Mockito.mock(PnStreamConfigs.class);

        pnStreamConfigsStats = new PnStreamConfigs.Stats();
        pnStreamConfigsStats.setTtl(Duration.ofDays(30));
        pnStreamConfigsStats.setTimeUnit(StatsTimeUnit.DAYS);
        pnStreamConfigsStats.setSpanUnit(1);

        streamStatsService = new StreamStatsServiceImpl(streamUtils, streamStatsDao);
    }

    @Test
    void updateStreamStatsShouldUpdateAtomicCounterStats() {
        Instant currentInterval = Instant.now();

        when(streamUtils.retrieveCurrentInterval(StatsTimeUnit.DAYS, 1)).thenReturn(currentInterval);
        when(pnStreamConfigs.getStats()).thenReturn(pnStreamConfigsStats);

        StreamStatsEntity streamStatsEntity = new StreamStatsEntity("paId", "streamId", StreamStatsEnum.NUMBER_OF_REQUESTS);
        streamStatsEntity.setPk("paId" + "_" + "streamId" + "_" + StreamStatsEnum.NUMBER_OF_REQUESTS);
        streamStatsEntity.setSk(currentInterval.toString() + "#" + StatsTimeUnit.DAYS + "#" + 1);
        streamStatsEntity.setTtl(LocalDateTime.now().plus(Duration.ofDays(30)).atZone(ZoneOffset.UTC).toEpochSecond());

        when(streamUtils.buildEntity(eq(null), eq(StreamStatsEnum.NUMBER_OF_REQUESTS), eq("paId"), eq("streamId"))).thenReturn(streamStatsEntity);
        when(streamStatsDao.updateAtomicCounterStats(streamStatsEntity)).thenReturn(Mono.just(streamStatsEntity));

        streamStatsService.updateStreamStats(null, StreamStatsEnum.NUMBER_OF_REQUESTS, "paId", "streamId").block();

        Mockito.verify(streamStatsDao).updateAtomicCounterStats(streamStatsEntity);
    }

    @Test
    void updateStreamStatsShouldUpdateCustomAtomicCounterStats() {
        Instant currentInterval = Instant.now();
        when(streamUtils.retrieveCurrentInterval(StatsTimeUnit.DAYS, 1)).thenReturn(currentInterval);
        when(pnStreamConfigs.getStats()).thenReturn(pnStreamConfigsStats);

        StreamStatsEntity streamStatsEntity = new StreamStatsEntity("paId", "streamId", NUMBER_OF_READINGS);
        streamStatsEntity.setPk("paId#streamId#" + NUMBER_OF_READINGS);
        streamStatsEntity.setSk(currentInterval.toString() + "#" + StatsTimeUnit.DAYS + "#" + 1);
        streamStatsEntity.setTtl(LocalDateTime.now().plus(Duration.ofDays(30)).atZone(ZoneOffset.UTC).toEpochSecond());

        when(streamUtils.buildEntity(any(), eq(NUMBER_OF_READINGS), eq("paId"), eq("streamId"))).thenReturn(streamStatsEntity);
        when(streamUtils.buildSk(any())).thenReturn(streamStatsEntity.getSk());
        when(streamUtils.retrieveStatsConfig(NUMBER_OF_READINGS)).thenReturn(null);
        when(streamUtils.retrieveCustomTtl(any())).thenReturn(Duration.ofDays(30));
        when(streamStatsDao.updateCustomCounterStats(streamStatsEntity.getPk(), streamStatsEntity.getSk(), 3, Duration.ofDays(30))).thenReturn(Mono.empty());

        CustomStatsConfig customStatsConfig = new CustomStatsConfig();
        customStatsConfig.setConfig(Map.of(NUMBER_OF_READINGS, new StatConfig()));
        streamStatsService.updateNumberOfReadingStreamStats(customStatsConfig,"paId", "streamId", 3).block();

        Mockito.verify(streamStatsDao).updateCustomCounterStats(streamStatsEntity.getPk(), streamStatsEntity.getSk(), 3, Duration.ofDays(30));
    }

}
