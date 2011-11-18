package be.cytomine.api

import be.cytomine.ontology.Term
import be.cytomine.ontology.Ontology
import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.Exception.CytomineException

class RestTermController extends RestController {

    def springSecurityService
    def transactionService
    def termService

    def list = {
        responseSuccess(termService.list())
    }

    def show = {
        Term term = termService.show(params.id)
        if (term) responseSuccess(term)
        else responseNotFound("Term", params.id)
    }


    def listByOntology = {
        Ontology ontology = Ontology.read(params.idontology)
        if (ontology) responseSuccess(termService.listByOntology(ontology))
        else responseNotFound("Term", "Ontology", params.idontology)
    }

    def listAllByProject = {
        Project project = Project.read(params.idProject)
        if (project && project.ontology) responseSuccess(termService.listAllByProject(project))
        else responseNotFound("Term", "Project", params.idProject)
    }

    def listByImageInstance = {
        ImageInstance image = ImageInstance.read(params.id)
        if (image) responseSuccess(termService.listByImageInstance(image))
        else responseNotFound("Term", "Image", params.id)
    }

    def statProject = {
        Term term = Term.read(params.id)
        if (term)  termService.statProject(term)
        else responseNotFound("Project", params.id)
    }

    def add = {
        try {
            def result = termService.addTerm(request.JSON)
            responseOK(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.message], e.code)
        } finally {
            transactionService.stopIfTransactionInProgress()
        }
    }

    def update = {
        try {
            def result = termService.updateTerm(request.JSON)
            responseOK(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.message], e.code)
        } finally {
            transactionService.stopIfTransactionInProgress()
        }
    }

    def delete = {
        try {
            def result = termService.deleteTerm(params.id)
            responseOK(result)
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.message], e.code)
        } finally {
            transactionService.stopIfTransactionInProgress()
        }
    }

}
