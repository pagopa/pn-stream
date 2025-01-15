const { expect } = require("chai");
const proxyquire = require("proxyquire").noPreserveCache();

describe("Lambda Handler Tests", () => {
  const mockDynamoDBClient = {
    send: async () => ({}), // Simulating a successful DynamoDB operation
  };

  const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
    "@aws-sdk/client-dynamodb": {
      DynamoDBClient: class {
        constructor() {
          return mockDynamoDBClient;
        }
      },
      PutItemCommand: class {
        constructor(input) {
          this.input = input;
        }
      },
    },
    "../app/lib/kinesis": {
      extractKinesisData: (event) => event.mockKinesisData || [], // Simulate Kinesis data extraction
    },
    "../app/lib/utils": {
      parseKinesisObjToJsonObj: (newImage) => ({
        iun: newImage.iun?.S,
        group: newImage.group?.S,
      }), // Simulate parsing Kinesis object
    },
  });

  beforeEach(() => {
    delete process.env.TTL_OFFSET;
    mockDynamoDBClient.send = async () => ({}); // Reset to simulate success
  });

  it("should handle empty event data", async () => {
    const event = { mockKinesisData: [] };
    const result = await lambda.handleEvent(event);
    expect(result).to.deep.equal({ batchItemFailures: [] });
  });

  it("should handle a single valid Kinesis event", async () => {
    const event = {
      mockKinesisData: [
        {
          dynamodb: {
            NewImage: { iun: { S: "testIUN" }, group: { S: "testGroup" } },
          },
          kinesisSeqNumber: "seq123",
        },
      ],
    };

    const result = await lambda.handleEvent(event);
    expect(result).to.deep.equal({ batchItemFailures: [] });
  });

  it("should handle invalid `iun` or `group` gracefully", async () => {
    const event = {
      mockKinesisData: [
        {
          dynamodb: {
            NewImage: { iun: {}, group: {} }, // Invalid structure
          },
          kinesisSeqNumber: "seq123",
        },
      ],
    };

    const result = await lambda.handleEvent(event);
    expect(result).to.deep.equal({
      batchItemFailures: [{ itemIdentifier: "seq123" }],
    });
  });

  it("should handle DynamoDB put operation failure", async () => {
    const event = {
      mockKinesisData: [
        {
          dynamodb: {
            NewImage: { iun: { S: "testIUN" }, group: { S: "testGroup" } },
          },
          kinesisSeqNumber: "seq123",
        },
      ],
    };

    // Simulate a DynamoDB failure
    mockDynamoDBClient.send = async () => {
      throw new Error("Simulated DynamoDB Error");
    };

    const result = await lambda.handleEvent(event);
    expect(result).to.deep.equal({
      batchItemFailures: [{ itemIdentifier: "seq123" }],
    });
  });

  it("should calculate TTL correctly when TTL_OFFSET is set", async () => {
    process.env.TTL_OFFSET = "3600"; // 1 hour
    const now = Math.floor(Date.now() / 1000);
    const expectedTTL = now + 3600;

    const event = {
      mockKinesisData: [
        {
          dynamodb: {
            NewImage: { iun: { S: "testIUN" }, group: { S: "testGroup" } },
          },
          kinesisSeqNumber: "seq123",
        },
      ],
    };

    mockDynamoDBClient.send = async (command) => {
      const ttl = command.input.Item.ttl.N;
      expect(Number(ttl)).to.be.closeTo(expectedTTL, 10); // Allow a small margin for time difference
    };

    await lambda.handleEvent(event);
  });

  it("should handle invalid TTL_OFFSET gracefully", async () => {
    process.env.TTL_OFFSET = "INVALID";
    const now = Math.floor(Date.now() / 1000);

    const event = {
      mockKinesisData: [
        {
          dynamodb: {
            NewImage: { iun: { S: "testIUN" }, group: { S: "testGroup" } },
          },
          kinesisSeqNumber: "seq123",
        },
      ],
    };

    mockDynamoDBClient.send = async (command) => {
      const ttl = command.input.Item.ttl.N;
      expect(Number(ttl)).to.equal(now);
    };

    await lambda.handleEvent(event);
  });

  it("should handle faulty data gracefully", async () => {
    const event = {
      mockKinesisData: [
        // Case 1: Missing `NewImage` entirely
        {
          dynamodb: {},
          kinesisSeqNumber: "seq123",
        },
        // Case 2: `NewImage` structure missing `iun` or `group`
        {
          dynamodb: {
            NewImage: { someField: { S: "value" } }, // Missing `iun` and `group`
          },
          kinesisSeqNumber: "seq124",
        },
        // Case 3: `NewImage` with invalid data types (e.g., `iun` being an object instead of a string)
        {
          dynamodb: {
            NewImage: { iun: { S: 1234 }, group: { S: "testGroup" } }, // Invalid `iun` type
          },
          kinesisSeqNumber: "seq125",
        },
        // Case 4: `NewImage` with a malformed structure (e.g., missing `S` in `iun`)
        {
          dynamodb: {
            NewImage: { iun: {}, group: { S: "testGroup" } }, // Invalid `iun` value
          },
          kinesisSeqNumber: "seq126",
        },
      ],
    };
  
    mockDynamoDBClient.send = async () => {
      throw new Error("Simulated DynamoDB Error");
    };
  
    const result = await lambda.handleEvent(event);
    expect(result).to.deep.equal({
      batchItemFailures: [
        { itemIdentifier: "seq123" }, // No `NewImage`
        { itemIdentifier: "seq124" }, // Missing `iun` or `group`
        { itemIdentifier: "seq125" }, // Invalid `iun` type
        { itemIdentifier: "seq126" }, // Invalid `iun` value
      ],
    });
  });  
});
