package it.pagopa.pn.stream.utils;

import it.pagopa.pn.commons.log.dto.metrics.Dimension;
import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.commons.log.dto.metrics.Metric;

import java.util.List;

public class MetricUtils {

    private MetricUtils() { }

    public static GeneralMetric generateGeneralMetric(String paId, String streamId, String metricName, String metricValue, String timeline) {
        GeneralMetric generalMetric = new GeneralMetric();
        generalMetric.setNamespace("stream-statistics");
        generalMetric.setDimensions(List.of(new Dimension("paId", paId), new Dimension("streamId", streamId)));
        generalMetric.setMetrics(List.of(new Metric(metricName, metricValue)));
        generalMetric.setTimestamp(timeline);
        return generalMetric;
    }
}
