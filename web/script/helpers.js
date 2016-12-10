function isEmpty(str) {
	return (!str || 0 === str.length);
}

function prettifyList(string) {
	return string.replace(/\[/g, "")
	.replace(/\]/g, "")
	.replace(/\_/g, "")
	.toLowerCase();
};

function trimArrayOfStrings(array) {
	for (var i = 0; i < array.length; i++) {
		array[i] = array[i].trim();
	}
	return array;
};

function removeUnwantedCharactersForTesting(string) {
	// strip percentages in brackets
	string = string.replace(/\(\d*%\d*\)/g, "");
	// strip ))
	string = string.replace(/\)\),/g, ",");
	// replace  ( or )
	string = string.replace(/\(|\),|\)/g, ",");
	// replace : or ; with ,
	string = string.replace(/:|;/g, ",");
	// split "Or" into two
	string = string.replace(/\sor\s/g, ",");
	// split "And" into two
	string = string.replace(/\sand\s/g, ",");
	// replace - with ,
	string = string.replace(/-/g, ",");

	return string.split(',')
};

function getFirebasePromise(database, path) {
	return database.ref(path).once('value').then(function(snapshot) {
		return snapshot.val();
	});
};