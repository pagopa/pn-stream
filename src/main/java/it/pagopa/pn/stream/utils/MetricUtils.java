package it.pagopa.pn.stream.utils;

import it.pagopa.pn.commons.log.dto.metrics.Dimension;
import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.commons.log.dto.metrics.Metric;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;

import java.util.List;

public class MetricUtils {

    private MetricUtils() { }

    public static GeneralMetric generateGeneralMetric(String paId, String streamId, String metricName, int metricValue, long timeline) {
        GeneralMetric generalMetric = new GeneralMetric();
        generalMetric.setNamespace("stream-statistics");
        generalMetric.setDimensions(List.of(new Dimension("paId", paId), new Dimension("streamId", streamId)));
        generalMetric.setMetrics(List.of(new Metric(metricName, metricValue)));
        generalMetric.setTimestamp(timeline);
        return generalMetric;
    }

    public static List<GeneralMetric> generateListOfGeneralMetricsFromStreams(List<StreamEntity> listOfStreams, String metricName, int metricValue, long timeline) {
        return listOfStreams.stream().map(stream -> generateGeneralMetric(stream.getPaId(), stream.getStreamId(), metricName, metricValue, timeline)).toList();
    }
}
