// Workflow controller
tsApp.controller('WorkflowCtrl', [
  '$scope',
  '$http',
  '$location',
  '$uibModal',
  'utilService',
  'websocketService',
  'tabService',
  'configureService',
  'securityService',
  'projectService',
  'metadataService',
  'workflowService',
  'reportService',
  function($scope, $http, $location, $uibModal, utilService, websocketService, tabService,
    configureService, securityService, projectService, metadataService, workflowService,
    reportService) {
    console.debug("configure WorkflowCtrl");

    // Set up tabs and controller
    tabService.setShowing(true);
    utilService.clearError();
    $scope.user = securityService.getUser();
    projectService.getUserHasAnyRole();
    tabService.setSelectedTabByLabel('Workflow');

    $scope.pageSizes = utilService.getPageSizes();

    // Selected variables
    $scope.selected = {
      project : null,
      config : null,
      bin : null,
      clusterType : null,
      projectRole : null,
      // Used to trigger events in worklist-table directive controller
      refreshCt : 0,
      terminology : null,
      metadata : null
    };

    // Lists
    $scope.lists = {
      bins : [],
      records : [],
      configs : [],
      projects : [],
      projectRoles : [],
      queryTypes : [],
      recordTypes : workflowService.getRecordTypes()
    }

    // Accordion Groups
    $scope.groups = [ {
      title : "Bins",
      open : true
    }, {
      open : false
    }, {
      open : false
    } ];

    // Handle worklist actions
    $scope.$on('termServer::binsChange', function(event, project) {
      if (project.id == $scope.selected.project.id) {
        // Bins changed, refresh bins
        $scope.getBins($scope.selected.project.id, $scope.selected.config, $scope.selected.bin);
      }
    });

    // $scope.$on('termServer::checklistChange', -- n/a, no action on checklist
    // change
    $scope.$on('termServer::worklistChange', function(event, data) {
      if (data.id == $scope.selected.project.id) {
        // could affect worklist bin counts
        $scope.getBins($scope.selected.project.id, $scope.selected.config, $scope.selected.bin);
      }
    });

    // Paging parameters
    $scope.resetPaging = function() {
      $scope.paging = utilService.getPaging();
      $scope.paging.sortField = 'clusterId';
      $scope.paging.callbacks = {
        getPagedList : getPagedList
      };
    }
    $scope.resetPaging();

    // Set the workflow config
    $scope.setConfig = function(config) {
      $scope.selected.config = config;
      if ($scope.selected.config) {
        $scope.getBins($scope.selected.project.id, $scope.selected.config, $scope.selected.bin);
      }
    }

    // Retrieve all bins with project and type
    $scope.getBins = function(projectId, config, bin) {
      // Clear the records
      $scope.lists.records = [];

      // Skip if no config types
      if (config.type) {
        workflowService.getWorkflowBins(projectId, config.type).then(
        // Success
        function(data) {
          $scope.lists.bins = data.bins;
          $scope.lists.bins.totalCount = $scope.lists.bins.length;
          if (bin) {
            var filtered = $scope.lists.bins.filter(function(item) {
              return item.id == bin.id;
            });
            if (filtered.length == 1) {
              $scope.selectBin(filtered[0]);
            }
          }

        });
      }
    };

    // handle change in project role
    $scope.changeProjectRole = function() {
      // save the change
      securityService.saveRole($scope.user.userPreferences, $scope.selected.projectRole);
    }

    // Set the project
    $scope.setProject = function(project) {
      $scope.selected.project = project;

      // Get role for project (requires a lookup and will save user prefs
      projectService.getRoleForProject($scope.user, $scope.selected.project.id).then(
      // Success
      function(data) {
        // Get role and set role options
        $scope.selected.projectRole = data.role;
        $scope.lists.projectRoles = data.options;

        // Get configs
        $scope.getConfigs();
      });
      projectService.findAssignedUsersForProject($scope.selected.project.id, null, null).then(
        function(data) {
          $scope.lists.users = data.users;
          $scope.lists.users.totalCount = data.totalCount;
        });

      // Initialize metadata
      metadataService.getTerminology($scope.selected.project.terminology,
        $scope.selected.project.version).then(
      // Success
      function(data) {
        metadataService.setTerminology(data);
      });
      metadataService.getAllMetadata($scope.selected.project.terminology,
        $scope.selected.project.version).then(
      // Success
      function(data) {
        $scope.selected.metadata = data;
      });

    }

    // Retrieve all projects
    $scope.getProjects = function() {

      projectService.getProjectsForUser($scope.user).then(
      // Success
      function(data) {
        $scope.lists.projects = data.projects;
        $scope.setProject(data.project);
      });

    };

    // Retrieve all projects
    $scope.getConfigs = function() {
      workflowService.getWorkflowConfigs($scope.selected.project.id).then(
      // Success
      function(data) {
        $scope.lists.configs = data.configs.sort(utilService.sortBy('type'));
        $scope.setConfig($scope.lists.configs[0]);
      });
    };

    // Selects a bin (setting $scope.selected.bin)
    // clusterType is optional
    $scope.selectBin = function(bin, clusterType) {
      $scope.selected.bin = bin;
      $scope.selected.clusterType = clusterType;

      if (clusterType && clusterType == 'default') {
        $scope.paging.filter = ' NOT clusterType:[* TO *]';
      } else if (clusterType && clusterType != 'all') {
        $scope.paging.filter = clusterType;
      } else if (clusterType == 'all') {
        $scope.paging.filter = '';
      }
      $scope.resetPaging();
      getPagedList();
    };

    // This needs to be a function so it can be scoped properly for the
    // pager
    $scope.getPagedList = function() {
      getPagedList();
    };
    function getPagedList() {

      var pfs = {
        startIndex : ($scope.paging.page - 1) * $scope.paging.pageSize,
        maxResults : $scope.paging.pageSize,
        sortField : $scope.paging.sortField,
        ascending : $scope.paging.sortAscending,
        queryRestriction : $scope.paging.filter ? $scope.paging.filter : ''
      };

      if ($scope.paging.typeFilter) {
        var value = $scope.paging.typeFilter;

        // Handle inactive
        if (value == 'N') {
          pfs.queryRestriction += (pfs.queryRestriction ? ' AND ' : '') + ' workflowStatus:N*';
        } else if (value == 'R') {
          pfs.queryRestriction += (pfs.queryRestriction ? ' AND ' : '') + ' workflowStatus:R*';
        }

      }

      workflowService.findTrackingRecordsForWorkflowBin($scope.selected.project.id,
        $scope.selected.bin.id, pfs).then(
      // Success
      function(data) {
        $scope.lists.records = data.records;
        $scope.lists.records.totalCount = data.totalCount;
      });

    }

    // Regenerate single bin
    $scope.regenerateBin = function(bin) {
      workflowService
        .regenerateBin($scope.selected.project.id, bin.id, $scope.selected.config.type).then(
        // Success
        function(data) {
          $scope.getBins($scope.selected.project.id, $scope.selected.config, bin);
        });
    };

    // Regenerate bins
    $scope.regenerateBins = function() {
      workflowService.clearBins($scope.selected.project.id, $scope.selected.config.type).then(
        // Success
        function(data) {
          workflowService.regenerateBins($scope.selected.project.id, $scope.selected.config.type)
            .then(
            // Success
            function(data) {
              $scope.getBins($scope.selected.project.id, $scope.selected.config, $scope.selected.bin);
            });
        });
    };

    // Recompute concept status
    $scope.recomputeConceptStatus = function(updateFlag) {
      workflowService.recomputeConceptStatus($scope.selected.project.id, updateFlag).then(
      // Success
      function(response) {
        $scope.getBins($scope.selected.project.id, $scope.selected.config, $scope.selected.bin);
      });
    };

    // enable/disable
    $scope.toggleEnable = function(bin) {

      workflowService.getWorkflowBinDefinition($scope.selected.project.id, bin.name,
        $scope.selected.config.type).then(
        function(response) {
          var bin = response;
          if (bin.enabled) {
            bin.enabled = false;
          } else {
            bin.enabled = true;
          }
          workflowService.updateWorkflowBinDefinition($scope.selected.project.id, bin).then(
            function(response) {
              $scope.getBins($scope.selected.project.id, $scope.selected.config, bin);
            });
        });
    };

    // remove config
    $scope.removeConfig = function(config) {
      workflowService.removeWorkflowConfig($scope.selected.project.id, config.id).then(
      // Success
      function(response) {
        $scope.getConfigs();
      });
    };

    // remove bin/definition
    $scope.removeBin = function(bin) {
      workflowService.getWorkflowBinDefinition($scope.selected.project.id, bin.name,
        $scope.selected.config.type).then(
        // Success
        function(response) {
          var definition = response;

          workflowService.removeWorkflowBinDefinition($scope.selected.project.id, definition.id)
            .then(
            // Successs
            function(response) {
              $scope.getBins($scope.selected.project.id, $scope.selected.config);
            });
        });
    };

    // Convert date to a string
    $scope.toDate = function(lastModified) {
      return utilService.toDate(lastModified);
    };

    // Save the accorion status
    $scope.saveAccordionStatus = function() {
      console.debug('saveAccordionStatus', $scope.groups);
      $scope.user.userPreferences.properties['workflowGroups'] = JSON.stringify($scope.groups);
      securityService.updateUserPreferences($scope.user.userPreferences);
    }

    // Indicate whether user has permission
    $scope.hasPermissions = function(action) {
      return securityService.hasPermissions(action);
    }

    //
    // MODALS
    //

    // Add checklist modal
    $scope.openAddChecklistModal = function(bin, clusterType) {

      var modalInstance = $uibModal.open({
        templateUrl : 'app/page/workflow/addChecklist.html',
        backdrop : 'static',
        controller : 'ChecklistModalCtrl',
        resolve : {
          selected : function() {
            return $scope.selected;
          },
          lists : function() {
            return $scope.lists;
          },
          user : function() {
            return $scope.user;
          },
          bin : function() {
            return bin;
          },
          clusterType : function() {
            return clusterType;
          }

        }
      });

      modalInstance.result.then(
      // Success
      function(checklist) {
        // "checklists" accordion should reload
        $scope.selected.refreshCt++;
      });
    };

    // Add worklist modal
    $scope.openAddWorklistModal = function(bin, clusterType, availableClusterCt) {

      var modalInstance = $uibModal.open({
        templateUrl : 'app/page/workflow/addWorklist.html',
        backdrop : 'static',
        controller : 'WorklistModalCtrl',
        resolve : {
          selected : function() {
            return $scope.selected;
          },
          lists : function() {
            return $scope.lists;
          },
          user : function() {
            return $scope.user;
          },
          bin : function() {
            return bin;
          },
          clusterType : function() {
            return clusterType;
          },
          availableClusterCt : function() {
            return availableClusterCt;
          }
        }
      });

      modalInstance.result.then(
      // Success
      function(project) {
        $scope.getBins($scope.selected.project.id, $scope.selected.config, $scope.selected.bin);
        // "worklists" accordion should reload
        $scope.selected.refreshCt++;

      });
    };

    // Add config modal
    $scope.openAddConfigModal = function() {

      var modalInstance = $uibModal.open({
        templateUrl : 'app/page/workflow/editConfig.html',
        controller : 'ConfigModalCtrl',
        backdrop : 'static',
        resolve : {
          selected : function() {
            return $scope.selected;
          },
          lists : function() {
            return $scope.lists;
          },
          user : function() {
            return $scope.user;
          },
          action : function() {
            return 'Add';
          }
        }
      });

      modalInstance.result.then(
      // Success
      function(data) {
        if (data) {
          $scope.getConfigs();
        }
      });
    };

    // Edit config modal
    $scope.openEditConfigModal = function() {

      var modalInstance = $uibModal.open({
        templateUrl : 'app/page/workflow/editConfig.html',
        controller : 'ConfigModalCtrl',
        backdrop : 'static',
        resolve : {
          selected : function() {
            return $scope.selected;
          },
          lists : function() {
            return $scope.lists;
          },
          user : function() {
            return $scope.user;
          },
          action : function() {
            return 'Edit';
          }
        }
      });

      modalInstance.result.then(
      // Success
      function(data) {
        if (data) {
          $scope.getConfigs();
        }
      });
    };
    // Edit bin modal
    $scope.openEditBinModal = function(lbin) {

      var modalInstance = $uibModal.open({
        templateUrl : 'app/page/workflow/editBin.html',
        controller : 'BinModalCtrl',
        backdrop : 'static',
        resolve : {
          selected : function() {
            return $scope.selected;
          },
          lists : function() {
            return $scope.lists;
          },
          user : function() {
            return $scope.user;
          },
          bin : function() {
            return lbin;
          },
          action : function() {
            return 'Edit';
          }
        }
      });

      modalInstance.result.then(
      // Success
      function(data) {
        $scope.getBins($scope.selected.project.id, $scope.selected.config, lbin);
      });
    };

    // Clone bin modal
    $scope.openCloneBinModal = function(lbin) {

      var modalInstance = $uibModal.open({
        templateUrl : 'app/page/workflow/editBin.html',
        controller : 'BinModalCtrl',
        backdrop : 'static',
        resolve : {
          selected : function() {
            return $scope.selected;
          },
          lists : function() {
            return $scope.lists;
          },
          user : function() {
            return $scope.user;
          },
          bin : function() {
            return lbin;
          },
          action : function() {
            return 'Clone';
          }
        }
      });

      modalInstance.result.then(
      // Success
      function(data) {
        $scope.getBins($scope.selected.project.id, $scope.selected.config);
      });
    };

    // Add bin modal
    $scope.openAddBinModal = function(lbin) {

      var modalInstance = $uibModal.open({
        templateUrl : 'app/page/workflow/editBin.html',
        controller : 'BinModalCtrl',
        backdrop : 'static',
        resolve : {
          selected : function() {
            return $scope.selected;
          },
          lists : function() {
            return $scope.lists;
          },
          user : function() {
            return $scope.user;
          },
          bin : function() {
            return lbin;
          },
          action : function() {
            return 'Add';
          }
        }
      });

      modalInstance.result.then(
      // Success
      function(data) {
        $scope.getBins($scope.selected.project.id, $scope.selected.config);
      });
    };

    //
    // Initialize - DO NOT PUT ANYTHING AFTER THIS SECTION
    //
    $scope.initialize = function() {
      // configure tab
      securityService.saveTab($scope.user.userPreferences, '/workflow');

      $scope.getProjects();
      // Get query types
      projectService.getQueryTypes().then(function(data) {
        $scope.lists.queryTypes = data.strings;
      });
      if ($scope.user.userPreferences.properties['workflowGroups']) {
        var savedWorkflowGroups = JSON
          .parse($scope.user.userPreferences.properties['workflowGroups']);
        angular.copy(savedWorkflowGroups, $scope.groups);
      }
    };

    //
    // Initialization: Check that application is configured
    //
    configureService.isConfigured().then(function(isConfigured) {
      if (!isConfigured) {
        $location.path('/configure');
      } else {
        $scope.initialize();
      }
    });

    // end
  } ]);