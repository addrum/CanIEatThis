![](/other/logo.png?raw=true)
# CanIEatThis
CanIEatThis was an Android app created as a final year project at university. It allows the user to scan a barcode, fetch the ingredients and tells the user whether the product is suitable for Vegetarians (will be expanded to encompass Vegans, gluten-free etc).

It is now an Android, iOS and web project containing the features above as well as collecting restaurant data about dietary suitability.

[![Build Status](http://217.67.52.70:8080/buildStatus/icon?job=CanIEatThis)](http://217.67.52.70:8080/job/CanIEatThis/)

## Development

Branches "android" and "ios" are "master" branches for respective platforms. Develop by branching off these branches for features/bugs/etc.

"android" and "ios" should *always* be 100% buildable and deployable to a real device.

e.g. 
android -> feature x
ios -> bug y

## Setup
You need two things in order to fully setup the project. The first being the keys.xml which contains all the private auth keys for various services.
This needs to be placed at the path: ./android/devicebridge/src/main/res/values/keys.xml

Secondly, you need to create a global *gradle.properties* file which points to the signing package. 
- Create a *gradle.properties* at ~/.gradle/gradle.properties
- Include the following four properties:
	- RELEASE_STORE_FILE
	- RELEASE_STORE_PASSWORD
	- RELEASE_KEY_ALIAS
	- RELEASE_KEY_PASSWORD
