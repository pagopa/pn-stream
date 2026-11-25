const axios = require("../lib/httpClient");
const EventHandler  = require('./baseHandler.js');
class DeleteEventStreamHandler extends EventHandler {
    constructor() {
        super();
    }

    checkOwnership(event, context){
        const {path, httpMethod} = event;
        return path.includes('/streams') && event["pathParameters"] !== undefined && event["pathParameters"] != null && httpMethod.toUpperCase() === 'DELETE';
    }

    async handlerEvent(event, context) {
        console.log("Versioning_V1-V2.x_DeleteEventStream_Lambda function started");

        // HEADERS
        let version = this.getVersion(event);
        const headers = this.prepareHeaders(event, version);
        headers["Accept"] = "*/*";

        const streamId = event["pathParameters"]["streamId"];
        const url = `${this.baseUrl}/streams/${streamId}`;

        console.log('calling ', url);
        let response = await axios.delete(url, { headers: headers, timeout: this.attemptTimeout});

        const ret = {
            statusCode: response.status,
            body: JSON.stringify(response.data)
        }
        return ret;
    }
}

module.exports = DeleteEventStreamHandler;