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
    --table-name WebhookStreams  \
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
    --table-name WebhookEvents  \
    --attribute-definitions \
        AttributeName=hashKey,AttributeType=S \
        AttributeName=sortKey,AttributeType=S \
    --key-schema \
        AttributeName=hashKey,KeyType=HASH \
        AttributeName=sortKey,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5


echo "Initialization terminated"
