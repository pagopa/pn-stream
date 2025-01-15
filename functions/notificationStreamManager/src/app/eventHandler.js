const { extractKinesisData } = require("../app/lib/kinesis");
const { parseKinesisObjToJsonObj } = require("../app/lib/utils");
const { DynamoDBClient, PutItemCommand } = require("@aws-sdk/client-dynamodb");

const dynamoDbClient = new DynamoDBClient({ region: process.env.REGION });
const TABLE_NAME = "pn-webhookNotification";
const MAX_TTL = process.env.MAX_TTL ? parseInt(process.env.MAX_TTL, 10) : 9223372036854775807;

exports.handleEvent = async (event) => {
  let cdcEvents;
  try {
    cdcEvents = extractKinesisData(event);
  } catch (error) {
    console.error("Error extracting Kinesis data:", error);
    return { batchItemFailures: [] };
  }
  console.log(`Batch size: ${cdcEvents.length} cdc`);

  if (cdcEvents.length === 0) {
    console.log("No events to process");
    return { batchItemFailures: [] };
  }

  let batchItemFailures = [];
  while (cdcEvents.length > 0) {
    const currentBatch = cdcEvents.splice(0, 10);

    try {
      for (const cdcEvent of currentBatch) {
        try {
          const notificationItem = buildNotificationItem(cdcEvent.dynamodb.NewImage);

          // Validate `iun` and `group` values
          if (!notificationItem.Item.hashkey.S || !notificationItem.Item.group.S) {
            const errorMessage = `Invalid data: missing or empty 'iun' or 'group' for event: ${JSON.stringify(cdcEvent)}`;
            console.error(errorMessage);
            throw new Error(errorMessage);
          }

          await putNotificationItem(notificationItem);
        } catch (error) {
          console.error("Error processing event", cdcEvent, error);
          batchItemFailures.push({ itemIdentifier: cdcEvent.kinesisSeqNumber });
        }
      }
    } catch (batchError) {
      console.error("Error processing batch of events:", currentBatch, batchError);
      batchItemFailures = batchItemFailures.concat(
        currentBatch.map((event) => ({ itemIdentifier: event.kinesisSeqNumber }))
      );
    }
  }

  if (batchItemFailures.length > 0) {
    console.log("Process finished with errors!");
  }

  return { batchItemFailures };
};

function buildNotificationItem(newImage) {
  const parsedData = parseKinesisObjToJsonObj(newImage);
  const now = Math.floor(Date.now() / 1000);
  const ttlOffset = parseInt(process.env.TTL_OFFSET, 10);
  const ttl = isNaN(ttlOffset) ? MAX_TTL : Math.max(now + ttlOffset, 0);

  // Validate `iun` and `group` during item creation
  if (!parsedData.iun || !parsedData.group) {
    throw new Error(`Missing required fields 'iun' or 'group' in data: ${JSON.stringify(parsedData)}`);
  }

  return {
    TableName: TABLE_NAME,
    Item: {
      hashkey: { S: parsedData.iun },
      group: { S: parsedData.group }, // Storing `group` as a single string
      creationDate: { N: now.toString() },
      ttl: { N: ttl.toString() },
    },
  };
}

async function putNotificationItem(item) {
  const command = new PutItemCommand(item);
  try {
    await dynamoDbClient.send(command);
  } catch (error) {
    console.error("Error putting item to DynamoDB:", error);
    throw error;
  }
}
