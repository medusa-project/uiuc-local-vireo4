vireo.directive("vireoTabs", function() {
	return {
		template: '<div id="tabs-directive" class="tabs"><span ng-transclude></span><hr></div>',
		restrict: 'E',
		replace: false,
		transclude: true,
		scope: false,
		controller: function($scope, $location, VireoTabService) {
			var isCurrent = function(path) {
				return ('/' + path).indexOf($location.path()) === 0;
			};
			$scope.activeTab = function(path) {
				var active = VireoTabService.isActive(path);
				if(!active && isCurrent(path)) {
					$scope.setActive(path);
				}
				return active;
			};
			$scope.setActive = function(path) {
				VireoTabService.activate(path);
			};
		}
	};
});

vireo.directive("vireoTab", function($compile, $location, VireoTabService, WsApi) {
	 return {
		template: '<span ng-class="{\'active\': activeTab(path)}" ng-click="setActive(path)" class="tab">{{label}}</span>',
		restrict: 'E',
		replace: false,
		transclude: false,
		require: '^vireoTabs',
		scope: true,
		link: function ($scope, attr, parent) {
			angular.extend($scope, parent);
			angular.extend($scope, attr);

			$scope.reload = angular.isDefined($scope.reload) ? ($scope.reload === 'false') ? false : true : true;

			var span = angular.element('<span id="'+($scope.path.replace(/\//g, "-"))+'" ng-if="activeTab(path)">');
			span.html("<ng-include src='view'></ng-include>");

			if(angular.element("#"+$scope.path.replace(/\//g, "-")).length!==0) {
				angular.element("#"+$scope.path.replace(/\//g, "-")).remove();
			}
			angular.element('#tabs-directive').after($compile(span)($scope));

			if($scope.reload === false) {
				WsApi.registerPersistentRouteBasedChannel($scope.path);
			}

			VireoTabService.register($scope.path, function() {
				$location.path($scope.path);
			});

		}
	};
});

vireo.service("VireoTabService", function() {
	var tabs = {};
	var active;
	return {
		register: function(path, loader) {
			tabs[path] = {
				load: loader
			};
		},
		activate: function(path) {
			tabs[path].load();
			active = path;
		},
		isActive: function(path) {
			return active === path;
		}
	};
});