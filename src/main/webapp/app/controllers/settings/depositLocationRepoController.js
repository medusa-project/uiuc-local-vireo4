
vireo.controller("DepositLocationRepoController", function ($controller, $scope, $q, DepositLocationRepo, DepositLocation, PackagerRepo, DragAndDropListenerFactory) {
    angular.extend(this, $controller("AbstractController", {
        $scope: $scope
    }));

    $scope.depositLocationRepo = DepositLocationRepo;

    $scope.depositLocations = DepositLocationRepo.getAll();

    $scope.protocols = {
        "SWORDv1Depositor": "SWORDv1Depositor",
        "FileDeposit": "File Deposit"
    };

    $scope.collections = [];

    $scope.packagers = PackagerRepo.getAll();

    $scope.ready = $q.all([DepositLocationRepo.ready(), PackagerRepo.ready()]);

    $scope.dragging = false;

    $scope.trashCanId = 'deposit-location-trash';

    $scope.forms = {};

    var isTestDepositing = false;

    $scope.ready.then(function () {

        $scope.resetDepositLocation = function () {

            $scope.depositLocationRepo.clearValidationResults();

            if ($scope.modalData !== undefined && $scope.modalData.refresh !== undefined) {
                $scope.modalData.refresh();
            }

            $scope.modalData = {
                name: '',
                depositorName: 'SWORDv1Depositor',
                timeout: 240,
                packager: $scope.packagers.length > 0 ? $scope.packagers[0] : undefined,
                repository: '',
                username: '',
                password: '',
                testDepositLocation: function () {
                    isTestDepositing = true;
                    var testData = angular.copy($scope.modalData);
                    delete testData.packager;
                    var testableDepositLocation = new DepositLocation(testData);
                    testableDepositLocation.testConnection().then(function (response) {
                        var apiRes = angular.fromJson(response.body);
                        if (apiRes.meta.status === 'SUCCESS') {
                            var collections = apiRes.payload.HashMap;
                            angular.forEach(collections, function (uri, name) {
                                $scope.collections.push({
                                    "name": name,
                                    "uri": uri
                                });
                            });
                        }
                        isTestDepositing = false;
                    });
                },
                isTestDepositing: function () {
                    return isTestDepositing;
                },
                isTestable: function () {
                    return (!isTestDepositing && $scope.modalData.name && $scope.modalData.depositorName && $scope.modalData.repository && $scope.modalData.username && $scope.modalData.password);
                }
            };

            for (var key in $scope.forms) {
                if ($scope.forms[key] !== undefined && !$scope.forms[key].$pristine) {
                    $scope.forms[key].$setPristine();
                    $scope.forms[key].$setUntouched();
                }
            }

            $scope.closeModal();
        };

        $scope.resetDepositLocation();

        $scope.createDepositLocation = function () {
            DepositLocationRepo.create($scope.modalData);
        };

        $scope.selectDepositLocation = function (index) {
            $scope.resetDepositLocation();

            angular.extend($scope.modalData, $scope.depositLocations[index]);
        };

        $scope.editDepositLocation = function (index) {
            $scope.selectDepositLocation(index - 1);
            $scope.openModal('#depositLocationEditModal');
        };

        $scope.updateDepositLocation = function () {
            $scope.modalData.save();
        };

        $scope.removeDepositLocation = function () {
            $scope.modalData.delete();
        };

        $scope.reorderDepositLocation = function (src, dest) {
            return DepositLocationRepo.reorder(src, dest);
        };

        $scope.dragControlListeners = DragAndDropListenerFactory.buildDragControls({
            trashId: $scope.trashCanId,
            dragging: $scope.dragging,
            select: $scope.selectDepositLocation,
            model: $scope.depositLocations,
            confirm: '#depositLocationConfirmRemoveModal',
            reorder: $scope.reorderDepositLocation,
            container: '#deposit-locations'
        });

        DepositLocationRepo.listen(function (data) {
            $scope.resetDepositLocation();
        });

    });

});
