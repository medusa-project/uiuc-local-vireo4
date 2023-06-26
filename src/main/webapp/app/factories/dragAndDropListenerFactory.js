vireo.factory('DragAndDropListenerFactory', function($q, ModalService) {

    this.buildDragControls = function(drag) {

        var listener = {

            'trash': {
                hover: false,
                element: null,
                id: ''
            },

            'select': null,

            'dragging': null,

            'confirm': {
                'remove': {
                    'modal': ''
                }
            },

            'reorder': function(src, dest) {
                return $q(function(resolve) {
                    resolve();
                });
            }
        };

        if(typeof drag == 'object') {
            listener.trash.id = drag.trashId;
            listener.dragging = drag.dragging;
            listener.select = drag.select;
            listener.model = drag.model;
            listener.confirm.remove.modal = drag.confirm;
            listener.reorder = drag.reorder;
        }
        else {
            console.log('ensure configured');
        }

        var startingObj;

        var inbounds;

        var original;

        var updateRequest = function() {
            angular.forEach(listener.model, function(model) {
                model.updateRequested = true;
            });
        };

        var dragControls = {
            getListener: function () {
                return listener;
            },
            dragStart: function(event) {
                inbounds = true;
                original = angular.copy(listener.model);
                startingObj = event.source.sortableScope.modelValue[0];
                listener.dragging = true;
                listener.select(event.source.index);
                angular.element('.as-sortable-drag').css('display', 'none');
            },
            dragMove: function(movement, containment, event) {
                inbounds = angular.element(event.target).parents('.list-wrapper').length > 0;
                var dragging = angular.element('.as-sortable-drag');
                dragging.css('display', 'block');
                dragging.css('margin-top', angular.element(drag.container).offset().top - angular.element('html').scrollTop());
                dragging.css('margin-left', '250px');
                if(listener.trash.hover) {
                    listener.trash.hover = false;
                    listener.trash.element.removeClass('dragging');
                }
            },
            dragEnd: function(event) {
                if(listener.dragging) {
                    if(!inbounds) {
                        angular.extend(listener.model, original);
                    }
                    if(listener.trash.hover) {
                        updateRequest();
                        ModalService.openModal(listener.confirm.remove.modal);
                        listener.trash.element.removeClass('dragging');
                    } else {
                        listener.select(-1);
                    }
                }
                listener.dragging = false;
            },
            accept: function (sourceItemHandleScope, destSortableScope) {
                var currentElement = destSortableScope.element;
                if(listener.dragging && currentElement[0].id === listener.trash.id) {
                    listener.trash.hover = true;
                    listener.trash.element = currentElement;
                    listener.trash.element.addClass('dragging');
                 }
                 else {
                     listener.trash.hover = false;
                 }
                 return sourceItemHandleScope.itemScope.sortableScope.$id === destSortableScope.$id;
            },
            orderChanged: function(event) {
                if(inbounds) {
                    var isSingleSorted = (listener.model.length === event.source.sortableScope.modelValue.length);
                    var src = event.source.index + 1;
                    var dest = event.dest.index + 1;
                    updateRequest();
                    listener.reorder(src, dest).then(function(res) {
                        var message = angular.fromJson(res.body);
                        if (message.meta.status !== "SUCCESS") {
                            angular.extend(listener.model, original);
                        }
                    });
                }
            },
            containerPositioning: 'relative',
            containment: drag.container
        };

        return dragControls;
    };

    return this;
});
