const { Buffer } = require("node:buffer");
const { gunzipSync } = require("node:zlib");

function myGunzip(buffer) {
  return gunzipSync(buffer);
}

function decodePayload(b64Str) {
  const payloadBuf = Buffer.from(b64Str, "base64");

  let parsedJson;
  try {
    parsedJson = JSON.parse(payloadBuf.toString("utf8"));
  } catch (err) {
    const uncompressedBuf = myGunzip(payloadBuf);
    parsedJson = JSON.parse(uncompressedBuf.toString("utf8"));
  }

  return parsedJson;
}

exports.extractKinesisData = function (kinesisEvent) {
  return kinesisEvent.Records.map((rec) => {
    const decodedPayload = decodePayload(rec.kinesis.data);
    return {
      kinesisSeqNumber: rec.kinesis.sequenceNumber,
      ...decodedPayload,
    };
  }).filter((item) => {
    const communicationType = item.dynamodb?.NewImage?.communicationType;
    if (!communicationType) {
      return true;
    }

    if (typeof communicationType.S === "string") {
      return communicationType.S.trim().toUpperCase() === "INFORMAL";
    }

    return false;
  });
};
