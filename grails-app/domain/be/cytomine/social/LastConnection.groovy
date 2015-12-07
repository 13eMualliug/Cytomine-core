package be.cytomine.social

/*
* Copyright (c) 2009-2015. Authors: see NOTICE file.
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

import be.cytomine.CytomineDomain
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

/**
 * Info on last user connection on Cytomine
 * User x connect to poject y the 2013/01/01 at xxhyymin
 */
@RestApiObject(name = "last connection", description = "A Connection is recorded all long the user is connected.")
class LastConnection extends CytomineDomain{

    static mapWith = "mongo"

    @RestApiObjectField(description = "The user")
    SecUser user
    @RestApiObjectField(description = "(mandatory) The project. Null if the user is not in a project")
    Project project

    static constraints = {
        user (nullable:false)
        project (nullable: true)
    }

    static mapping = {
        id(generator: 'assigned', unique: true)
        sort "id"
        compoundIndex date:1, indexAttributes:['expireAfterSeconds':60]
    }
}
