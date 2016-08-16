// Workflow controller
tsApp.controller('WorkflowCtrl', [
  '$scope',
  '$http',
  '$location',
  'gpService',
  'utilService',
  'tabService',
  'configureService',
  'securityService',
  'workflowService',
  'utilService',
  'configureService',
  'projectService',
  'reportService',
  '$uibModal',
  function($scope, $http, $location, gpService, utilService, tabService, configureService,
    securityService, workflowService, utilService, configureService, projectService, reportService,
    $uibModal) {
    console.debug("configure WorkflowCtrl");

    // Set up tabs and controller
    tabService.setShowing(true);
    utilService.clearError();
    tabService.setSelectedTabByLabel('Workflow');
    $scope.user = securityService.getUser();
    projectService.getUserHasAnyRole();

    // Selected variables
    $scope.selected = {
      project : null,
      config : null,
      bin : null,
      clusterType : null,
      projectRole : null
    };

    // Lists
    $scope.lists = {
      bins : [],
      records : [],
      configs : [],
      projects : [],
      projectRoles : [],
      recordTypes : workflowService.getRecordTypes(),
      configTypes : workflowService.getConfigTypes()
    }

    // Paging parameters
    $scope.resetPaging = function() {
      $scope.paging = utilService.getPaging();
      $scope.paging.sortField = 'clusterId';
      $scope.paging.callback = {
        getPagedList : getPagedList
      };
    }
    $scope.resetPaging();

    // Set the workflow config
    $scope.setConfig = function(config) {
      $scope.selected.config = config;
      if ($scope.selected.config) {
        $scope.getBins($scope.selected.project.id, $scope.selected.config);
      }
    }

    // Retrieve all bins with project and type
    $scope.getBins = function(projectId, config) {
      // Clear the records
      $scope.lists.records = [];

      // Skip if no config types
      if (config.type) {
        workflowService.getWorkflowBins(projectId, config.type).then(
        // Success
        function(data) {
          $scope.lists.bins = data.bins;
          $scope.lists.bins.totalCount = $scope.lists.bins.length;
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
        $scope.lists.configs = data.configs;
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

    // Regenerate bins
    $scope.regenerateBins = function() {
      workflowService.clearBins($scope.selected.project.id, $scope.selected.config.type).then(
        // Success
        function(response) {
          workflowService.regenerateBins($scope.selected.project.id, $scope.selected.config.type)
            .then(
            // Success
            function(response) {
              $scope.getBins($scope.selected.project.id, $scope.selected.config);
            });
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
              $scope.regenerateBins();
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
              $scope.regenerateBins();
            });
        });
    };

    // Convert date to a string
    $scope.toDate = function(lastModified) {
      return utilService.toDate(lastModified);
    };

    //
    // MODALS
    //

    // Create checklist modal
    $scope.openCreateChecklistModal = function(bin, clusterType) {

      var modalInstance = $uibModal.open({
        templateUrl : 'app/page/workflow/addChecklist.html',
        backdrop : 'static',
        controller : CreateChecklistModalCtrl,
        resolve : {
          projectId : function() {
            return $scope.selected.project.id;
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
      function(project) {

      });
    };

    // Create worklist modal
    // TODO: make this and others work like "log" with an icon in the directive.
    $scope.openCreateWorklistModal = function(bin, clusterType, availableClusterCt) {

      var modalInstance = $uibModal.open({
        templateUrl : 'app/page/workflow/addWorklist.html',
        backdrop : 'static',
        controller : CreateWorklistModalCtrl,
        resolve : {
          projectId : function() {
            return $scope.selected.project.id;
          },
          bin : function() {
            return bin;
          },
          user : function() {
            return $scope.user;
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
        $scope.getBins($scope.selected.project.id, $scope.selected.config);
      });
    };

    // Add config modal
    $scope.openAddConfigModal = function() {
      console.debug('Open add config modal');

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
      console.debug('Open edit config modal');

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
      console.debug('openEditBinModal ');

      var modalInstance = $uibModal.open({
        templateUrl : 'app/page/workflow/editBin.html',
        controller : EditBinModalCtrl,
        backdrop : 'static',
        resolve : {
          bin : function() {
            return lbin;
          },
          workflowConfig : function() {
            return $scope.selected.config;
          },
          bins : function() {
            return $scope.lists.bins;
          },
          config : function() {
            return $scope.selected.config;
          },
          project : function() {
            return $scope.selected.project;
          },
          projects : function() {
            return $scope.lists.projects;
          },
          action : function() {
            return 'Edit';
          }
        }
      });

      modalInstance.result.then(
      // Success
      function(data) {
        $scope.regenerateBins();
      });
    };

    // Clone bin modal
    $scope.openCloneBinModal = function(lbin) {
      console.debug('openCloneBinModal ');

      var modalInstance = $uibModal.open({
        templateUrl : 'app/page/workflow/editBin.html',
        controller : EditBinModalCtrl,
        backdrop : 'static',
        resolve : {
          bin : function() {
            return lbin;
          },
          workflowConfig : function() {
            return $scope.selected.config;
          },
          bins : function() {
            return $scope.lists.bins;
          },
          config : function() {
            return $scope.selected.config;
          },
          project : function() {
            return $scope.selected.project;
          },
          projects : function() {
            return $scope.lists.projects;
          },
          action : function() {
            return 'Clone';
          }
        }
      });

      modalInstance.result.then(
      // Success
      function(data) {
        $scope.regenerateBins();
      });
    };

    // Add bin modal
    $scope.openAddBinModal = function(lbin) {
      console.debug('openAddBinModal ');

      var modalInstance = $uibModal.open({
        templateUrl : 'app/page/workflow/editBin.html',
        controller : EditBinModalCtrl,
        backdrop : 'static',
        resolve : {
          bin : function() {
            return undefined;
          },
          workflowConfig : function() {
            return $scope.selected.config;
          },
          bins : function() {
            return $scope.lists.bins;
          },
          config : function() {
            return $scope.selected.config;
          },
          project : function() {
            return $scope.selected.project;
          },
          projects : function() {
            return $scope.lists.projects;
          },
          action : function() {
            return 'Add';
          }
        }
      });

      modalInstance.result.then(
      // Success
      function(data) {
        $scope.regenerateBins();
      });
    };

    //
    // Initialize - DO NOT PUT ANYTHING AFTER THIS SECTION
    //
    $scope.initialize = function() {
      // configure tab
      securityService.saveTab($scope.user.userPreferences, '/workflow');
      $scope.getProjects();
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