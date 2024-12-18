package it.pagopa.pn.stream.middleware.dao.timelinedao;

import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;

import java.util.Optional;
import java.util.Set;

public interface TimelineDao {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.timeline-dao";

    void addTimelineElementIfAbsent(TimelineElementInternal dto);
    
    Optional<TimelineElementInternal> getTimelineElement( String iun, String timelineId );

    Optional<TimelineElementInternal> getTimelineElementStrongly(String iun, String timelineId);

    Set<TimelineElementInternal> getTimeline(String iun );

    Set<TimelineElementInternal> getTimelineStrongly(String iun );

    Set<TimelineElementInternal> getTimelineFilteredByElementId(String iun , String timelineId);

    void deleteTimeline( String iun );

}