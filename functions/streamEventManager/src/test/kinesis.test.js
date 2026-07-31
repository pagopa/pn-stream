const { expect } = require("chai");
const { extractKinesisData } = require("../app/lib/kinesis");

function recordFromPayload(payload, sequenceNumber = "seq1") {
  return {
    kinesis: {
      sequenceNumber,
      data: Buffer.from(JSON.stringify(payload), "utf8").toString("base64"),
    },
  };
}

describe("stream kinesis extractor", () => {
  it("keeps events without communicationType", () => {
    const payload = { dynamodb: { NewImage: { iun: { S: "IUN1" } } } };
    const res = extractKinesisData({ Records: [recordFromPayload(payload)] });
    expect(res).to.have.length(1);
  });

  it("keeps events with INFORMAL communicationType", () => {
    const payload = { dynamodb: { NewImage: { communicationType: { S: "informal" } } } };
    const res = extractKinesisData({ Records: [recordFromPayload(payload)] });
    expect(res).to.have.length(1);
  });

  it("skips events with unsupported communicationType", () => {
    const payload = { dynamodb: { NewImage: { communicationType: { N: 1 } } } };
    const res = extractKinesisData({ Records: [recordFromPayload(payload)] });
    expect(res).to.have.length(0);
  });
});
