from typing import Dict, Any

import boto3
import botocore.exceptions as botoExceptions

from models.db_entry import *

TEXT_BUCKET_NAME = os.environ["TEXT_BUCKET_NAME"]
AUDIO_BUCKET_NAME = os.environ["AUDIO_BUCKET_NAME"]

s3_client = boto3.client("s3")
comprehend_client = boto3.client("comprehend")
polly_client = boto3.client("polly")
comprehend_to_polly = {'en': 'en-US', 'fr': 'fr-FR', 'de': 'de-DE', 'it': 'it-IT', 'es': 'es-ES', 'pt': 'pt-PT'}


def handler(event: Dict[str, Any], _: Any):
    for record in event['Records']:
        # to access head object for job-id
        bucket = record['s3']['bucket']['name']
        key = record['s3']['object']['key']
        head_response = s3_client.head_object(Bucket=bucket, Key=key)
        # get job id and gender from head response
        job_id = head_response["Metadata"]["job-id"]
        gender = head_response["Metadata"]["gender"]

        # get dynamoDB entry with job id
        dynamo_entry = DBEntry.get(job_id)
        # get processed text from bucket
        file_content = s3_client.get_object(
            Bucket=dynamo_entry.processed_text_bucket, Key=dynamo_entry.processed_text_key)["Body"]
        text = file_content.read().decode('utf-8')
        if len(text) < 20:  # text under 20 characters cannot be converted to audio by polly
            dynamo_entry.update(actions=[DBEntry.status.set(TEXT_TOO_SHORT)])
            return
        # detect language
        try:
            comprehend_response = comprehend_client.detect_dominant_language(
                Text=text)
        except (botoExceptions.BotoCoreError, botoExceptions.ClientError, botoExceptions.UndefinedModelAttributeError):
            dynamo_entry.update(actions=[DBEntry.status.set(STATUS_AUDIO_FAILED)])
            return
        language = comprehend_response['Languages'][0]['LanguageCode']
        # given language is not supported
        if language not in comprehend_to_polly.keys():
            dynamo_entry.update(actions=[DBEntry.status.set(LANGUAGE_NOT_SUPPORTED)])
            return

        voice_id = get_voice_id(gender, language)
        # process audio
        try:
            speech = polly_client.synthesize_speech(
                OutputFormat='mp3',
                Text=text[:1500],
                VoiceId=voice_id
            )
        except (botoExceptions.BotoCoreError, botoExceptions.ClientError, botoExceptions.UndefinedModelAttributeError):
            dynamo_entry.update(actions=[DBEntry.status.set(STATUS_AUDIO_FAILED)])
            return
        # upload audio to bucket
        audio = speech['AudioStream'].read()
        audio_key = key + ".mp3"
        s3_client.put_object(Body=audio, Bucket=AUDIO_BUCKET_NAME, Key=audio_key)
        # update DB
        dynamo_entry.update(actions=[DBEntry.processed_audio_bucket.set(AUDIO_BUCKET_NAME),
                                     DBEntry.processed_audio_key.set(audio_key),
                                     DBEntry.status.set(STATUS_SUCCESS)])


def get_voice_id(gender, language_code):
    """
    retrieves voice id from polly's voices according to client's choice of voice gender
    :param gender: Male or Female
    :param language_code: in comprehend code
    :return: voice ID
    """
    language_id = comprehend_to_polly[language_code]
    language_voices = polly_client.describe_voices(
        Engine='standard',
        LanguageCode=language_id,
        IncludeAdditionalLanguageCodes=False
    )['Voices']
    for voice in language_voices:
        if voice['Gender'] == gender:
            return voice['Id']