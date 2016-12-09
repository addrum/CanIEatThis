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
					? 'No ingredients found' : response.data.product.ingredients_text
				$scope.traces = response.data.product.traces == ''
					? 'No traces found' : response.data.product.traces
				$scope.results = response.data;
			});
		}
	});
})();