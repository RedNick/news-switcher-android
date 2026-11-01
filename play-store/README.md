# Google Play store listing

Everything Google Play shows on the app's store page, kept here so it can be
reviewed and changed like code. When these files change on `main`, the
**Update store listing** workflow uploads them to Play. You can also run it
by hand from the Actions tab.

| What | File |
| --- | --- |
| App name, short description, full description | `listing/en-GB/*.txt` |
| Contact email, website, default language | `store-details.json` |
| Data safety answers | `data-safety.csv` |
| Store icon (512×512) | `graphics/icon.png` |
| Feature graphic (1024×500) | `graphics/feature-graphic.png` |
| Android TV banner (1280×720) | `graphics/tv-banner.png` |
| Phone screenshots | `graphics/phone-screenshots/` |
| TV screenshots | `graphics/tv-screenshots/` |

Limits: name 30 characters, short description 80, full description 4000.
To add another language, add a folder such as `listing/fr-FR/` with the same
three text files.

## Data safety

`data-safety.csv` is Play Console's own Data safety export with the answers
filled in. To change an answer, set the third column (`Response value`) of
its row to `TRUE`/`FALSE`, or clear it. A fresh blank template can be
downloaded from Play Console (Data safety > Export to CSV).

Current answers: the app itself collects nothing, but Google counts data
collected by third-party code running inside an app, and the embedded YouTube
player collects device IDs and viewing activity. So it declares "App
interactions" and "Device or other IDs" as collected (not shared), required,
not processed ephemerally, for app functionality, analytics and advertising.
No accounts, data encrypted in transit, no deletion request option.

## Remaking the graphics

- Icon, feature graphic and TV banner: `python3 play-store/make_graphics.py` (needs Pillow).
- Screenshots: `node play-store/take_screenshots.js <path to the news-switcher web repo>` (needs Playwright).
  This loads the real page but swaps YouTube's player for a plain backdrop,
  because broadcasters' footage can't be used in store screenshots.

## Set by hand in Play Console

Google doesn't let these be set through its API. Under **Policy and programs > App content**, and in **Store settings**:

| Section | What to choose |
| --- | --- |
| Privacy policy | `https://rednick.github.io/news-switcher/privacy.html` |
| Ads | No, the app doesn't contain ads |
| App access | All functionality is available without special access |
| Content rating | Fill in the questionnaire as a news app. It has no user content, chat, purchases or location sharing. It shows live news, which can include real-world violence, so answer that question honestly. Expect a "Teen"/PEGI 12 type rating. |
| Target audience | 18 and over (avoids the extra Families rules) |
| News app | Yes. Then fill in the news declaration. It's an aggregator of broadcasters' official live streams. |
| Government app | No |
| Financial features / Health | None |
| App category (Store settings) | News & Magazines |
| Android TV (Advanced settings > Form factors) | Opt in to Android TV, then submit for TV review |
