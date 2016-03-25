// Content controller
tsApp.directive('header', [ '$rootScope', '$routeParams', 'securityService',
  function($rootScope, $routeParams, securityService) {
    console.debug('configure header directive');
    return {
      restrict : 'A',
      scope : {},
      templateUrl : 'app/page/header/header.html',
      link : function(scope, element, attrs) {

        scope.isShowing = function() {
          switch ($routeParams.mode) {
          case 'simple':
             return false;
          default:
             return true;
          }
        }
        // Declare user
        scope.user = securityService.getUser();

        // Logout method
        scope.logout = function() {
          securityService.logout();
        };
      }
    };
  } ]);