exports.createStreamRequestV30 = (requestBody) => {
    console.log("transformStreamRequestFromV29ToV30");
    requestBody.communicationType = undefined;
    return requestBody;
}