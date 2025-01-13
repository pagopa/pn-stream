const { parseKinesisObjToJsonObj } = require("./utils");
const crypto = require('crypto');


exports.mapEvents = async (events) => {
  let result = [];

  for (let i = 0; i < events.length; i++) {

    let timelineObj = parseKinesisObjToJsonObj(events[i].dynamodb.NewImage);

    let date = new Date();

    let action = {
      event: timelineObj,
      eventId: `${date.toISOString()}_${timelineObj.timelineElementId}`,
      type: 'REGISTER_EVENT'
    };

    let messageAttributes = {
      publisher: {
        DataType: 'String',
        StringValue: 'deliveryPush'
      },
      iun: {
        DataType: 'String',
        StringValue: action.event.iun
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

    let resultElement = {
      Id: events[i].kinesisSeqNumber,
      MessageAttributes: messageAttributes,
      MessageBody: JSON.stringify(action)
    };

    result.push(resultElement);

  }
  return result;
};


