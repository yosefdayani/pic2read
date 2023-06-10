import json
from typing import Dict, Any
from uuid import uuid4
from http import HTTPStatus
from models.db_entry import *
import boto3

IMAGES_BUCKET_NAME = os.environ["IMAGES_BUCKET_NAME"]

s3_client = boto3.client("s3")


def handler(event: Dict[str, Any], _: Any) -> Dict[str, Any]:
    if event['requestContext']['http']['method'] != 'POST':
        return {
            'statusCode': HTTPStatus.BAD_REQUEST
        }
    # process request body
    body_as_json = json.loads(event['body'])
    file_name = body_as_json.get('fileName')
    if not file_name:
        return {
            'statusCode': HTTPStatus.BAD_REQUEST
        }
    gender = 'Male' if body_as_json.get('gender') else 'Female'

    job_id = str(uuid4())
    # generate presigned url to upload image to bucket
    response = s3_client.generate_presigned_url(
        ClientMethod="put_object",
        Params={
            "Bucket": IMAGES_BUCKET_NAME,
            "Key": file_name,
            "Metadata": {'job-id': job_id,
                         'gender': gender},
            'ContentType': "image/jpeg"
        }
    )
    # update DB
    dynamo_entry = DBEntry()
    dynamo_entry.job_id = job_id
    dynamo_entry.status = STATUS_CREATED
    dynamo_entry.save()
    return {
        'statusCode': HTTPStatus.CREATED,
        'headers': {
            "Content-Type": "application/json"
        },
        'body': json.dumps({
            "url": response,
            "jobId": job_id
        })
    }
