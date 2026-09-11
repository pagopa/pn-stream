exports.createStreamMetadataResponseV29 = (responseBody) => {
    console.log("transformStreamMetadataResponseFromV30ToV29");
    responseBody.communicationType = undefined;
    return responseBody;
}