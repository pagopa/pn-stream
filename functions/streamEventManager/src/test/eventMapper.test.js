const { expect } = require("chai");
const sinon = require("sinon");
const proxyquire = require("proxyquire").noCallThru();
const fs = require("fs");
const { unmarshall } = require("@aws-sdk/util-dynamodb");
const { mapEvents } = require("../app/lib/eventMapper");

describe("event mapper tests", function () {

  it("test mapping", async () => {
    const eventJSON = fs.readFileSync("./src/test/eventMapper.timeline.json");
    let event = JSON.parse(eventJSON);
    event = setCategory(event, "REQUEST_REFUSED");

    const unmarshalled = { ...unmarshall(event.dynamodb.NewImage) };
    const events = [{ timelineObject: unmarshalled }];

    const res = await mapEvents(events);

    console.log(res[0]);

    let body = JSON.parse(res[0].MessageBody);
    expect(body.timelineElementInternal.iun).equal("abcd");
    expect(body.timelineElementInternal.paId).equal("026e8c72-7944-4dcd-8668-f596447fec6d");
    expect(body.timelineElementInternal.timelineElementId).equal("notification_viewed_creation_request;IUN_XLDW-MQYJ-WUKA-202302-A-1;RECINDEX_1");
    expect(body.type).equal("REGISTER_EVENT");

    // details must be stringified only at mapping time
    expect(body.timelineElementInternal.details).to.be.a("string");
    expect(body.timelineElementInternal.details).to.equal(
      JSON.stringify(unmarshalled.details)
    );

    expect(res[0].MessageAttributes.publisher.StringValue).equal("deliveryPush");
    expect(res[0].MessageAttributes.iun.StringValue).equal("abcd");
    expect(res[0].MessageAttributes.eventType.StringValue).equal("WEBHOOK_ACTION_GENERIC");

    console.log('OK');
  
  });

  it("enrich: returns same array when no relatedTimelineElements", async () => {
    const batchGetStub = sinon.stub().resolves({});
    const { enrichReworkedItemsWithTimelineElements } = proxyquire(
      "../app/lib/eventMapper",
      {
        "./dynamo.js": {
          batchGetTimelineElements: batchGetStub
        }
      }
    );

    const reworkedItems = [
      {
        timelineObject: {
          iun: "IUN_1",
          details: {}
        }
      }
    ];

    const res = await enrichReworkedItemsWithTimelineElements(
      "IUN_1",
      reworkedItems
    );

    expect(res).to.equal(reworkedItems);
    expect(batchGetStub.called).to.be.false;
  });

  it("enrich: replaces ids with items and filters missing", async () => {
    const batchGetStub = sinon.stub().resolves({
      A: { timelineElementId: "A", extra: 1 },
      B: { timelineElementId: "B", extra: 2 },
      C: { timelineElementId: "C", extra: 3 }
    });

    const { enrichReworkedItemsWithTimelineElements } = proxyquire(
      "../app/lib/eventMapper",
      {
        "./dynamo.js": {
          batchGetTimelineElements: batchGetStub
        }
      }
    );

    const reworkedItems = [
      {
        timelineObject: {
          iun: "IUN_1",
          details: {
            invalidatedTimelineAndStatusHistory: [
              { relatedTimelineElements: ["A", "B", "MISSING"] }
            ]
          }
        }
      },
      {
        timelineObject: {
          iun: "IUN_1",
          details: {
            invalidatedTimelineAndStatusHistory: [
              { relatedTimelineElements: ["B", "C"] },
              { relatedTimelineElements: [] }
            ]
          }
        }
      }
    ];

    const res = await enrichReworkedItemsWithTimelineElements(
      "IUN_1",
      reworkedItems
    );

    expect(batchGetStub.calledOnce).to.be.true;
    expect(batchGetStub.firstCall.args[0]).to.equal("IUN_1");
    expect(batchGetStub.firstCall.args[1]).to.deep.equal([
      "A",
      "B",
      "MISSING",
      "C"
    ]);

    // no mutation: original arrays still contain ids
    expect(
      reworkedItems[0].timelineObject.details.invalidatedTimelineAndStatusHistory[0]
        .relatedTimelineElements[0]
    ).to.equal("A");

    // enriched items are rebuilt
    expect(res[0]).to.not.equal(reworkedItems[0]);
    expect(res[1]).to.not.equal(reworkedItems[1]);

    const enriched0 =
      res[0].timelineObject.details.invalidatedTimelineAndStatusHistory[0]
        .relatedTimelineElements;
    expect(enriched0).to.have.length(2);
    expect(enriched0.map(e => e.timelineElementId)).to.deep.equal(["A", "B"]);

    const enriched1_0 =
      res[1].timelineObject.details.invalidatedTimelineAndStatusHistory[0]
        .relatedTimelineElements;
    expect(enriched1_0.map(e => e.timelineElementId)).to.deep.equal(["B", "C"]);
  });

  it("enrich: keeps items without invalidated history untouched", async () => {
    const batchGetStub = sinon.stub().resolves({
      X: { timelineElementId: "X" }
    });

    const { enrichReworkedItemsWithTimelineElements } = proxyquire(
      "../app/lib/eventMapper",
      {
        "./dynamo.js": {
          batchGetTimelineElements: batchGetStub
        }
      }
    );

    const untouched = {
      timelineObject: {
        iun: "IUN_1",
        details: { someOtherField: true }
      }
    };

    const toEnrich = {
      timelineObject: {
        iun: "IUN_1",
        details: {
          invalidatedTimelineAndStatusHistory: [
            { relatedTimelineElements: ["X"] }
          ]
        }
      }
    };

    const res = await enrichReworkedItemsWithTimelineElements("IUN_1", [
      untouched,
      toEnrich
    ]);

    expect(res[0]).to.equal(untouched);
    expect(res[1]).to.not.equal(toEnrich);
    expect(
      res[1].timelineObject.details.invalidatedTimelineAndStatusHistory[0]
        .relatedTimelineElements
    ).to.deep.equal([{ timelineElementId: "X" }]);
  });

});

function setCategory(event, category) {
  event.dynamodb.NewImage.category = {
    S: category,
  };
  return event;
}
