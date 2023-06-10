import time
import requests

from constants import *


def test_image_uploader_success():
    """
    test image uploader lambda for a valid request
    :return:
    """
    r = requests.post(BASE_UPLOAD_URL, json={'fileName': FILE_NAME, 'gender': False})
    assert r.status_code == 201


def test_image_uploader_no_filename_failure():
    """
    test image uploader lambda for a an invalid request
    :return:
    """
    r = requests.post(BASE_UPLOAD_URL, json={'gender': False})
    assert r.status_code == 400


def test_upload_url():
    """
    test for uploading image using the given presigned url
    :return:
    """
    r = requests.post(BASE_UPLOAD_URL, json={'fileName': FILE_NAME, 'gender': False})
    presigned_url = r.json()['url']
    with open(IMG_ENGLISH_GALLERY, 'rb') as data:
        r = requests.put(url=presigned_url, data=data.read(), headers={'content-type': 'image/jpeg'})
    assert r.status_code == 200


def check_processing_on_image(file_name, empty_text=False):
    """
    helper for testing: this function uploads the given file_name and requests get until received processed data or
    timeout occurred
    :param empty_text: True if the given file_name contains no text
    :param file_name:
    :return: status code and non-empty result if succeeded
    """
    r = requests.post(BASE_UPLOAD_URL, json={'fileName': FILE_NAME, 'gender': False})
    job_id = r.json()['jobId']
    presigned_url = r.json()['url']
    with open(file_name, 'rb') as data:
        requests.put(url=presigned_url, data=data.read(), headers={'content-type': 'image/jpeg'})
    for i in range(30):
        time.sleep(0.5)
        r = requests.get(url=BASE_PROCESSED_DATA_URL, params={'jobId': job_id})
        if r.status_code != 200:
            return r.status_code, dict()
        result = r.json()
        status = result['status']
        if status == STATUS_TEXT_PROCESSING:  # check text and audio not returned
            assert not result['processedText']
            assert not result['urlAudio']
        if status == STATUS_AUDIO_PROCESSING:  # check processed text returned
            if not empty_text:  # if the image contains no text, the processed text should be always empty
                assert result['processedText']
            assert not result['urlAudio']
        # if it is in a final processing phase (success or failure)
        if status not in (STATUS_CREATED, STATUS_TEXT_PROCESSING, STATUS_AUDIO_PROCESSING):
            return r.status_code, result
    assert False  # timeout exceeded for getting result


def test_valid_languages_success():
    """
    tests all valid languages: English, Spanish, French, Portuguese, Italian and German
    :return:
    """
    for img in VALID_IMG:
        status_code, result = check_processing_on_image(img)
        assert status_code == 200
        assert result
        assert result['status'] == STATUS_SUCCESS
        assert result['processedText']
        assert result['urlAudio']


def test_invalid_language_only_text_received():
    """
    tests the getter lambda for a non-supported language
    :return:
    """
    status_code, result = check_processing_on_image(IMG_SWEDISH)
    assert status_code == 200
    assert result
    assert result['status'] == STATUS_LANGUAGE_NOT_SUPPORTED
    assert result['processedText']
    assert not result['urlAudio']


def test_text_too_short_only_text_received():
    """
    tests the getter lambda for text less than 20 characters
    the expected result is processed text and an empty audio url
    :return:
    """
    status_code, result = check_processing_on_image(IMG_SHORT_TEXT)
    assert status_code == 200
    assert result
    assert result['status'] == STATUS_TEXT_TOO_SHORT
    assert result['processedText']
    assert not result['urlAudio']


def test_long_text_success():
    """
        tests the getter lambda for text longer than 1500 characters
        :return:
        """
    status_code, result = check_processing_on_image(IMG_LONG_TEXT)
    assert status_code == 200
    assert result
    assert result['status'] == STATUS_SUCCESS
    assert result['processedText']
    assert result['urlAudio']


def test_no_text_empty_data_received():
    """
    tests the server for a picture that contains no text, the expected result is an empty text and no audio url
    :return:
    """
    status_code, result = check_processing_on_image(IMG_NO_TEXT, True)
    assert status_code == 200
    assert result
    assert result['status'] == STATUS_TEXT_TOO_SHORT
    assert not result['processedText']
    assert not result['urlAudio']


def test_bad_job_id_not_found():
    """
    tests the server for a get request with a bad job id
    :return:
    """
    r = requests.get(url=BASE_PROCESSED_DATA_URL, params={'jobId': BAD_JOB_ID})
    assert r.status_code == 404
