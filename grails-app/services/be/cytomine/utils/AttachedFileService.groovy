package be.cytomine.utils

import be.cytomine.AnnotationDomain

/*
* Copyright (c) 2009-2019. Authors: see NOTICE file.
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

import be.cytomine.command.Command
import be.cytomine.CytomineDomain
import be.cytomine.command.DeleteCommand
import be.cytomine.command.Transaction
import be.cytomine.image.AbstractImage
import be.cytomine.project.Project
import be.cytomine.security.SecUser

import static org.springframework.security.acls.domain.BasePermission.DELETE
import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

class AttachedFileService extends ModelService {

    static transactional = true
    def springSecurityService
    def transactionService
    def commandService
    def cytomineService
    def securityACLService

    def currentDomain() {
        return AttachedFile
    }

    /**
     * List all description, Only for admin
     */
    def list() {
        securityACLService.checkAdmin(cytomineService.currentUser)
        return AttachedFile.list()
    }

    def list(Long domainIdent,String domainClassName) {
        if(domainClassName.contains("AbstractImage")) {
            securityACLService.checkAtLeastOne(domainIdent,domainClassName,"containers",READ)
        } else if(domainClassName.contains("AnnotationDomain")) {
            AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(domainIdent)
            securityACLService.check(domainIdent,annotation.getClass().name,"container",READ)
        } else {
            securityACLService.check(domainIdent,domainClassName,"container",READ)
        }
        return AttachedFile.findAllByDomainIdentAndDomainClassName(domainIdent,domainClassName)
    }


    def read(def id) {
        AttachedFile file = AttachedFile.read(id)
        if(file) {
            if(file.domainClassName.contains("AbstractImage")) {
                securityACLService.checkAtLeastOne(file.domainIdent, file.domainClassName, "containers", READ)
            } else {
                securityACLService.check(file.domainIdent,file.domainClassName,"container",READ)
            }
        }
        file
    }

    def add(String filename,byte[] data,Long domainIdent,String domainClassName) {
        CytomineDomain recipientDomain = Class.forName(domainClassName, false, Thread.currentThread().contextClassLoader).read(domainIdent)
        if(recipientDomain instanceof AbstractImage) {
            securityACLService.checkAtLeastOne(domainIdent, domainClassName, "containers", READ)
        } else if(recipientDomain instanceof Project || !recipientDomain.container() instanceof Project) {
            securityACLService.check(domainIdent,domainClassName,"container",WRITE)
        } else {
            securityACLService.checkFullOrRestrictedForOwner(domainIdent,domainClassName)
        }
        AttachedFile file = new AttachedFile()
        file.domainIdent =  domainIdent
        file.domainClassName = domainClassName
        file.filename = filename
        file.data = data
        saveDomain(file)
        file
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(AttachedFile domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        CytomineDomain recipientDomain = domain.retrieveCytomineDomain()
        if(recipientDomain instanceof AbstractImage) {
            securityACLService.checkAtLeastOne(domain.domainIdent, domain.domainClassName, "containers", READ)
        } else if(recipientDomain instanceof Project || !recipientDomain.container() instanceof Project) {
            securityACLService.check(domain.domainIdent,domain.domainClassName,"container",DELETE)
        } else {
            securityACLService.checkFullOrRestrictedForOwner(domain.domainIdent,domain.domainClassName)
        }
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.domainClassName]
    }

}
