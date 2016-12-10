(function() {
	angular.module('CanIEatThisApp', [])
	.controller('SearchController', function($scope, $http) {

		var database = firebase.database();

		$scope.$watch('search', function() {
			fetch();
		});

		$scope.search = 5000168001142;

		function fetch() {
			$http.get("http://world.openfoodfacts.org/api/v0/product/" + $scope.search)
			.then(function(response) {
				$scope.name = response.data.product.product_name == '' 
					? 'No product name found' : response.data.product.product_name
				$scope.ingredients = response.data.product.ingredients_text == ''
					? 'No ingredients found' : prettifyList(response.data.product.ingredients_text)
				$scope.traces = response.data.product.traces == ''
					? 'No traces found' : prettifyList(response.data.product.traces)
				$scope.results = response.data;

				var ingredientsToTest = prettifyList(response.data.product.ingredients_text);
				ingredientsToTest = removeUnwantedCharactersForTesting(ingredientsToTest);
				ingredientsToTest = trimArrayOfStrings(ingredientsToTest);

				var tracesToTest = prettifyList(response.data.product.traces);
				tracesToTest = removeUnwantedCharactersForTesting(tracesToTest);
				tracesToTest = trimArrayOfStrings(tracesToTest);

				compareDataWithFirebase(ingredientsToTest, tracesToTest);
			});
		};

		function compareDataWithFirebase(ingredients, traces) {
			database.ref('/ingredients/').once('value').then(function(snapshot) {
				if (isEmpty(ingredients) && isEmpty(traces)) {
					return [false, false, false, false];
				}

				var bools = [null, null, null, null];

				bools = checkIfValuesAreSuitable(ingredients, snapshot.val(), bools);
				bools = checkIfValuesAreSuitable(traces, snapshot.val(), bools);

				updateDietaryCheckboxes([bools[0] !== null && bools[0], bools[1] !== null && bools[1],
                bools[2] !== null && bools[2], bools[3] !== null && bools[3]]);
			});
		};

		function checkIfValuesAreSuitable(values, snapshot, bools) {
			for (var i = 0; i < values.length; i++) {
				var lowerResValue = values[i].toLowerCase();
				for (key in snapshot) {
					var name = key.toLowerCase();
					if (name === lowerResValue) {
						if (bools[0] === null || bools[0]) {
							bools[0] = snapshot[key].lactose_free;
						}
						if (bools[1] === null || bools[1]) {
							bools[1] = snapshot[key].vegetarian;
						}
						if (bools[2] === null || bools[2]) {
							bools[2] = snapshot[key].vegan;
						}
						if (bools[3] === null || bools[3]) {
							bools[3] = snapshot[key].gluten_free;
						}
					}
				}
			}
			return bools;
		};

		function updateDietaryCheckboxes(bools) {
			console.log(bools);
			$scope.lactoseCheckbox = bools[0];
			$scope.vegetarianCheckbox = bools[1];
			$scope.veganCheckbox = bools[2];
			$scope.glutenCheckbox = bools[3];
		}
	});
})();