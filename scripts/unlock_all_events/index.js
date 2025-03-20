import { parseArgs } from 'util';
import { AwsClientsWrapper } from "pn-common";
import { ScanCommand } from "@aws-sdk/client-dynamodb";


const VALID_ENVIRONMENTS = ['dev', 'uat', 'test', 'prod', 'hotfix'];
const QUEUE_URL = "pn-stream_schedule";

/**
 * Validates command line arguments
 * @returns {Object} Parsed and validated arguments
 */
function validateArgs() {
    const usage = `
Usage: node index.js --envName|-e <environment>

Description:
    Questo script permette di sbloccare tutti gli elementi associati agli stream con sorting abilitato dalla tabella DynamoDB pn-WebhookEventsQuarantine.

Parameters:
    --envName, -e     Required. Environment to update (dev|uat|test|prod|hotfix)
    --help, -h        Display this help message`;

    const args = parseArgs({
        options: {
            envName: { type: "string", short: "e" },
            help: { type: "boolean", short: "h" }
        },
        strict: true
    });

    if (args.values.help) {
        console.log(usage);
        process.exit(0);
    }

    if (!args.values.envName) {
        console.error("Error: Missing required parameters");
        console.log(usage);
        process.exit(1);
    }

    if (!VALID_ENVIRONMENTS.includes(args.values.envName)) {
        console.error(`Error: Invalid environment. Must be one of: ${VALID_ENVIRONMENTS.join(', ')}`);
        process.exit(1);
    }

    return args.values;
}

/**
 * Tests SSO credentials for AWS client
 * @param {AwsClientsWrapper} awsClient - AWS client wrapper
 */
async function testSsoCredentials(awsClient) {
    try {
        awsClient._initSTS();
        await awsClient._getCallerIdentity();
    } catch (error) {
        if (error.name === 'CredentialsProviderError' ||
            error.message?.includes('expired') ||
            error.message?.includes('credentials')) {
            console.error('\n=== SSO Authentication Error ===');
            console.error('Your SSO session has expired or is invalid.');
            console.error('Please run the following commands:');
            console.error('1. aws sso logout');
            console.error(`2. aws sso login --profile ${awsClient.ssoProfile}`);
            process.exit(1);
        }
        throw error;
    }
}

/**
 * Prints execution summary
 * @param {Object} stats - Execution statistics
 */
function printSummary(stats) {
    console.log('\n=== Execution Summary ===');
    console.log(`\nTotal items processed: ${stats.total}`);
    console.log(`\nItems updated: ${stats.updated}`);
    console.log(`Failed updates: ${stats.failed}`);
}

/**
 * Main execution function
 */
async function main() {
    const args = validateArgs();
    const { envName } = args;

    // Initialize AWS client
    const coreClient = new AwsClientsWrapper('core', envName);
    await testSsoCredentials(coreClient);
    coreClient._initDynamoDB();
    coreClient._initSQS();

    const params = {
        TableName: 'pn-WebhookStreams',
        ExclusiveStartKey: null
    };

    const records = [];
    let items;
    const scan = new ScanCommand(params);
    do {
        items = await coreClient._dynamoClient.send(scan);
        items.Items.forEach((item) => records.push(item));
        params.ExclusiveStartKey = items.LastEvaluatedKey;
    } while (typeof items.LastEvaluatedKey !== "undefined");


    const stats = {
        total: records.length,
        updated: 0,
        failed: 0
    };

    const queueUrl = await coreClient._getQueueUrl(QUEUE_URL);

    // Process each record
    for (const record of records) {
        try {
            if(!record.sorting || !record.sorting.BOOL) {
                continue;
            }

            let eventBody = {
                "eventKey" : record.sortKey.S
            }
            let messageAttributes = {
                eventType: {
                    DataType: "String",
                    StringValue: "UNLOCK_ALL_EVENTS",
                  },
            }
            await coreClient._sendSQSMessage(queueUrl, eventBody, 0, messageAttributes)

            console.log(`Sended message UNLOCK_ALL_EVENTS for stream:  ${record.hashKey}/${record.sortKey}`);
            stats.updated++;
        } catch (error) {
            if (error.name === 'ConditionalCheckFailedException') {
                console.log(`Skipping ${record.hashKey.S}/${record.sortKey.S} - version already exists`);
                stats.skipped++;
            } else {
                console.error(`Error processing ${record.hashKey}/${record.sortKey}:`, error);
                stats.failed++;
            }
        }
    }

    // Print summary
    printSummary(stats);
}

// Start execution with error handling
main().catch(err => {
    console.error('Script failed:', err);
    process.exit(1);
});