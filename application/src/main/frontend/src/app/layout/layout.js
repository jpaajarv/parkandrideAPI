(function() {
    var m = angular.module('parkandride.layout', []);

    function findElement(elements, tag, index) {
        tag = tag.toUpperCase();
        for (var i=0; i < elements.length; i++) {
            if (elements[i].tagName === tag) {
                return elements[i];
            }
        }
    }
    m.directive('mainLayout', function () {
        return {
            restrict: 'E',
            templateUrl: 'layout/mainLayout.tpl.html',
            transclude: true,
            compile: function() {
                return {
                    post: function (scope, element, attributes, controller, transcludeFn) {
                        var transcluded;
                        transcludeFn(scope, function(clone) {
                            transcluded = clone;
                            element.find('headline').replaceWith(findElement(transcluded, 'headline'));
                            // map.link is called after this transclude and it needs to be attached before linking
                            element.find('content').replaceWith(findElement(transcluded, 'content'));
                            // actions.link has not been called yet. It's empty and cannot be attached yet
                        });
                        // actions.link is done and can be attached
                        var actions = findElement(transcluded, 'actions').children[0].children;
                        element.find('actions-top').replaceWith(findElement(actions, 'actions-top'));
                        element.find('actions-bottom').replaceWith(findElement(actions, 'actions-bottom'));
                    }
                };
            }
        };
    });
    m.directive('actions', function() {
        return {
            restrict: 'E',
            transclude: true,
            template: "<div><actions-top></actions-top><actions-bottom></actions-bottom></div>",
            link: function (scope, element, attributes, controller, transcludeFn) {
                transcludeFn(scope, function(clone) {
                    element.find('actions-top').append(clone);
                });
                transcludeFn(scope, function(clone) {
                    element.find('actions-bottom').append(clone);
                });
            }
        };
    });
})();
