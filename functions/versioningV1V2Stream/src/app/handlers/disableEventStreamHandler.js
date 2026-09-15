const axios = require("axios");
const axiosRetry = require("axios-retry").default;
const EventHandler  = require('./baseHandler.js');
const { createStreamMetadataResponseV26 } = require("./mapper/transformStreamMetadataResponseFromV27ToV26");
const { createStreamMetadataResponseV29 } = require("./mapper/transformStreamMetadataResponseFromV30ToV29");
class DisableEventStreamHandler extends EventHandler {
    constructor() {
        super();
    }

    checkOwnership(event, context){
        const {path, httpMethod} = event;
        return path.includes('/streams') && path.includes('/action/disable') && event["pathParameters"] !== undefined && event["pathParameters"] != null && httpMethod.toUpperCase() === 'POST';
    }

    async handlerEvent(event, context) {
        console.log("Versioning_V1-V2.x_DisableEventStream_Lambda function started");

        // HEADERS
        let version = this.getVersion(event);
        
        if(version == 10){
            console.log("Disable operation not implemented for version ", version);
            throw new Error("NOT IMPLEMENTED")
        }

        const headers = this.prepareHeaders(event, version);

        // REQUEST BODY
        const requestBodyV24 = {};

        const streamId = event["pathParameters"]["streamId"];
        const url = `${this.baseUrl}/streams/${streamId}/action/disable`;
        axiosRetry(axios, {
            retries: this.numRetry,
            shouldResetTimeout: true ,
            retryCondition: (error) => {
              return axiosRetry.isNetworkOrIdempotentRequestError(error) || error.code === 'ECONNABORTED';
            },
            onRetry: (retryCount, error, requestConfig) => {
                console.warn(`Retry num ${retryCount} - error:${error.message}`);
            },
            onMaxRetryTimesExceeded: (error, retryCount) => {
                console.warn(`Retries exceeded: ${retryCount} - error:${error.message}`);
            }
        });

        console.log('calling ', url);
        let response = await axios.post(url, requestBodyV24, {headers: headers, timeout: this.attemptTimeout});

        // RESPONSE BODY
        let transformedObject;
        
        switch(version) {
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
                transformedObject = createStreamMetadataResponseV26(createStreamMetadataResponseV29(response.data));
            break;
            case 29:
                transformedObject = createStreamMetadataResponseV29(response.data);
            break;
            default:
                console.error('Invalid version ', version)
            break;
        }

        const ret = {
            statusCode: response.status,
            body: JSON.stringify(transformedObject),
        }
        return ret;
    }
}

module.exports = DisableEventStreamHandler;