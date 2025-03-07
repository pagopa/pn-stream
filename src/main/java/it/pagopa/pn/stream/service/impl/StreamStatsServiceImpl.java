package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamStatsDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamStatsEntity;
import it.pagopa.pn.stream.service.StreamStatsService;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Duration;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class StreamStatsServiceImpl implements StreamStatsService {
    private static final String UPDATE_STREAM_STATS_LOG = "Update stream stats: {} for paId: {} and streamId: {}";

    private final StreamUtils streamUtils;
    private final StreamStatsDao streamStatsDao;

    @Override
    public Mono<StreamStatsEntity> updateStreamStats(StreamStatsEnum streamStatsEnum, String paId, String streamId) {
        log.info(UPDATE_STREAM_STATS_LOG, streamStatsEnum, paId, streamId);
        StreamStatsEntity streamStatsEntity = streamUtils.buildEntity(streamStatsEnum, paId, streamId);
        return streamStatsDao.updateAtomicCounterStats(streamStatsEntity);
    }

    @Override
    public Mono<UpdateItemResponse> updateNumberOfReadingStreamStats(String paId, String streamId, Integer increment) {
        log.info(UPDATE_STREAM_STATS_LOG, StreamStatsEnum.NUMBER_OF_READINGS, paId, streamId);
        String pk = StreamStatsEntity.buildPk(paId, streamId, StreamStatsEnum.NUMBER_OF_READINGS);
        String sk = streamUtils.buildSk(StreamStatsEnum.NUMBER_OF_READINGS);
        Duration ttl = streamUtils.retrieveCustomTtl(streamUtils.retrieveStatsConfig(StreamStatsEnum.NUMBER_OF_READINGS));
        return streamStatsDao.updateCustomCounterStats(pk, sk, increment, ttl);
    }
}
