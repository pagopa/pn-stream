const crypto = require("crypto");
const { batchGetTimelineElements } = require("./dynamo.js");

/**
 * Map enriched timeline events to SQS messages
 */
function mapEvents(events) {
  let result = [];

  for (let i = 0; i < events.length; i++) {

    let timelineEvent = events[i];

    let date = new Date();

    // stringify details ONLY here (after enrichment)
    const timelineObject = {
      ...timelineEvent.timelineObject,
      details: JSON.stringify(timelineEvent.timelineObject.details)
    };

    const action = {
      timelineElementInternal: timelineObject,
      eventId: `${date.toISOString()}_${timelineObject.timelineElementId}`,
      type: "REGISTER_EVENT"
    };

    let messageAttributes = {
      publisher: {
        DataType: 'String',
        StringValue: 'deliveryPush'
      },
      iun: {
        DataType: 'String',
        StringValue: timelineEvent.timelineObject.iun
      },
      eventId: {
        DataType: 'String',
        StringValue: crypto.randomUUID()
      },
      createdAt: {
        DataType: 'String',
        StringValue: date.toISOString()
      }, 
      eventType:  {
        DataType: 'String',
        StringValue:'WEBHOOK_ACTION_GENERIC'
      },
    };

    result.push({
      Id: timelineEvent.kinesisSeqNumber,
      MessageAttributes: messageAttributes,
      MessageBody: JSON.stringify(action)
    });
  }

  return result;
}

/**
 * Enrich reworked items replacing relatedTimelineElements ids
 * with full timeline elements from DynamoDB
 *
 * @param {string} iun
 * @param {Array} reworkedItems
 * @returns {Promise<Array>}
 */
async function enrichReworkedItemsWithTimelineElements(iun, reworkedItems) {
  const timelineElementIds = [
    ...new Set(
      reworkedItems.flatMap(item =>
        item.timelineObject.details?.invalidatedTimelineAndStatusHistory
          ?.flatMap(h => h.relatedTimelineElements ?? []) ?? []
      )
    )
  ];

  if (timelineElementIds.length === 0) {
    return reworkedItems;
  }

  const timelineItemById = await batchGetTimelineElements(
    iun,
    timelineElementIds
  );

  return reworkedItems.map(item => {
    const details = item.timelineObject.details;

    if (!details?.invalidatedTimelineAndStatusHistory) {
      return item;
    }

    return {
      ...item,
      timelineObject: {
        ...item.timelineObject,
        details: {
          ...details,
          invalidatedTimelineAndStatusHistory:
            details.invalidatedTimelineAndStatusHistory.map(history => ({
              ...history,
              relatedTimelineElements:
                history.relatedTimelineElements
                  .map(elementId => timelineItemById[elementId])
                  .filter(Boolean)
            }))
        }
      }
    };
  });
}

module.exports = {
  mapEvents,
  enrichReworkedItemsWithTimelineElements
};
