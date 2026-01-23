// test/dynamo.spec.js
const { expect } = require("chai");
const sinon = require("sinon");
const proxyquire = require("proxyquire");

describe("updateRework (dynamo.js)", () => {
  let sendStub;
  let DynamoDBDocumentClientFromStub;
  let dynamo;
  const TABLE_NAME = "pn-Timelines";

  process.env = Object.assign(process.env, {
    START_READ_STREAM_TIMESTAMP: "1999-01-01T00:00:00Z",
    STOP_READ_STREAM_TIMESTAMP: "2099-01-01T00:00:00Z",
    PN_TIMELINES_TABLE_NAME: TABLE_NAME
  });

  beforeEach(() => {

    sendStub = sinon.stub().resolves();
    DynamoDBDocumentClientFromStub = sinon.stub().returns({ send: sendStub });

    BatchGetCommandStub = sinon.stub().callsFake((params) => ({ __params: params }));

    dynamo = proxyquire("../app/lib/dynamo.js", {
      "@aws-sdk/client-dynamodb": {
        DynamoDBClient: function DynamoDBClient() {}
      },
      "@aws-sdk/lib-dynamodb": {
        DynamoDBDocumentClient: { from: DynamoDBDocumentClientFromStub },
        BatchGetCommand: BatchGetCommandStub
      }
    });
  });

  afterEach(() => {
    sinon.restore();
  });

  it("invia BatchGetCommand (non-ERROR)", async () => {
    iun = "IUN_PROVA"

    let response = {Responses: { [TABLE_NAME]: [ { timelineElementId: "elementId1" }, { timelineElementId: "elementId12" } ] } }
    sendStub.resolves(response);
    const res = await dynamo.batchGetTimelineElements(iun, ["elementId1", "elementId12"]);
    expect(res).to.deep.equal({elementId1: { timelineElementId: "elementId1" }, elementId12: { timelineElementId: "elementId12" }});
    expect(BatchGetCommandStub.calledOnce).to.be.true;

    const sent = BatchGetCommandStub.firstCall.args[0];
    expect(sent.RequestItems["pn-Timelines"].Keys).to.to.have.length(2);
    expect(sent.RequestItems["pn-Timelines"].Keys[0].iun).to.deep.equal("IUN_PROVA");
    expect(sent.RequestItems["pn-Timelines"].Keys[0].timelineElementId).to.deep.equal("elementId1");
    expect(sent.RequestItems["pn-Timelines"].Keys[1].iun).to.deep.equal("IUN_PROVA");
    expect(sent.RequestItems["pn-Timelines"].Keys[1].timelineElementId).to.deep.equal("elementId12");
    expect(sendStub.calledOnce).to.be.true;
  });
});
