package be.cytomine.image

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.SecurityCheck
import be.cytomine.command.AddCommand
import be.cytomine.command.DeleteCommand
import be.cytomine.security.Group
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.security.access.annotation.Secured

import be.cytomine.command.Transaction
import grails.converters.JSON
import be.cytomine.security.UserGroup

class AbstractImageGroupService extends ModelService {

    /**
     * CRUD operation for this domain will be undo/redo-able
     */

    def cytomineService
    def responseService
    def transactionService

    def get(AbstractImage abstractimage, Group group) {
        AbstractImageGroup.findByAbstractimageAndGroup(abstractimage, group)
    }

    @Secured(['ROLE_USER'])
    def list(user) {
        def groups = UserGroup.findByUser(user).collect{it.group}
        return AbstractImageGroup.findAllByGroupInList(groups)
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @param security Security service object (user for right check)
     * @return Response structure (created domain data,..)
     */
    @Secured(['ROLE_ADMIN'])
    def add(def json, SecurityCheck security) throws CytomineException {
        SecUser currentUser = cytomineService.getCurrentUser()
        json.user = currentUser.id
        return executeCommand(new AddCommand(user: currentUser), json)
    }

    /**
     * Delete domain in argument
     * @param json JSON that was passed in request parameter
     * @param security Security service object (user for right check)
     * @return Response structure (created domain data,..)
     */
    @Secured(['ROLE_ADMIN'])
    def delete(def json,SecurityCheck security) throws CytomineException {
        Transaction transaction = transactionService.start()
        delete(retrieve(json),transaction)
    }

    def delete(AbstractImageGroup aig, Transaction transaction = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        def json = JSON.parse("{abstractimage: ${aig.abstractimage.id},group:${aig.group.id}}")
        return executeCommand(new DeleteCommand(user: currentUser,transaction:transaction), json)
    }

    /**
     * Create new domain in database
     * @param json JSON data for the new domain
     * @param printMessage Flag to specify if confirmation message must be show in client
     * Usefull when we create a lot of data, just print the root command message
     * @return Response structure (status, object data,...)
     */
    def create(JSONObject json, boolean printMessage) {
        create(AbstractImageGroup.createFromDataWithId(json), printMessage)
    }

    /**
     * Create new domain in database
     * @param domain Domain to store
     * @param printMessage Flag to specify if confirmation message must be show in client
     * @return Response structure (status, object data,...)
     */
    def create(AbstractImageGroup domain, boolean printMessage) {
        //Save new object
        saveDomain(domain)
        //Build response message
        return responseService.createResponseMessage(domain, [domain.id, domain.abstractimage.filename, domain.group.name], printMessage, "Add", domain.getCallBack())
    }

    /**
     * Destroy domain from database
     * @param json JSON with domain data (to retrieve it)
     * @param printMessage Flag to specify if confirmation message must be show in client
     * @return Response structure (status, object data,...)
     */
    def destroy(JSONObject json, boolean printMessage) {
        //Get object to delete
        destroy(retrieve(json), printMessage)
    }

    /**
     * Destroy domain from database
     * @param domain Domain to remove
     * @param printMessage Flag to specify if confirmation message must be show in client
     * @return Response structure (status, object data,...)
     */
    def destroy(AbstractImageGroup domain, boolean printMessage) {
        //Build response message
        def response = responseService.createResponseMessage(domain, [domain.id, domain.abstractimage.filename, domain.group.name], printMessage, "Delete", domain.getCallBack())
        //Delete object
        removeDomain(domain)
        return response
    }

    /**
     * Create domain from JSON object
     * @param json JSON with new domain info
     * @return new domain
     */
    AbstractImageGroup createFromJSON(def json) {
        return AbstractImageGroup.createFromData(json)
    }

    /**
     * Retrieve domain thanks to a JSON object
     * @param json JSON with new domain info
     * @return domain retrieve thanks to json
     * TODO: secure!
     */
    def retrieve(JSONObject json) {
        AbstractImage abstractimage = AbstractImage.get(json.abstractimage)
        Group group = Group.get(json.group)
        AbstractImageGroup domain = AbstractImageGroup.findByAbstractimageAndGroup(abstractimage, group)
        if (!domain) {
            throw new ObjectNotFoundException("AbstractImageGroup group=${json.group} image=${json.abstractimage} not found")
        }
        return domain
    }
}
