package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamStatsDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamStatsEntity;
import it.pagopa.pn.stream.service.StreamStatsService;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Slf4j
@AllArgsConstructor
public class StreamStatsServiceImpl implements StreamStatsService {
    private final StreamUtils streamUtils;
    private final StreamStatsDao streamStatsDao;
    private final PnStreamConfigs pnStreamConfigs;

    @Override
    public void updateStreamStats(StreamStatsEnum streamStatsEnum, String paId, String streamId) {
        log.info("Update stream stats: {} for paId: {} and streamId: {}", streamStatsEnum, paId, streamId);
        streamUtils.retrieveCurrentInterval();
        StreamStatsEntity streamStatsEntity = buildEntity(streamStatsEnum, paId, streamId);
        streamStatsDao.updateAtomicCounterStats(streamStatsEntity);
        log.info("Stream stats updated: {} for paId: {} and streamId: {}", streamStatsEnum, paId, streamId);
    }

    private StreamStatsEntity buildEntity(StreamStatsEnum streamStatsEnum, String paId, String streamId) {
        log.info("Build entity for stream stats: {} for paId: {} and streamId: {}", streamStatsEnum, paId, streamId);
        StreamStatsEntity streamStatsEntity = new StreamStatsEntity();
        streamStatsEntity.setPk(paId + "_" + streamId + "_" + streamStatsEnum.toString());
        streamStatsEntity.setSk(streamUtils.retrieveCurrentInterval() + "#" + pnStreamConfigs.getStats().getTimeUnit() + "#" + pnStreamConfigs.getStats().getSpanUnit());
        if (!pnStreamConfigs.getStats().getTtl().isZero())
            streamStatsEntity.setTtl(LocalDateTime.now().plus(pnStreamConfigs.getStats().getTtl()).atZone(ZoneId.systemDefault()).toEpochSecond());
        log.info("Entity built for stream stats: {} for paId: {} and streamId: {}", streamStatsEnum, paId, streamId);
        return streamStatsEntity;
    }
}
