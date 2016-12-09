(function() {
	angular.module('CanIEatThisApp', [])
	.controller('SearchController', function($scope, $http) {
		$scope.$watch('search', function() {
			fetch();
		});

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
			});
		}

		function prettifyList(list) {
			return list.replace(/\[/g, "")
				.replace(/\]/g, "")
				.replace(/\_/g, "")
				.toLowerCase();
		}
	});
})();