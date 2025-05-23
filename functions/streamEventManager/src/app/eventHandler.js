const { extractKinesisData } = require("./lib/kinesis.js");
const { mapEvents } = require("./lib/eventMapper.js");
const { SQSClient, SendMessageBatchCommand } = require("@aws-sdk/client-sqs");
const { unmarshall } = require("@aws-sdk/util-dynamodb");

const sqs = new SQSClient({ region: process.env.REGION });
const QUEUE_URL = process.env.QUEUE_URL

exports.handleEvent = async (event) => {

  const cdcEvents = extractKinesisData(event);
  console.log(`Batch size: ${cdcEvents.length} cdc`);

  if (cdcEvents.length == 0) {
    console.log("No events to process");
    return {
      batchItemFailures: [],
    };
  }else{
    let batchItemFailures = [];
    while(cdcEvents.length > 0){
      let currentCdcEvents = cdcEvents.splice(0,10);
      if (currentCdcEvents.length == 0) {
        return batchItemFailures;
      }
      let filteredItems = currentCdcEvents
        .map(event => ({timelineObject : {...unmarshall(event.dynamodb.NewImage)}, kinesisSeqNumber: event.kinesisSeqNumber}))
        .filter((eventItem) => new Date(eventItem.timelineObject.timestamp) >= new Date(`${process.env.START_READ_STREAM_TIMESTAMP}`) && new Date(eventItem.timelineObject.timestamp) < new Date(`${process.env.STOP_READ_STREAM_TIMESTAMP}`))
      try{
        let processedItems = mapEvents(filteredItems);
        if (processedItems.length > 0){
          let responseError = await sendMessages(processedItems);

          if(responseError.length > 0){
            console.log('Error in persist current cdcEvents: ', filteredItems);
            batchItemFailures = batchItemFailures.concat(responseError.map((i) => {
            return { itemIdentifier: i.kinesisSeqNumber };
            }));
          }
        }else{
          console.log('No events to persist in current cdcEvents: ',filteredItems);
        }
      }catch(exc){
        console.log('Error in persist current cdcEvents: ', filteredItems);
        batchItemFailures = batchItemFailures.concat(filteredItems.map((i) => {
          return { itemIdentifier: i.kinesisSeqNumber };
        }));
      }
    }
    if(batchItemFailures.length > 0){
      console.log('process finished with error!');
    }
    return {
      batchItemFailures: batchItemFailures,
    };
  }
           
};

async function sendMessages(messages) {
  let error = [];
  try{
    
      console.log('Proceeding to send ' + messages.length + ' messages to ' + QUEUE_URL);
      const input = {
        Entries: messages, 
        QueueUrl: QUEUE_URL
      }

      console.log('Sending batch message: %j', input);

      const command = new SendMessageBatchCommand(input);
      const response = await sqs.send(command);
      
      if (response.Failed && response.Failed.length > 0)
      {
        console.log("error sending some message totalErrors:" + response.Failed.length);

        error = error.concat(response.Failed.map((i) => {
          return { kinesisSeqNumber : i.Id };
        }));
        
      }

  }catch(exc){
      console.log("error sending message", exc)
      throw exc;
  }
  return error;

};
