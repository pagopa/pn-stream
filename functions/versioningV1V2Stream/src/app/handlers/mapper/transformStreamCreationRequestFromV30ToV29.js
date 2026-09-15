exports.createStreamCreationRequestV29 = (requestBody) => {
    console.log("transformStreamCreationRequestFromV30ToV29");
        requestBody.communicationType = undefined;
        return requestBody;
}