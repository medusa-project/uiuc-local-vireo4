var dataDepositLocationRepo1 = [
    dataDepositLocation1,
    dataDepositLocation2,
    dataDepositLocation3
];

var dataDepositLocationRepo2 = [
    dataDepositLocation3,
    dataDepositLocation2,
    dataDepositLocation1
];

var dataDepositLocationRepo3 = [
    dataDepositLocation4,
    dataDepositLocation5,
    dataDepositLocation6
];

angular.module("mock.depositLocationRepo", []).service("DepositLocationRepo", function($q) {
    var repo = mockRepo("DepositLocationRepo", $q, mockDepositLocation, dataDepositLocationRepo1);

    return repo;
});
