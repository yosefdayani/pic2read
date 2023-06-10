
from typing import Dict, Any

import boto3
import botocore.exceptions as botoExceptions

from models.db_entry import *

TEXT_BUCKET_NAME = os.environ["TEXT_BUCKET_NAME"]

s3_client = boto3.client("s3")
texTract_client = boto3.client("textract")


def handler(event: Dict[str, Any], _: Any):
    for record in event['Records']:
        # to access head object for job-id
        bucket = record['s3']['bucket']['name']
        key = record['s3']['object']['key']
        head_response = s3_client.head_object(Bucket=bucket, Key=key)
        # get job id and gender from head response
        gender = head_response["Metadata"]["gender"]
        job_id = head_response["Metadata"]["job-id"]
        # update dynamoDB with job id
        dynamo_entry = DBEntry.get(job_id)
        dynamo_entry.update(actions=[DBEntry.status.set(STATUS_TEXT_PROCESSING)])

        # detect text
        try:
            texTract_response = texTract_client.detect_document_text(
                Document={'S3Object': {'Bucket': bucket, 'Name': key}})
        except (botoExceptions.BotoCoreError, botoExceptions.ClientError, botoExceptions.UndefinedModelAttributeError):
            dynamo_entry.update(actions=[DBEntry.status.set(STATUS_TEXT_FAILED)])
            return
        text = ""
        for item in texTract_response["Blocks"]:
            if item["BlockType"] == "LINE":
                text += item["Text"] + "\n"
        text_key = key + ".txt"
        # put processed text in bucket
        s3_client.put_object(Body=text,
                             Bucket=TEXT_BUCKET_NAME,
                             Key=text_key,
                             Metadata={'job-id': job_id,
                                       'gender': gender},
                             ContentType="text/plain")

        # update DB
        dynamo_entry.update(actions=[DBEntry.processed_text_bucket.set(TEXT_BUCKET_NAME),
                                     DBEntry.processed_text_key.set(text_key),
                                     DBEntry.status.set(STATUS_AUDIO_PROCESSING)])

