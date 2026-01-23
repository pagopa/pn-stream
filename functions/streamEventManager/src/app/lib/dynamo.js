const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  BatchGetCommand,
  DynamoDBDocumentClient
} = require("@aws-sdk/lib-dynamodb");

const client = new DynamoDBClient({ region: process.env.REGION });
const docClient = DynamoDBDocumentClient.from(client);

const TABLE_NAME = process.env.PN_TIMELINES_TABLE_NAME;

if (!TABLE_NAME) {
  throw new Error("PN_TIMELINES_TABLE_NAME env variable is not defined");
}

/**
 * BatchGet timeline elements by iun + elementId
 *
 * @param {string} iun
 * @param {string[]} elementIds
 * @returns {Promise<Record<string, any>>} map elementId -> full item
 */
async function batchGetTimelineElements(iun, elementIds = []) {
  if (!iun || elementIds.length === 0) {
    return {};
  }

  const chunks = [];
  for (let i = 0; i < elementIds.length; i += 100) {
    chunks.push(elementIds.slice(i, i + 100));
  }

  const resultMap = {};

  for (const chunk of chunks) {
    let requestItems = {
      [TABLE_NAME]: {
        Keys: chunk.map(timelineElementId => ({
          iun,
          timelineElementId
        }))
      }
    };

    do {
      const command = new BatchGetCommand({
        RequestItems: requestItems
      });

      const response = await docClient.send(command);

      const items = response.Responses?.[TABLE_NAME] ?? [];
      for (const item of items) {
        resultMap[item.timelineElementId] = item;
      }

      requestItems = response.UnprocessedKeys;

      if (requestItems && Object.keys(requestItems).length > 0) {
        console.warn("BatchGetItem retry due to UnprocessedKeys", {
          table: TABLE_NAME,
          unprocessedCount: requestItems[TABLE_NAME]?.Keys?.length ?? 0
        });
      }

    } while (requestItems && Object.keys(requestItems).length > 0);
  }
  return resultMap;
}

module.exports = {
  batchGetTimelineElements
};
