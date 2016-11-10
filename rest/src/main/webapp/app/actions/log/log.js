// Log directive
tsApp.directive('log', [ function() {
  console.debug('configure log directive');
  return {
    restrict : 'A',
    scope : {
      selected : '=',
      type : '@',
      lines : '@'
    },
    templateUrl : 'app/actions/log/log.html',
    controller : [
      '$scope',
      '$uibModal',
      'utilService',
      'projectService',
      'workflowService',
      'processService',
      function($scope, $uibModal, utilService, projectService, workflowService, processService) {
        console.debug('configure LogDirective', $scope.selected);

        // Log modal
        $scope.openLogModal = function() {

          var modalInstance = $uibModal.open({
            templateUrl : 'app/actions/log/logModal.html',
            controller : LogModalCtrl,
            backdrop : 'static',
            size : 'lg',
            resolve : {
              selected : function() {
                return $scope.selected;
              },
              type : function() {
                return $scope.type;
              },
            }
          });

          // NO need for result function - no action on close
          // modalInstance.result.then(function(data) {});
        };
        var LogModalCtrl = function($scope, $uibModalInstance, selected, type) {
          console.debug("configure LogModalCtrl");
          $scope.type = type;
          $scope.errors = [];
          $scope.warnings = [];

          // Get log to display
          $scope.getLog = function() {

            if (type == 'Worklist' || type == 'Checklist') {
              var checklistId = (type == 'Checklist' ? selected.worklist.id : null);
              var worklistId = (type == 'Worklist' ? selected.worklist.id : null);
              // Make different calls depending upon the object type
              workflowService.getLog(selected.project.id, checklistId, worklistId).then(
              // Success
              function(data) {
                $scope.log = data;
              },
              // Error
              function(data) {
                utilService.handleDialogError($scope.errors, data);
              });

            }

            else if (type == 'Process') {
              processService.getProcessLog(selected.project.id, selected.process.id).then(
              // Success
              function(data) {
                $scope.log = data;
              },
              // Error
              function(data) {
                utilService.handleDialogError($scope.errors, data);
              });
            }

            else if (type == 'Step') {
              processService.getAlgorithmLog(selected.project.id, selected.step.id).then(
              // Success
              function(data) {
                $scope.log = data;
              },
              // Error
              function(data) {
                utilService.handleDialogError($scope.errors, data);
              });
            }

            // Project/component
            else if (type == 'Project' || type == 'Concept' || type == 'Descriptor'
              || type == 'Code') {

              // Make different calls depending upon the object type
              var objectId = (type == 'Project' ? null : selected.component.id);
              projectService.getLog(selected.project.id, objectId).then(
              // Success
              function(data) {
                $scope.log = data;
              },
              // Error
              function(data) {
                utilService.handleDialogError($scope.errors, data);
              });
            }

            // fail
            else {
              $scope.errors.push('Invalid type passed to log modal controller - ' + type);
            }
          };

          // Close modal
          $scope.close = function() {
            // nothing changed, don't pass a refset
            $uibModalInstance.close();
          };

          // initialize
          $scope.getLog();
        };

      } ]

  };
} ]);