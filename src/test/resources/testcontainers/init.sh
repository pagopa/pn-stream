## Quando viene aggiornato questo file, aggiornare anche il commitId presente nel file initsh-for-testcontainer-sh

echo "### CREATE QUEUES ###"

queues="pn-stream_actions pn-delivery_push_to_stream"
for qn in  $( echo $queues | tr " " "\n" ) ; do

    echo creating queue $qn ...

    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2"}' \
        --queue-name $qn


done

echo " - Create pn-stream TABLES"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-WebhookStreams  \
    --attribute-definitions \
        AttributeName=hashKey,AttributeType=S \
        AttributeName=sortKey,AttributeType=S \
    --key-schema \
        AttributeName=hashKey,KeyType=HASH \
        AttributeName=sortKey,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-StreamStats  \
    --attribute-definitions \
        AttributeName=pk,AttributeType=S \
        AttributeName=sk,AttributeType=S \
    --key-schema \
        AttributeName=pk,KeyType=HASH \
        AttributeName=sk,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-WebhookEvents  \
    --attribute-definitions \
        AttributeName=hashKey,AttributeType=S \
        AttributeName=sortKey,AttributeType=S \
    --key-schema \
        AttributeName=hashKey,KeyType=HASH \
        AttributeName=sortKey,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-streamNotification  \
    --attribute-definitions \
        AttributeName=hashKey,AttributeType=S \
    --key-schema \
        AttributeName=hashKey,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

echo " - Create PARAMETERS"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
	ssm put-parameter \
	--name "/pn-stream/retry/b19920b0-ec40-4b56-80c0-0e06998b37e5" \
	--value "{\"retryAfter\": \"3000\"}"\
	--type String \

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
	ssm put-parameter \
	--name "/pn-stream/stats/custom-ttl" \
	--value "{\"config\":{\"NUMBER_OF_REQUESTS\":{\"ttl\":\"10d\",\"spanUnit\":\"1\",\"timeUnit\":\"HOURS\"},
	\"RETRY_AFTER_VIOLATION\":{\"ttl\":\"20d\",\"spanUnit\":\"2\",\"timeUnit\":\"HOURS\"},
	\"NUMBER_OF_READINGS\":{\"ttl\":\"30d\",\"spanUnit\":\"3\",\"timeUnit\":\"HOURS\"},
	\"NUMBER_OF_WRITINGS\":{\"ttl\":\"40d\",\"spanUnit\":\"4\",\"timeUnit\":\"HOURS\"},
	\"NUMBER_OF_EMPTY_READINGS\":{\"ttl\":\"50d\",\"spanUnit\":\"5\",\"timeUnit\":\"HOURS\"}}}"\
	--type String \


echo "Initialization terminated"


