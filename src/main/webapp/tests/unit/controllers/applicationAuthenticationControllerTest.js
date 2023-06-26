describe("controller: ApplicationAuthenticationController", function () {

    var controller, q, scope, mockedUser, User, WsApi;

    var initializeVariables = function(settings) {
        inject(function ($q, _WsApi_) {
            q = $q;
            mockedUser = mockParameterModel(q, mockUser);

            WsApi = _WsApi_;
        });
    };

    var initializeController = function(settings) {
        inject(function ($controller, $location, $rootScope, _ModalService_, _RestApi_, _StorageService_, _User_, _UserService_, _ValidationStore_) {
            scope = $rootScope.$new();

            sessionStorage.role = settings && settings.role ? settings.role : "ROLE_ADMIN";
            sessionStorage.token = settings && settings.token ? settings.token : "faketoken";

            controller = $controller("ApplicationAuthenticationController", {
                $location: $location,
                $scope: scope,
                $window: mockWindow(),
                ModalService: _ModalService_,
                RestApi: _RestApi_,
                StorageService: _StorageService_,
                User: _User_,
                UserService: _UserService_,
                ValidationStore: _ValidationStore_,
                WsApi: WsApi
            });

            // ensure that the isReady() is called.
            if (!scope.$$phase) {
                scope.$digest();
            }
        });
    };

    beforeEach(function() {
        module("core");
        module("vireo");
        module("mock.modalService");
        module("mock.restApi");
        module("mock.storageService");
        module("mock.user", function($provide) {
            User = function() {
                return mockedUser;
            };
            $provide.value("User", User);
        });
        module("mock.userService");
        module("mock.validationStore");
        module("mock.wsApi");

        installPromiseMatchers();
        initializeVariables();
        initializeController();
    });

    describe("Is the controller defined", function () {
        it("should be defined for admin", function () {
            expect(controller).toBeDefined();
        });
        it("should be defined for manager", function () {
            initializeController({role: "ROLE_MANAGER"});
            expect(controller).toBeDefined();
        });
        it("should be defined for reviewer", function () {
            initializeController({role: "ROLE_REVIEWER"});
            expect(controller).toBeDefined();
        });
        it("should be defined for student", function () {
            initializeController({role: "ROLE_STUDENT"});
            expect(controller).toBeDefined();
        });
        it("should be defined for anonymous", function () {
            initializeController({role: "ROLE_ANONYMOUS"});
            expect(controller).toBeDefined();
        });
    });

});
