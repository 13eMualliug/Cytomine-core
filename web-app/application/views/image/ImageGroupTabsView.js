/**
 * Created by laurent
 * Date : 02.03.17.
 */
/*
 * Copyright (c) 2009-2016. Authors: see NOTICE file.
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

var ImageGroupTabsView = Backbone.View.extend({
    tagName: "div",
    groups: null, //array of images that are printed
    idProject: null,
    project : null,
    initialize: function (options) {
        this.idProject = options.idProject;
        this.project = options.project;
    },


    render : function() {
        var self = this;
        this.doLayout();
        $("#groupAdd"+self.idProject).click(function() {
            new AddImageGroupToProjectDialog({el: "#dialogs", model: self.project}).render();
        });
        return this;
    },

    doLayout: function () {
        var self = this;
        self.groups = [];
        var isAdmin = window.app.status.currentProjectModel.isAdmin(window.app.models.projectAdmin);
        var table = $(this.el).find("#imageGroupProjectTable" + self.idProject);
        var body = $(this.el).find("#imageGroupProjectArray" + self.idProject);
        var columns = [
            { sClass: 'center', "mData": "id", "bSearchable": false},

            { "mDataProp": "name", sDefaultContent: "", "bSearchable": true,"bSortable": true, "fnRender" : function (o) {
                self.groups.push(o.aData);
                return o.aData["name"];
            }}
            ,
            { "mDataProp": "channel", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                return'<div id="channel-'+o.aData.id+'"></div>';
            }},
            { "mDataProp": "zstack", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                return'<div id="zstack-'+o.aData.id + '"></div>';
            } },
            { "mDataProp": "slice", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                return'<div id="slice-'+o.aData.id + '"></div>';
            }},
            { "mDataProp": "time", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                return'<div id="time-'+o.aData.id + '"></div>';
            }},
            { "mDataProp": "hdf5", sDefaultContent: "", "bSearchable": false,"bSortable": true, "fnRender" : function (o) {
                var grouphdf5 = new ImageGroupHDF5Model({group: o.aData.id, id: 666});
                var toRet = '<div id="con-'+o.aData.id + '"></div>';
                grouphdf5.fetch({
                    success: function (data) {
                        toRet =  "" + data.get("filenames");
                        $(self.el).find("#con-"+o.aData.id).append(toRet);

                    },
                    error: function () {
                        var tt =  '<a href="#imagegroup/convert-<%= id %>">Convert</a> ';
                        toRet = _.template(tt, o.aData);
                        $(self.el).find("#con-"+o.aData.id).append(toRet);

                    }
                });


                return toRet;
            }},
            { "mDataProp": "delete", sDefaultContent: "", "bSearchable": false,"bSortable": true, "fnRender" : function (o) {
                var html =    ' <button class="btn btn-info btn-xs" id="delete-button-<%=  id  %>">Delete</button>';
                return _.template(html, o.aData);
            }}
        ];
        self.imagesdDataTables = table.dataTable({
            "bProcessing": true,
            "bServerSide": true,
            "sAjaxSource": new ImageGroupCollection({project: this.idProject}).url(),
            "fnServerParams": function ( aoData ) {
                aoData.push( { "name": "datatables", "value": "true" } );
            },
            "fnDrawCallback": function(oSettings, json) {

                _.each(self.groups, function(aData) {
                    console.log("AU p");
                    var imageGroup = new ImageGroupModel({});
                    imageGroup.set(aData);
                    $(self.el).find("#delete-button-"+aData.id).click(function () {
                        window.app.controllers.imagegroup.deleteGroup(aData.id);
                    });
                    var cb = function (){
                        $(self.el).find("#channel-"+aData.id).append(imageGroup.channel.toString());
                        $(self.el).find("#zstack-"+aData.id).append(imageGroup.zstack.toString());
                        $(self.el).find("#slice-"+aData.id).append(imageGroup.slice.toString());
                        $(self.el).find("#time-"+aData.id).append(imageGroup.time.toString());
                    };

                    imageGroup.feed(cb);

                });
                self.groups = [];
            },
            "aoColumns" : columns,
            "aaSorting": [[ 0, "desc" ]]

        });

    }


});
