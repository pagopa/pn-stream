import { parseArgs } from 'util';
import { AwsClientsWrapper } from "pn-common";
import { UpdateItemCommand, ScanCommand  } from "@aws-sdk/client-dynamodb";
import { readFileSync } from 'fs';


const VALID_ENVIRONMENTS = ['dev', 'uat', 'test', 'prod', 'hotfix'];
const DEFAULT_TABLE = "pn-WebhookStreams1";
/**
 * Validates command line arguments
 * @returns {Object} Parsed and validated arguments
 */
function validateArgs() {
    const usage = `
Usage: node index.js --envName|-e <ambiente> --filename|-f filename.json --inputDate|-d <data> --tableName|-t <streamTable>--help|-h

Description:
    Questo script permette di aggiornare il flag di sorting e la data di attivazione degli stream passati in input.

Parameters:
    --envName, -e       Required. Environment to update (dev|uat|test|prod|hotfix)
    --filename, -f      File con i record da aggiornare
    --inputDate, -d     Data di attivazione degli stream, obbligatoriamente nel formato UTC
    -tableName, -t      Nome della tabella degli stream
    --help, -h          Display this help message`;

    const args = parseArgs({
        options: {
            envName: { type: "string", short: "e" },
            filename: {type: "string", short: "f"},
            inputDate: {type: "string", short: "d"},
            tableName: {type: "string", short: "t"},
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

    if(!args.values.inputDate) {
        console.error("Error: Missing required parameters inputDate");
        console.log(usage);
        process.exit(1);
    }

    if(!args.values.filename) {
        console.error("Error: Missing required parameters filename");
        console.log(usage);
        process.exit(1);
    }

    if(!args.values.tableName) {
        console.info("Using default Table");
        args.values.tableName = DEFAULT_TABLE;
        
    }
    console.log("updated stream table : ", args.values.tableName)
    console.log("inputDate : ", args.values.inputDate)

    if(isNaN(new Date(args.values.inputDate))) {
        console.error("Error: the inputDate is not a valid date");
        console.log(usage);
        process.exit(1);
    }

    if(new Date(args.values.inputDate) < new Date()) {
        console.error("Error: the inputDate must be greater than the current day");
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
    const { envName, filename, inputDate, tableName } = args;

    // Initialize AWS client
    const coreClient = new AwsClientsWrapper('core', envName);
    await testSsoCredentials(coreClient);
    coreClient._initDynamoDB();

    let streamIDs = JSON.parse(readFileSync(filename, 'utf8'));

    const params = {
        TableName: tableName,
        ExclusiveStartKey: null
    };
    const streams = [];
    let items;

    const scan = new ScanCommand(params);
    do {
        items = await coreClient._dynamoClient.send(scan);
        items.Items.forEach((item) => streams.push(item));
        params.ExclusiveStartKey = items.LastEvaluatedKey;
    } while (typeof items.LastEvaluatedKey !== "undefined");

    const stats = {
        total: streamIDs.length,
        updated: 0,
        failed: 0
    };

    // Process each record
    for (const record of streamIDs) {
        try {

            if (!record.hasOwnProperty("streamId")) {
                console.log("skipping wrong record: ",record)
                continue;
            }

            console.log("stream: ", record.streamId)

            let stream = streams.find(stream => stream.sortKey.S == record.streamId)

            if (!stream) {
                console.log(`stream ${record.streamId} not found on table ${tableName}`);
                continue;
            }


            const command = new UpdateItemCommand({
                TableName: tableName,
                Key: {
                  hashKey: {S: stream.hashKey.S},
                  sortKey: {S: record.streamId}
                },
                UpdateExpression: "set sorting = :sorting, originalActivationDate = activationDate, activationDate = :activationDate",
                ExpressionAttributeValues: {
                  ":sorting": {BOOL: true},
                  ":activationDate": {S: inputDate},
                }
              });
            
            await coreClient._dynamoClient.send(command);

            console.log(`Migrated succeded for stream: ${record.streamId} on table ${tableName}`);
            stats.updated++;
        } catch (error) {
            console.error(`Error processing ${record.streamId}:`, error);
            stats.failed++;
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