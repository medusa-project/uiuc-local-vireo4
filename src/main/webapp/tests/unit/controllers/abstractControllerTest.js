describe("controller: AbstractController", function () {

    var controller, q, scope, timeout, window, RestApi, WsApi;

    var initializeVariables = function(settings) {
        inject(function ($q, $timeout, _RestApi_, _WsApi_) {
            q = $q;
            timeout = $timeout;
            window = mockWindow();

            RestApi = _RestApi_;
            WsApi = _WsApi_;
        });
    };

    var initializeController = function(settings) {
        inject(function ($controller, $rootScope, _ModalService_, _StorageService_) {
            scope = $rootScope.$new();

            sessionStorage.role = settings && settings.role ? settings.role : "ROLE_ADMIN";
            sessionStorage.token = settings && settings.token ? settings.token : "faketoken";

            controller = $controller("AbstractController", {
                $scope: scope,
                $window: window,
                ModalService: _ModalService_,
                RestApi: RestApi,
                StorageService: _StorageService_,
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

    describe("Are the scope methods defined", function () {
        it("copy should be defined", function () {
            expect(scope.copy).toBeDefined();
            expect(typeof scope.copy).toEqual("function");
        });
        it("isAdmin should be defined", function () {
            expect(scope.isAdmin).toBeDefined();
            expect(typeof scope.isAdmin).toEqual("function");
        });
        it("isAnonymous should be defined", function () {
            expect(scope.isAnonymous).toBeDefined();
            expect(typeof scope.isAnonymous).toEqual("function");
        });
        it("isManager should be defined", function () {
            expect(scope.isManager).toBeDefined();
            expect(typeof scope.isManager).toEqual("function");
        });
        it("isReviewer should be defined", function () {
            expect(scope.isReviewer).toBeDefined();
            expect(typeof scope.isReviewer).toEqual("function");
        });
        it("isStudent should be defined", function () {
            expect(scope.isStudent).toBeDefined();
            expect(typeof scope.isStudent).toEqual("function");
        });
        it("isUser should be defined", function () {
            expect(scope.isUser).toBeDefined();
            expect(typeof scope.isUser).toEqual("function");
        });
        it("reportError should be defined", function () {
            expect(scope.reportError).toBeDefined();
            expect(typeof scope.reportError).toEqual("function");
        });
    });

    describe("Do the scope methods work as expected", function () {
        it("copy should return a copy", function () {
            var result;
            var toCopy = {a: "a"};

            result = scope.copy(toCopy);
            expect(result).toEqual(toCopy);
        });
        it("isAdmin should return a boolean", function () {
            var result = scope.isAdmin();

            expect(result).toBe(true);
        });
        it("isAnonymous should return a boolean", function () {
            var result = scope.isAnonymous();

            expect(result).toBe(false);

            initializeController({role: "ROLE_ANONYMOUS"});
            result = scope.isAnonymous();

            expect(result).toBe(true);
        });
        it("isManager should return a boolean", function () {
            var result = scope.isManager();

            expect(result).toBe(false);

            initializeController({role: "ROLE_MANAGER"});
            result = scope.isManager();

            expect(result).toBe(true);
        });
        it("isReviewer should return a boolean", function () {
            var result = scope.isReviewer();

            expect(result).toBe(false);

            initializeController({role: "ROLE_REVIEWER"});
            result = scope.isReviewer();

            expect(result).toBe(true);
        });
        it("isStudent should return a boolean", function () {
            var result = scope.isStudent();

            expect(result).toBe(false);

            initializeController({role: "ROLE_STUDENT"});
            result = scope.isStudent();

            expect(result).toBe(true);
        });
        it("isUser should return a boolean", function () {
            var result = scope.isUser();

            expect(result).toBe(false);

            initializeController({role: "ROLE_STUDENT"});
            result = scope.isUser();

            expect(result).toBe(true);
        });
        it("reportError should report an error", function () {
            var alert = {
                channel: "channel",
                time: 0,
                type: 0,
                message: "message"
            };
            var response = {
                data: null
            };

            spyOn(scope, "openModal");
            scope.reportError(alert);
            scope.$digest();
            expect(scope.openModal).toHaveBeenCalled();

            scope.openModal = function() {};
            spyOn(scope, "openModal");
            RestApi.post = function () {
                return valuePromise(q.defer(), response, "reject");
            };
            scope.reportError(alert);
            scope.$digest();
            expect(scope.openModal).not.toHaveBeenCalled();

            response.data = {
                message: "EXPIRED_JWT"
            };
            scope.reportError(alert);
            scope.$digest();
            expect(scope.openModal).not.toHaveBeenCalled();

            RestApi.post = function () {
                return valuePromise(q.defer(), response, "notify", timeout);
            };
            scope.reportError(alert);
            scope.$digest();
            timeout.flush();
            expect(scope.openModal).not.toHaveBeenCalled();
        });
    });

});
