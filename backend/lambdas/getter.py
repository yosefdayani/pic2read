import json
from typing import Dict, Any
from http import HTTPStatus
import boto3

from models.db_entry import *

s3_client = boto3.client('s3')


def handler(event: Dict[str, Any], _: Any) -> Dict[str, Any]:
    if event['requestContext']['http']['method'] != 'GET':
        return {
            'statusCode': HTTPStatus.BAD_REQUEST
        }
    # get job_id and retrieve DB entry
    job_id = event["queryStringParameters"]['jobId']
    try:
        dynamo_entry = DBEntry.get(job_id)
    except DBEntry.DoesNotExist:
        return {
            'statusCode': HTTPStatus.NOT_FOUND,
            'body': {}
        }

    status = dynamo_entry.status
    processed_text = ""
    # get processed text only if was already processed
    if status in [STATUS_AUDIO_PROCESSING, STATUS_AUDIO_FAILED, STATUS_SUCCESS, TEXT_TOO_SHORT, LANGUAGE_NOT_SUPPORTED]:
        file_content = s3_client.get_object(
            Bucket=dynamo_entry.processed_text_bucket, Key=dynamo_entry.processed_text_key)["Body"]
        processed_text = file_content.read().decode('utf-8')
    url_audio = ""
    # get processed audio presigned url only if was already processed
    if status == STATUS_SUCCESS:
        url_audio = s3_client.generate_presigned_url(
            ClientMethod='get_object',
            Params={
                'Bucket': dynamo_entry.processed_audio_bucket,
                'Key': dynamo_entry.processed_audio_key
            },
        )

    return {
        'statusCode': HTTPStatus.OK,
        'body': json.dumps({
            "processedText": processed_text,
            "urlAudio": url_audio,
            "status": status
        })
    }
