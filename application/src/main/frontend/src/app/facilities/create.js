(function() {
    var m = angular.module('facilities.create', [
        'ui.router',
        'ngBoilerplate.services.facilities'
    ]);

    m.config(function config($stateProvider) {
        $stateProvider.state('facilities-create', { // dot notation in ui-router indicates nested ui-view
            url: '/facilities/create', // TODO set facilities base path on upper level and say here /create ?
            views: {
                "main": {
                    controller: 'CreateCtrl',
                    templateUrl: 'facilities/create.tpl.html'
                }
            },
            data: { pageTitle: 'Create Facility' }
        });
    });

    m.controller('CreateCtrl', CreateController);
    function CreateController($state, FacilityService) {
        this.facility = {};

        this.addFacility = function() {
            var dummyBorder =   {
                "type": "Polygon",
                    "coordinates": [[
                    [60.25055, 25.010827],
                    [60.250023, 25.011867],
                    [60.250337, 25.012479],
                    [60.250886, 25.011454],
                    [60.25055, 25.010827]
                ]]
            };

            this.facility.border = dummyBorder;
            FacilityService.save(this.facility);
            $state.go('facilities');
        };
    }

    m.directive('aliases', function() {
        return {
            restrict: 'A',
            require: 'ngModel',
            link: function(scope, element, attr, ngModel) {
                function fromView(text) {
                    return (text || '').split(/\s*,\s*/);
                }
                ngModel.$parsers.push(fromView);
            }
        };
    });

    m.controller('CapacityCtrl', CapacityController);
    function CapacityController($log) {
        this.capacity = {};
        this.addCapacity = function(facility){
            facility.capacities = (facility.capacities || {});
            facility.capacities[this.capacity.type] = {'built': this.capacity.built, 'unavailable': this.capacity.unavailable };
            $log.info(facility.capacities);
            this.capacity = {};
        };
    }
})();

