package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.stats.StatsTimeUnit;
import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamStatsDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamStatsEntity;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.Mockito.*;

class StreamStatsServiceImplTest {
    private StreamUtils streamUtils;
    private StreamStatsDao streamStatsDao;
    private PnStreamConfigs pnStreamConfigs;
    private StreamStatsServiceImpl webhookStatsService;

    @BeforeEach
    void setup() {
        streamUtils = Mockito.mock(StreamUtils.class);
        streamStatsDao = Mockito.mock(StreamStatsDao.class);
        pnStreamConfigs = Mockito.mock(PnStreamConfigs.class);

        webhookStatsService = new StreamStatsServiceImpl(streamUtils, streamStatsDao, pnStreamConfigs);
    }

    @Test
    void updateWebhookStatsShouldUpdateStreamStatsSuccessfully() {
        StreamStatsEnum streamStatsEnum = StreamStatsEnum.NUMBER_OF_REQUESTS;
        String paId = "paId";
        String streamId = "streamId";
        Instant currentInterval = Instant.now();
        when(streamUtils.retrieveCurrentInterval()).thenReturn(currentInterval);
        PnStreamConfigs.Stats statsConfig = new PnStreamConfigs.Stats();
        statsConfig.setTtl(Duration.ofDays(30));
        statsConfig.setTimeUnit(StatsTimeUnit.DAYS);
        statsConfig.setSpanUnit(1);
        when(pnStreamConfigs.getStats()).thenReturn(statsConfig);
        when(pnStreamConfigs.getTtl()).thenReturn(Duration.ofDays(30));

        StreamStatsEntity streamStatsEntity = new StreamStatsEntity();
        streamStatsEntity.setPk(paId + "_" + streamId + "_" + streamStatsEnum);
        streamStatsEntity.setSk(currentInterval.toString() + "#" + StatsTimeUnit.DAYS + "#" + 1);

        when(streamStatsDao.updateAtomicCounterStats(Mockito.any(StreamStatsEntity.class)))
                .thenReturn(Mono.just(streamStatsEntity));

        webhookStatsService.updateStreamStats(streamStatsEnum, paId, streamId);

        ArgumentCaptor<StreamStatsEntity> captor = ArgumentCaptor.forClass(StreamStatsEntity.class);
        verify(streamStatsDao, times(1)).updateAtomicCounterStats(captor.capture());

        StreamStatsEntity capturedEntity = captor.getValue();
        Assertions.assertEquals(paId + "_" + streamId + "_" + streamStatsEnum, capturedEntity.getPk());
        Assertions.assertEquals(currentInterval + "#" + StatsTimeUnit.DAYS + "#" + 1, capturedEntity.getSk());
    }

}