describe("controller: EmbargoRepoController", function () {

    var controller, q, scope, EmbargoRepo, WsApi;

    var initializeVariables = function(settings) {
        inject(function ($q, _EmbargoRepo_, _WsApi_) {
            q = $q;

            EmbargoRepo = _EmbargoRepo_;
            WsApi = _WsApi_;
        });
    };

    var initializeController = function(settings) {
        inject(function ($controller, $filter, $rootScope, _DragAndDropListenerFactory_, _ModalService_, _RestApi_, _StorageService_, _WsApi_) {
            scope = $rootScope.$new();

            sessionStorage.role = settings && settings.role ? settings.role : "ROLE_ADMIN";
            sessionStorage.token = settings && settings.token ? settings.token : "faketoken";

            controller = $controller("EmbargoRepoController", {
                $filter: $filter,
                $q: q,
                $scope: scope,
                $window: mockWindow(),
                DragAndDropListenerFactory: _DragAndDropListenerFactory_,
                EmbargoRepo: EmbargoRepo,
                ModalService: _ModalService_,
                RestApi: _RestApi_,
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
        module("mock.dragAndDropListenerFactory");
        module("mock.embargo");
        module("mock.embargoRepo");
        module("mock.modalService");
        module("mock.restApi");
        module("mock.storageService");
        module("mock.wsApi");

        installPromiseMatchers();
        initializeVariables();
        initializeController();
    });

    describe("Is the controller defined", function () {
        it("should be defined", function () {
            expect(controller).toBeDefined();
        });
    });

    describe("Are the scope methods defined", function () {
        it("createEmbargo should be defined", function () {
            expect(scope.createEmbargo).toBeDefined();
            expect(typeof scope.createEmbargo).toEqual("function");
        });
        it("editEmbargo should be defined", function () {
            expect(scope.editEmbargo).toBeDefined();
            expect(typeof scope.editEmbargo).toEqual("function");
        });
        it("isSystemRequired should be defined", function () {
            expect(scope.isSystemRequired).toBeDefined();
            expect(typeof scope.isSystemRequired).toEqual("function");
        });
        it("removeEmbargo should be defined", function () {
            expect(scope.removeEmbargo).toBeDefined();
            expect(typeof scope.removeEmbargo).toEqual("function");
        });
        it("reorderEmbargoDefault should be defined", function () {
            expect(scope.reorderEmbargoDefault).toBeDefined();
            expect(typeof scope.reorderEmbargoDefault).toEqual("function");
        });
        it("reorderEmbargoProquest should be defined", function () {
            expect(scope.reorderEmbargoProquest).toBeDefined();
            expect(typeof scope.reorderEmbargoProquest).toEqual("function");
        });
        it("resetEmbargo should be defined", function () {
            expect(scope.resetEmbargo).toBeDefined();
            expect(typeof scope.resetEmbargo).toEqual("function");
        });
        it("selectEmbargo should be defined", function () {
            expect(scope.selectEmbargo).toBeDefined();
            expect(typeof scope.selectEmbargo).toEqual("function");
        });
        it("sortEmbargoesDefault should be defined", function () {
            expect(scope.sortEmbargoesDefault).toBeDefined();
            expect(typeof scope.sortEmbargoesDefault).toEqual("function");
        });
        it("sortEmbargoesProquest should be defined", function () {
            expect(scope.sortEmbargoesProquest).toBeDefined();
            expect(typeof scope.sortEmbargoesProquest).toEqual("function");
        });
        it("updateEmbargo should be defined", function () {
            expect(scope.updateEmbargo).toBeDefined();
            expect(typeof scope.updateEmbargo).toEqual("function");
        });
    });

    describe("Do the scope methods work as expected", function () {
        it("createEmbargo should create a new embargo", function () {
            scope.modalData = new mockEmbargo(q);

            spyOn(EmbargoRepo, "create").and.callThrough();

            scope.createEmbargo();

            expect(EmbargoRepo.create).toHaveBeenCalled();
        });
        it("editEmbargo should open a modal", function () {
            spyOn(scope, "selectEmbargo");
            spyOn(scope, "openModal");

            scope.editEmbargo(1);

            expect(scope.selectEmbargo).toHaveBeenCalled();
            expect(scope.openModal).toHaveBeenCalled();
        });
        it("isSystemRequired should return a boolean", function () {
            var result;
            var embargo = new mockEmbargo(q);

            embargo.systemRequired = true;
            result = scope.isSystemRequired(embargo);
            expect(result).toBe(true);

            embargo.systemRequired = false;
            result = scope.isSystemRequired(embargo);
            expect(result).toBe(false);

            delete embargo.systemRequired;
            result = scope.isSystemRequired(embargo);
            expect(result).toBe(false);

            result = scope.isSystemRequired();
            expect(result).toBe(false);
        });
        it("removeEmbargo should delete a embargo", function () {
            scope.modalData = new mockEmbargo(q);

            spyOn(scope.modalData, "delete");

            scope.removeEmbargo();

            expect(scope.modalData.delete).toHaveBeenCalled();
        });
        it("reorderEmbargoDefault should reorder a defult embargo", function () {
            spyOn(EmbargoRepo, "reorder");

            scope.reorderEmbargoDefault("a", "b");

            expect(EmbargoRepo.reorder).toHaveBeenCalled();
        });
        it("reorderEmbargoProquest should reorder a proquest embargo", function () {
            spyOn(EmbargoRepo, "reorder");

            scope.reorderEmbargoProquest("a", "b");

            expect(EmbargoRepo.reorder).toHaveBeenCalled();
        });
        it("resetEmbargo should reset the embargo", function () {
            var embargo = new mockEmbargo(q);

            scope.forms = [];
            scope.modalData = embargo;
            scope.modalData.level = null;
            scope.proquestEmbargoes = null;
            scope.defaultEmbargoes = null;

            spyOn(scope.embargoRepo, "clearValidationResults");
            spyOn(embargo, "refresh");
            spyOn(scope, "closeModal");

            scope.resetEmbargo();

            expect(scope.embargoRepo.clearValidationResults).toHaveBeenCalled();
            expect(embargo.refresh).toHaveBeenCalled();
            expect(scope.closeModal).toHaveBeenCalled();
            expect(scope.modalData).toBeDefined();
            expect(scope.proquestEmbargoes).toBeDefined();
            expect(scope.defaultEmbargoes).toBeDefined();

            scope.forms.myForm = mockForms();
            scope.resetEmbargo();

            scope.forms.myForm.$pristine = false;
            scope.resetEmbargo();
        });
        it("selectEmbargo should select a embargo", function () {
            scope.modalData = null;
            scope.embargos = [
                new mockEmbargo(q),
                new mockEmbargo(q)
            ];
            scope.embargos[1].mock(dataEmbargo2);

            // FIXME: the implementation of this is different from other similar implementations in that this selects an ID instead of an index.
            scope.selectEmbargo(2);

            expect(scope.modalData.id).toBe(scope.embargos[1].id);
        });
        it("sortEmbargoesDefault should select a sort action", function () {
            spyOn(EmbargoRepo, "sort");

            scope.sortAction = "confirm";
            scope.sortEmbargoesDefault("column");

            expect(scope.sortAction).toEqual("sortDefaultEmbargoes");
            expect(EmbargoRepo.sort).not.toHaveBeenCalled();

            scope.sortAction = "sortDefaultEmbargoes";
            scope.sortEmbargoesDefault("column");

            expect(scope.sortAction).toEqual("confirm");
            expect(EmbargoRepo.sort).toHaveBeenCalled();

            scope.sortAction = "unknown";
            scope.sortDefault = "unknownDefault";
            scope.sortProquest = "unknownProquest";
            scope.sortEmbargoesDefault("column");
            expect(scope.sortAction).toEqual("unknownDefault");
        });
        it("sortEmbargoesProquest should select a sort action", function () {
            spyOn(EmbargoRepo, "sort");

            scope.sortAction = "confirm";
            scope.sortEmbargoesProquest("column");

            expect(scope.sortAction).toEqual("sortProquestEmbargoes");
            expect(EmbargoRepo.sort).not.toHaveBeenCalled();

            scope.sortAction = "sortProquestEmbargoes";
            scope.sortEmbargoesProquest("column");

            expect(scope.sortAction).toEqual("confirm");
            expect(EmbargoRepo.sort).toHaveBeenCalled();

            scope.sortAction = "unknown";
            scope.sortDefault = "unknownDefault";
            scope.sortProquest = "unknownProquest";
            scope.sortEmbargoesProquest("column");
            expect(scope.sortAction).toEqual("unknownProquest");
        });
        it("updateEmbargo should should save an embargo", function () {
            var embargo1 = new mockEmbargo(q);
            var embargo2 = new mockEmbargo(q);

            embargo1.systemRequired = false;
            embargo2.systemRequired = true;
            embargo2.isActive = false;

            spyOn(embargo1, "save");
            spyOn(EmbargoRepo, "deactivate");
            spyOn(EmbargoRepo, "activate");

            scope.modalData = embargo1;
            scope.updateEmbargo();
            expect(embargo1.save).toHaveBeenCalled();

            scope.modalData = embargo2;
            scope.updateEmbargo();
            expect(EmbargoRepo.deactivate).toHaveBeenCalled();
            expect(embargo2.isActive).toBe(false);

            embargo2.isActive = true;
            scope.updateEmbargo();
            expect(EmbargoRepo.activate).toHaveBeenCalled();
            expect(embargo2.isActive).toBe(true);
        });
    });

});
