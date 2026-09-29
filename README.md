# Virtual Guide v0.1 — first Android test

A separate app from Taxi Company Manager. This first version implements the confirmed idea: use a visitor's location to explain nearby places.

## Try it

1. Install Virtual-Guide-v0.1-test.apk. No account or API key is required.
2. Tap Find nearby and allow location while using the app. Turn on phone Location if prompted. An outdoor fix may be needed.
3. Choose a 2, 5, or 10 km search radius. English and Albanian Wikipedia editions are available.
4. Open a nearby story, read it, or tap Listen. Stop ends narration. Narration also stops when you leave the app.
5. Save a loaded story for offline reading. Offline listening depends on an offline voice being installed in Android's text-to-speech settings.
6. Open map directions to hand the place's coordinates to your maps app.

The app uses foreground location updates only; it stops requesting updates when it leaves the foreground. After moving roughly 500 metres, it refreshes nearby results at most once per minute while the Nearby screen is active. It never narrates automatically. Cached nearby results are labelled as saved until refreshed. Distances are straight-line distances, not walking routes.

Location coordinates, rounded to four decimal places, are sent over HTTPS to the chosen Wikipedia edition when you request nearby places. Stories and bookmarks are stored on the device. The installed Android voice engine handles speech; that engine may use the network depending on its voice settings. There is no custom tracking backend and no connection to the Taxi Manager database.

## Content

Stories are article introductions supplied by Wikipedia, not generated historical claims. Each story links to its original article/contributors and the CC BY-SA 4.0 license. Nearby results include geotagged articles and may include places of limited tourism interest. Coverage depends on the area and Wikipedia language edition; try English or a wider radius if Albanian has fewer results.

Technical references: [MediaWiki Geosearch](https://www.mediawiki.org/wiki/API:Geosearch) and [TextExtracts](https://www.mediawiki.org/wiki/Extension:TextExtracts#API).

## Build and test

Java 17, Android SDK 35, Gradle 8.9. Run `gradle testReleaseUnitTest assembleRelease`. A GitHub Actions workflow is included. The bundled key is for testing only; it gives subsequent test versions the same identity. Keep a separate production signing key for a store release.

Five unit tests cover distances (including date-line crossing) and narration chunk lengths. The release is a first test build, not a store-ready product. Live Wikipedia API requests could not be verified from this workstation because its outbound connection was refused. GPS, narration voices, live results, and screen layout require testing on a physical phone.

Possible next iterations, not yet agreed scope: curated tourism-only points of interest, route-based automatic stories, richer photographs, additional languages, and an AI question-and-answer guide. The earlier full discussion was not available; this version implements the location-based nearby-story concept confirmed in this task.
