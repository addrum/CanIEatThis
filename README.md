![](/other/logo.png?raw=true)
# CanIEatThis
CanIEatThis was an Android app created as a final year project at university. It allows the user to scan a barcode, fetch the ingredients and tells the user whether the product is suitable for Vegetarians (will be expanded to encompass Vegans, gluten-free etc).

It is now an Android, iOS and web project containing the features above as well as collecting restaurant data about dietary suitability.

[![Build Status](http://217.67.52.70:8080/buildStatus/icon?job=CanIEatThis)](http://217.67.52.70:8080/job/CanIEatThis/)

<a href='https://play.google.com/store/apps/details?id=com.adamshort.canieatthis.app&utm_source=global_co&utm_medium=prtnr&utm_content=Mar2515&utm_campaign=PartBadge&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_gb/badges/images/generic/en_badge_web_generic.png' height='100'/></a>

## Development

Branches "android" and "ios" are "master" branches for respective platforms. Develop by branching off these branches for features/bugs/etc.

"android" and "ios" should *always* be 100% buildable and deployable to a real device.

e.g. 
android -> feature x
ios -> bug y

## Build Process
Pushing to "android" and "ios" will kick off the relevant Jenkins release builds. To release, merge dev branches into "android" or "ios" and then push. For Android, Jenkins does a build and sign of the project and outputs an .apk. This job, when successful, kicks off a beta upload job which uploads the .apk to the beta channel on the Play Store. Prod releases are done manually.

## Setup - Android
You need two things in order to fully setup the project. The first being the keys.xml which contains all the private auth keys for various services.
This needs to be placed at the path: _./android/devicebridge/src/main/res/values/keys.xml_

Secondly, you need to add the following four properties to the _gradle.properties_ file that sits at _./android/gradle.properties_:
- RELEASE_STORE_FILE
- RELEASE_STORE_PASSWORD
- RELEASE_KEY_ALIAS
- RELEASE_KEY_PASSWORD
