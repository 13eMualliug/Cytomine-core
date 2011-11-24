package be.cytomine.command.user

import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.command.EditCommand
import be.cytomine.command.UndoRedoCommand
import be.cytomine.security.User
import grails.converters.JSON

class EditUserCommand extends EditCommand implements UndoRedoCommand {

    def execute() {
        //Retrieve domain
        User updatedDomain = User.get(json.id)
        if (!updatedDomain) throw new ObjectNotFoundException("User ${json.id} not found")
        def oldDomain = updatedDomain.encodeAsJSON()
        updatedDomain.getFromData(updatedDomain, json)
        //Validate and save domain
        domainService.editDomain(updatedDomain,json)
        //Build response message
        String message = createMessage(updatedDomain, [updatedDomain.id, updatedDomain.username])
        //Init command info
        fillCommandInfo(updatedDomain,oldDomain,message)
        //Create and return response
        return responseService.createResponseMessage(updatedDomain,message,printMessage)
    }

    def undo() {
        return edit(userService,JSON.parse(data).previousUser)
    }

    def redo() {
        return edit(userService,JSON.parse(data).newUser)
    }
}
