const { unmarshall } = require("@aws-sdk/util-dynamodb");
const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  PutCommand,
  DynamoDBDocumentClient
} = require("@aws-sdk/lib-dynamodb");

const client = new DynamoDBClient({});
const docClient = DynamoDBDocumentClient.from(client);

function buildNotificationItem(newImage, tableName, ttlOffset) {
  const parsedData = unmarshall(newImage);
  console.log(parsedData);
  const now = Math.floor(Date.now() / 1000);
  const ttl = Math.max(now + ttlOffset, 0);

  // Validate `iun` during item creation
  if (!parsedData.iun) {
    throw new Error(`Missing required fields 'iun' in data: ${JSON.stringify(parsedData)}`);
  }

  return {
      hashKey: parsedData.iun,
      group: parsedData.group ? parsedData.group : null,
      creationDate: parsedData.sentAt ? parsedData.sentAt : null,
      ttl: ttl,
  };
}

async function putNotificationItem(item) {
  console.log(item);
  const command = new PutCommand({
    TableName: process.env.TABLE_NAME,
    Item: item,
  });

  try {
    await docClient.send(command);
  } catch (error) {
    console.error("Error putting item to DynamoDB:", error);
    throw error;
  }
}

module.exports = { putNotificationItem, buildNotificationItem };