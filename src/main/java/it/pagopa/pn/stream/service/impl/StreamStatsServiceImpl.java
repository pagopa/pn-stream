package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.CustomStatsConfig;
import it.pagopa.pn.stream.dto.StatConfig;
import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamStatsDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamStatsEntity;
import it.pagopa.pn.stream.service.StreamStatsService;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class StreamStatsServiceImpl implements StreamStatsService {
    private static final String UPDATE_STREAM_STATS_LOG = "Update stream stats: {} for paId: {} and streamId: {}";

    private final StreamUtils streamUtils;
    private final StreamStatsDao streamStatsDao;

    @Override
    public Mono<StreamStatsEntity> updateStreamStats(CustomStatsConfig customStatsConfig, StreamStatsEnum streamStatsEnum, String paId, String streamId) {
        log.info(UPDATE_STREAM_STATS_LOG, streamStatsEnum, paId, streamId);
        StatConfig statConfig = null;
        if (Objects.nonNull(customStatsConfig) && Objects.nonNull(customStatsConfig.getConfig())) {
            statConfig = customStatsConfig.getConfig().get(streamStatsEnum);
        }
        StreamStatsEntity streamStatsEntity = streamUtils.buildEntity(statConfig, streamStatsEnum, paId, streamId);
        return streamStatsDao.updateAtomicCounterStats(streamStatsEntity);
    }

    @Override
    public Mono<Void> updateNumberOfWritingsStreamStats(List<StreamStatsEntity> streamStatsEntities) {
        return Flux.fromIterable(streamStatsEntities)
                .flatMap(streamStatsDao::updateAtomicCounterStats)
                .then();
    }

    @Override
    public Mono<UpdateItemResponse> updateNumberOfReadingStreamStats(CustomStatsConfig customStatsConfig, String paId, String streamId, Integer increment) {
        log.info(UPDATE_STREAM_STATS_LOG, StreamStatsEnum.NUMBER_OF_READINGS, paId, streamId);
        String pk = StreamStatsEntity.buildPk(paId, streamId, StreamStatsEnum.NUMBER_OF_READINGS);
        StatConfig statConfig = null;
        if (Objects.nonNull(customStatsConfig) && Objects.nonNull(customStatsConfig.getConfig())) {
            statConfig = customStatsConfig.getConfig().get(StreamStatsEnum.NUMBER_OF_READINGS);
        }
        String sk = streamUtils.buildSk(statConfig);
        Duration ttl = streamUtils.retrieveCustomTtl(streamUtils.retrieveStatsConfig(StreamStatsEnum.NUMBER_OF_READINGS));
        return streamStatsDao.updateCustomCounterStats(pk, sk, increment, ttl);
    }
}
