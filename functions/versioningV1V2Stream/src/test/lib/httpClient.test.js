const assert = require("assert");
const { expect } = require("chai");
const axios = require("axios");
const MockAdapter = require("axios-mock-adapter");
const proxyquire = require("proxyquire").noCallThru();

describe("httpClient", () => {
    let client;
    let mock;
    let originalNumRetry;
    let globalRequestInterceptors;
    let globalResponseInterceptors;

    beforeEach(() => {
        originalNumRetry = process.env.NUM_RETRY;
        globalRequestInterceptors = axios.interceptors.request.handlers.filter(Boolean).length;
        globalResponseInterceptors = axios.interceptors.response.handlers.filter(Boolean).length;
        process.env.NUM_RETRY = "2";
        client = proxyquire("../../app/lib/httpClient", {});
        mock = new MockAdapter(client);
    });

    afterEach(() => {
        if (mock) {
            mock.restore();
        }
        if (originalNumRetry === undefined) {
            delete process.env.NUM_RETRY;
        } else {
            process.env.NUM_RETRY = originalNumRetry;
        }
    });

    it("configures one interceptor on a client separate from global Axios", () => {
        expect(client).not.to.equal(axios);
        expect(client.interceptors.request.handlers.filter(Boolean)).to.have.lengthOf(1);
        expect(client.interceptors.response.handlers.filter(Boolean)).to.have.lengthOf(1);
        expect(axios.interceptors.request.handlers.filter(Boolean)).to.have.lengthOf(globalRequestInterceptors);
        expect(axios.interceptors.response.handlers.filter(Boolean)).to.have.lengthOf(globalResponseInterceptors);
    });

    it("retries an idempotent request and resets the timeout for each attempt", async () => {
        mock.onGet("/streams").replyOnce(503, {}, {});
        mock.onGet("/streams").replyOnce(503, {}, {});
        mock.onGet("/streams").reply(200, { streams: [] });

        const response = await client.get("/streams", { timeout: 5000 });

        expect(response.status).to.equal(200);
        expect(response.data).to.deep.equal({ streams: [] });
        expect(mock.history.get).to.have.lengthOf(3);
        expect(mock.history.get.map((request) => request.timeout)).to.deep.equal([5000, 5000, 5000]);
    });

    it("propagates the error after the configured retry limit", async () => {
        mock.onGet("/streams").reply(503, { message: "unavailable" }, {});

        await assert.rejects(
            () => client.get("/streams"),
            (error) => {
                expect(error.response.status).to.equal(503);
                expect(error.response.data).to.deep.equal({ message: "unavailable" });
                return true;
            }
        );
        expect(mock.history.get).to.have.lengthOf(3);
    });

    it("does not retry a non-retryable response", async () => {
        mock.onGet("/streams").reply(400);

        await assert.rejects(() => client.get("/streams"), (error) => error.response.status === 400);
        expect(mock.history.get).to.have.lengthOf(1);
    });

    it("does not retry a POST on a server response", async () => {
        mock.onPost("/streams").reply(503, {}, {});

        await assert.rejects(() => client.post("/streams", {}), (error) => error.response.status === 503);
        expect(mock.history.post).to.have.lengthOf(1);
    });

    it("retries a POST after a timeout", async () => {
        mock.onPost("/streams").timeoutOnce();
        mock.onPost("/streams").reply(201, { streamId: "123" });

        const response = await client.post("/streams", {}, { timeout: 5000 });

        expect(response.status).to.equal(201);
        expect(mock.history.post).to.have.lengthOf(2);
        expect(mock.history.post.map((request) => request.timeout)).to.deep.equal([5000, 5000]);
    });

    it("retries a POST after a network error", async () => {
        mock.onPost("/streams").replyOnce((config) => {
            const error = new Error("connection reset");
            error.code = "ECONNRESET";
            error.config = config;
            return Promise.reject(error);
        });
        mock.onPost("/streams").reply(201);

        const response = await client.post("/streams", {});

        expect(response.status).to.equal(201);
        expect(mock.history.post).to.have.lengthOf(2);
    });

    it("honors a zero retry limit", async () => {
        mock.restore();
        process.env.NUM_RETRY = "0";
        client = proxyquire("../../app/lib/httpClient", {});
        mock = new MockAdapter(client);
        mock.onGet("/streams").reply(503, {}, {});

        await assert.rejects(() => client.get("/streams"), (error) => error.response.status === 503);
        expect(mock.history.get).to.have.lengthOf(1);
    });

    it("retries through the handler without accumulating interceptors across invocations", async () => {
        const GetEventStreamHandler = proxyquire("../../app/handlers/getEventStreamHandler", {
            "../lib/httpClient": client
        });
        const event = {
            path: "/delivery-progresses/v2.9/streams/123",
            pathParameters: { streamId: "123" },
            httpMethod: "GET",
            headers: {},
            requestContext: { authorizer: {} }
        };
        const url = "http://example.test/streams/123";
        mock.onGet(url).replyOnce(503, {}, {});
        mock.onGet(url).reply(200, { streamId: "123" });

        const invoke = async () => {
            const handler = new GetEventStreamHandler();
            handler.baseUrl = "http://example.test";
            handler.attemptTimeout = 5000;
            return handler.handlerEvent(event, {});
        };

        const firstResponse = await invoke();
        const secondResponse = await invoke();

        expect(firstResponse.statusCode).to.equal(200);
        expect(secondResponse.statusCode).to.equal(200);
        expect(mock.history.get).to.have.lengthOf(3);
        expect(client.interceptors.request.handlers.filter(Boolean)).to.have.lengthOf(1);
        expect(client.interceptors.response.handlers.filter(Boolean)).to.have.lengthOf(1);
    });
});
