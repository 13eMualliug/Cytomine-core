/*
 * Copyright (c) 2009-2018. Authors: see NOTICE file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var DetailedUploadedFileTreeDialog = Backbone.View.extend({

    initialize: function () {
        _.bindAll(this, 'render');
    },
    render: function () {
        var self = this;
        require([
                "text!application/templates/upload/DetailedUploadedFileTreeDialog.tpl.html"
            ],
            function (tpl) {
                self.doLayout(tpl);
            });
        return this;
    },
    tree:null,

    doLayout: function (tpl) {
        var self = this;

        $("#detailedUploadedFileTreeDialog-"+self.model.id).remove();

        var htmlCode = _.template(tpl, self.model);

        $(this.el).append(htmlCode);

        self.update();

        $("#detailedUploadedFileTreeDialog-"+self.model.id).on('click', '.previewUploadedFile', function(e) {
            var url = $(this).data('url');
            $("#detailedUploadedFileTreeDialog-"+self.model.id).find("#filePreview")[0].src = url;
            return false;
        });

        $("#detailedUploadedFileTreeDialog-"+self.model.id).on('click', ".detailsUploadedFile", function (e) {
            var idUpload = $(e.currentTarget).data("ufid");
            $.get( new UploadedFileModel({ id: idUpload}).url(), function( data ) {
                $("#detailedUploadedFileTreeDialog-"+self.model.id).modal('hide');
                data.parentFilename = null;

                data.parentId = data.parent;
                if(data.parent != null){
                    $.get( new UploadedFileModel({ id: data.parent}).url(), function( parentData ) {
                        data.parentFilename = parentData.originalFilename;
                        new DetailedUploadedFileTreeDialog({el: "#dialogs", model: data}).render();
                    });
                } else {
                    new DetailedUploadedFileTreeDialog({el: "#dialogs", model: data}).render();
                }
            });
        });

        $("#detailedUploadedFileTreeDialog-"+self.model.id).on('click', ".deleteUploadedFile", function (e) {
            var idUpload = $(e.currentTarget).data("id");
            var idImage = $(e.currentTarget).data("aiid");

            $("#detailedUploadedFileTreeDialog-"+self.model.id).modal('hide');

            DialogModal.initDialogModal(null, idUpload, 'UploadFile', 'Do you want to delete this image ?', 'CONFIRMATIONWARNING', function(){
                var deleteUploadFile = function() {
                    $("#detailedUploadedFileTreeDialog-"+self.model.id).modal('show');
                    new UploadedFileModel({id: idUpload}).destroy({
                        success: function (model, response) {
                            window.app.view.message("Uploaded file", "deleted", "success");
                            self.update();
                        },
                        error: function (model, response) {
                            var json = $.parseJSON(response.responseText);
                            window.app.view.message("Delete failed", json.errors, "error");
                        }
                    });
                };

                if(idImage == null || idImage == 'null'|| idImage == 'undefined') {
                    deleteUploadFile();
                } else {
                    new ImageModel({id: idImage}).destroy({
                        success: function(model, response){
                            deleteUploadFile();
                        },
                        error: function(model, response){
                            var json = $.parseJSON(response.responseText);
                            window.app.view.message("Delete failed", json.errors, "error");
                        }
                    });
                }
            }, function(){
                $("#detailedUploadedFileTreeDialog-"+self.model.id).modal('show');
            });
        });

        $("#detailedUploadedFileTreeDialog-"+self.model.id).modal('show');
    },

    update : function () {
        var self = this;

        if(self.tree != null) {
            $("#detailedUploadedFileTreeDialog-" + self.model.id).find("#treefile").dynatree("destroy");
        }

        $.get( new UploadedFileCollection({ root: self.model.id}).url(), function( data ) {

            var nodes = [];
            var allNodes = [];
            var infos = self.getNodeInfo(data.collection[0]);
            var node = {
                title : infos.title,
                expand : true,
                key : data.collection[0].id,
                noLink : true,
                unselectable : true//,
            };
            nodes.push(node);
            allNodes.push(node);


            for(var i = 1; i<data.collection.length;i++) {

                var parent = $.grep(allNodes, function( n ) {
                    return ( n.key ==  data.collection[i].parentId );
                })[0];

                infos = self.getNodeInfo(data.collection[i]);
                node = {
                    title : infos.title,
                    expand : true,
                    key : data.collection[i].id,
                    noLink : true,
                    unselectable : true//,
                };
                if(parent.children == null) parent.children = [];
                parent.children.push(node);
                allNodes.push(node);
            }

            self.tree = $("#detailedUploadedFileTreeDialog-"+self.model.id).find("#treefile").dynatree({
                children: nodes
            });
        });
    },

    getNodeInfo : function (file) {
        var title = "";
        title += file.originalFilename;

        var model = new UploadedFileModel(file);

        var clazz = "label ";
        var icon = "glyphicon ";

        switch (model.getStatus()) {
            case "UPLOADED":
            case "TO DEPLOY":
            case "TO CONVERT":
                clazz += "label-warning";
                icon += "glyphicon-repeat";
                break;
            case "CONVERTED":
            case "DEPLOYED":
            case "UNCOMPRESSED":
                clazz += "label-success";
                icon += "glyphicon-ok";
                break;
            case "ERROR FORMAT":
            case "ERROR CONVERSION":
            case "ERROR DEPLOYMENT":
                clazz += "label-danger";
                icon += "glyphicon-remove";
                break;
        }

        title += " <span title=\"" + model.getStatus() + "\" class=\"" + clazz + "\" style='margin-left:20px;'> <i class=\"" + icon + "\"></i></span>";

        var size = model.get("size");
        if(size < 1024) {
            size = size + "o";
        } else if(size < Math.pow(1024,2)) {
            size = (size / 1024).toFixed(2) + "Ko";
        } else if(size < Math.pow(1024,3)) {
            size = (size / Math.pow(1024,2)).toFixed(2) + "Mo";
        } else {
            size = (size / Math.pow(1024,3)).toFixed(2) + "Go";
        }

        title += " <span style='margin-left:20px;'>"+size+"</span>";

        title += " <span style='margin-left:20px;'><a class='deleteUploadedFile btn btn-danger' data-id='"+file.id+"' data-aiid='"+file.image+"' style='color:white !important;'><i class='glyphicon glyphicon-trash'></i> Delete</a></span>";

        if(model.getStatus() === "DEPLOYED"){
            title += " <span style='margin-left:20px;'><a href='#' class='previewUploadedFile' data-url='"+file.thumbURL+"' style='color:#428bca !important;'>See preview</a></span>";
        }

        // si format en erreur proposer un refresh

        var lazy = false;
        if(model.getStatus() === "CONVERTED" || model.getStatus() === "UNCOMPRESSED" || model.getStatus() === "ERROR CONVERSION"){
            lazy = true;
        }


        return {title : title, lazy : lazy};
    }
});


/*
*
*                     // we allow deletion of non deployed image after a security gap of 24h.
*                     //ne plus utiliser cela et se baser seulement sur le status
                    /*if(row["to_deploy"] || row["error_format"] || row["error_convert"]){
                        if(($.now() - row["updated"])/3600000 > 24) {
                            result+="<button class='btn btn-info btn-xs deleteimage' id='deleteimage-"+row["image"]+"' data-ufid="+row["id"]+" data-aiid="+row["image"]+">Delete</button>";
                        } else {
                            result+="<button class='btn btn-info btn-xs deleteimage' id='deleteimage-"+row["image"]+"' data-ufid="+row["id"]+" data-aiid="+row["image"]+" disabled>Delete</button>";
                        }
                    }else {
                        result+="<button class='btn btn-info btn-xs deleteimage' id='deleteimage-"+row["image"]+"' data-ufid="+row["id"]+" data-aiid="+row["image"]+" disabled>Delete</button> ";
                        if(row["image"] !== null && window.app.status.user.model.get("adminByNow")){
                            result+="<a class='btn btn-info btn-xs' href='api/abstractimage/"+row["image"]+"/download'> Download</a>";
                        }
                    }*/




/*drawCallback: function() {
    new UploadedFileCollection().fetch({
        success: function(model,response) {

            $.get( "/api/abstractimage/unused.json", function( data ) {
                for(var i = 0; i<data.collection.length;i++) {
                    $('button[id^=deleteimage-][id$=-'+data.collection[i].id+']').prop("disabled",false);
                }
            });
        }
    });
}*/
