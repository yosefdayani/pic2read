import os

from pynamodb.attributes import UnicodeAttribute
from pynamodb.models import Model

STATUS_CREATED = "created"
STATUS_TEXT_PROCESSING = "text_processing"
STATUS_TEXT_FAILED = "text_failed"
STATUS_AUDIO_PROCESSING = "audio_processing"
STATUS_AUDIO_FAILED = "audio_failed"
LANGUAGE_NOT_SUPPORTED = "language_not_supported"
TEXT_TOO_SHORT = "text_too_short"
STATUS_SUCCESS = "success"


class DBEntry(Model):
    """
    A representation of an image and its processing status.
    When the processing is completed, the DBEntry model should contain the S3 bucket and key
    for both the processed text and audio.
    """
    class Meta:
        region = os.environ["AWS_DEFAULT_REGION"]
        table_name = "huji-lightricks-final-project-result"
    job_id = UnicodeAttribute(hash_key=True)
    status = UnicodeAttribute()
    processed_text_bucket = UnicodeAttribute(null=True)
    processed_text_key = UnicodeAttribute(null=True)
    processed_audio_bucket = UnicodeAttribute(null=True)
    processed_audio_key = UnicodeAttribute(null=True)