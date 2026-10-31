"""Uploads the store listing in this folder to Google Play.

Sets the app's contact details, the listing text for each language folder in
listing/, and replaces the graphics with the ones in graphics/. Everything goes
into one Play "edit", which is committed at the end, so a failure part way
through changes nothing.

Usage (normally run by .github/workflows/update-store-listing.yml):
    PLAY_SERVICE_ACCOUNT_JSON='{...}' python3 play-store/update_play_listing.py
"""

import json
import os
from pathlib import Path

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

PACKAGE_NAME = "uk.rdfn.newsswitcher"
STORE_FOLDER = Path(__file__).parent
LISTING_FOLDER = STORE_FOLDER / "listing"
GRAPHICS_FOLDER = STORE_FOLDER / "graphics"

# Play's name for each kind of image, and where ours live. A folder means
# "all the PNGs in it, in file name order".
IMAGES_BY_PLAY_TYPE = {
    "icon": GRAPHICS_FOLDER / "icon.png",
    "featureGraphic": GRAPHICS_FOLDER / "feature-graphic.png",
    "tvBanner": GRAPHICS_FOLDER / "tv-banner.png",
    "phoneScreenshots": GRAPHICS_FOLDER / "phone-screenshots",
    "tvScreenshots": GRAPHICS_FOLDER / "tv-screenshots",
}


def connect_to_play():
    credentials = service_account.Credentials.from_service_account_info(
        json.loads(os.environ["PLAY_SERVICE_ACCOUNT_JSON"]),
        scopes=["https://www.googleapis.com/auth/androidpublisher"],
    )
    return build("androidpublisher", "v3", credentials=credentials, cache_discovery=False)


def read_text(language_folder, file_name):
    return (language_folder / file_name).read_text(encoding="utf-8").strip()


def update_listing_text(edits, edit_id, language_folder):
    language = language_folder.name
    listing = {
        "language": language,
        "title": read_text(language_folder, "title.txt"),
        "shortDescription": read_text(language_folder, "short-description.txt"),
        "fullDescription": read_text(language_folder, "full-description.txt"),
    }
    edits.listings().update(
        packageName=PACKAGE_NAME, editId=edit_id, language=language, body=listing
    ).execute()
    print(f"Listing text set for {language}")


def replace_images(edits, edit_id, language):
    for image_type, image_location in IMAGES_BY_PLAY_TYPE.items():
        if image_location.is_dir():
            image_files = sorted(image_location.glob("*.png"))
        else:
            image_files = [image_location]

        # Clear the old ones first, so removed screenshots disappear too.
        edits.images().deleteall(
            packageName=PACKAGE_NAME, editId=edit_id, language=language, imageType=image_type
        ).execute()
        for image_file in image_files:
            edits.images().upload(
                packageName=PACKAGE_NAME,
                editId=edit_id,
                language=language,
                imageType=image_type,
                media_body=MediaFileUpload(str(image_file), mimetype="image/png"),
            ).execute()
        print(f"{image_type}: uploaded {len(image_files)} image(s) for {language}")


def main():
    play = connect_to_play()
    edits = play.edits()
    edit_id = edits.insert(packageName=PACKAGE_NAME, body={}).execute()["id"]

    language_folders = sorted(folder for folder in LISTING_FOLDER.iterdir() if folder.is_dir())
    for language_folder in language_folders:
        update_listing_text(edits, edit_id, language_folder)
        replace_images(edits, edit_id, language_folder.name)

    # Done after the listings, because the default language must already
    # have a listing.
    store_details = json.loads((STORE_FOLDER / "store-details.json").read_text(encoding="utf-8"))
    edits.details().update(packageName=PACKAGE_NAME, editId=edit_id, body=store_details).execute()
    print("Contact details and default language set")

    edits.commit(packageName=PACKAGE_NAME, editId=edit_id).execute()
    print("Committed to Google Play")


if __name__ == "__main__":
    main()
