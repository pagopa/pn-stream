const axios = require("axios");
const axiosRetry = require("axios-retry").default;

const httpClient = axios.create();

axiosRetry(httpClient, {
    retries: process.env.NUM_RETRY,
    shouldResetTimeout: true,
    retryCondition: (error) => {
        return axiosRetry.isNetworkOrIdempotentRequestError(error)
            || error.code === "ECONNABORTED";
    },
    onRetry: (retryCount, error) => {
        console.warn(`Retry num ${retryCount} - error:${error.message}`);
    },
    onMaxRetryTimesExceeded: (error, retryCount) => {
        console.warn(`Retries exceeded: ${retryCount} - error:${error.message}`);
    }
});

module.exports = httpClient;