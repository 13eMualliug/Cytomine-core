package be.cytomine.ontology

import be.cytomine.ModelService
import be.cytomine.command.suggestedTerm.AddSuggestedTermCommand
import be.cytomine.command.suggestedTerm.DeleteSuggestedTermCommand
import be.cytomine.processing.Job
import be.cytomine.project.Project
import be.cytomine.security.User
import grails.converters.JSON

class SuggestedTermService extends ModelService {

    static transactional = true
    def cytomineService
    def commandService

    def list() {
        SuggestedTerm.list()
    }

    def list(Annotation annotation) {
        SuggestedTerm.findAllByAnnotation(annotation)
    }

    def read(Annotation annotation, Term term, Job job) {
        SuggestedTerm.findWhere(annotation: annotation, term: term, job: job)
    }

    def listWorst(Project project, def max) {
        List<SuggestedTerm> results = new ArrayList<SuggestedTerm>()
        List<SuggestedTerm> suggest = SuggestedTerm.findAllByProject(project, [sort: "rate", order: "desc"])

        for (int i = 0; i < suggest.size() && max > results.size(); i++) {
            if (suggest.get(i).annotationMapWithBadTerm())
                results.add(suggest.get(i));
        }
        return results

    }

    def listWorstTerm(Project project, def max) {
        Map<Term, Integer> termMap = new HashMap<Term, Integer>()
        List<Term> termList = Term.findAllByOntology(project.ontology)
        termList.each {termMap.put(it, 0)}

        List<SuggestedTerm> suggest = SuggestedTerm.findAllByProject(project, [sort: "rate", order: "desc"])

        for (int i = 0; i < suggest.size(); i++) {
            if (suggest.get(i).annotationMapWithBadTerm()) {
                Term term = suggest.get(i).term
                termMap.put(term, termMap.get(term) + 1);
            }
        }
        termList.clear()
        termMap.each {  key, value ->
            key.rate = value
            termList.add(key)
        }
        return termList
    }

    def add(def json) {
        User currentUser = cytomineService.getCurrentUser()
        commandService.processCommand(new AddSuggestedTermCommand(user: currentUser), json)
    }

    def delete(def json) {
        User currentUser = cytomineService.getCurrentUser()
        def result = deleteSuggestedTerm(json.idannotation, json.idterm, json.idjob, currentUser)
        return result
    }

    def update(def json) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Delete an annotation term
     */
    def deleteSuggestedTerm(def idAnnotation, def idTerm, def idJob, User currentUser) {
        def json = JSON.parse("{annotation: $idAnnotation, term: $idTerm, job: $idJob}")
        def result = commandService.processCommand(new DeleteSuggestedTermCommand(user: currentUser), json)
        return result
    }

    /**
     * Delete all term map for annotation
     */
    def deleteSuggestedTermFromAllUser(Annotation annotation, User currentUser) {
        //Delete all annotation term
        def suggestedterm = SuggestedTerm.findAllByAnnotation(annotation)
        log.info "Delete old suggestedterm= " + suggestedterm.size()

        suggestedterm.each { sugterm ->
            log.info "unlink sugterm:" + sugterm.id
            deleteSuggestedterm(sugterm.annotation.id, sugterm.term.id, sugterm.job.id, currentUser)
        }
    }

    /**
     * Delete all term map by user for term
     */
    def deleteSuggestedTermFromAllUser(Term term, User currentUser) {
        //Delete all annotation term
        def suggestedterm = SuggestedTerm.findAllByTerm(term)
        log.info "Delete old suggestedterm= " + suggestedterm.size()

        suggestedterm.each { sugterm ->
            log.info "unlink sugterm:" + sugterm.id
            deleteSuggestedterm(sugterm.annotation.id, sugterm.term.id, sugterm.job.id, currentUser)
        }
    }

    /**
     * Restore domain which was previously deleted
     * @param json domain info
     * @param commandType command name (add/delete/...) which execute this method
     * @param printMessage print message or not
     * @return response
     */
    def restore(def json, String commandType, boolean printMessage) {
        //Rebuilt object that was previoulsy deleted
        def domain = SuggestedTerm.createFromDataWithId(json)
        //Build response message
        def response = responseService.createResponseMessage(domain, [domain.term.name, domain.annotation.id, domain.job?.software?.name], printMessage, commandType)
        //Save new object
        domain.save(flush: true)
        return response
    }

    /**
     * Destroy domain which was previously added
     * @param json domain info
     * @param commandType command name (add/delete/...) which execute this method
     * @param printMessage print message or not
     * @return response
     */
    def destroy(def json, String commandType, boolean printMessage) {
        //Get object to delete
        def domain = SuggestedTerm.get(json.id)
        //Build response message
        def response = responseService.createResponseMessage(domain, [domain.term.name, domain.annotation.id, domain.job?.software?.name], printMessage, commandType)
        //Delete object
        domain.delete(flush: true)
        return response
    }

    /**
     * Edit domain which was previously edited
     * @param json domain info
     * @param commandType command name (add/delete/...) which execute this method
     * @param printMessage print message or not
     * @return response
     */
    def edit(def json, String commandType, boolean printMessage) {
        //Rebuilt previous state of object that was previoulsy edited
        def domain = fillDomainWithData(new SuggestedTerm(), json)
        //Build response message
        def response = responseService.createResponseMessage(domain, [domain.term.name, domain.annotation.id, domain.job?.software?.name], printMessage, commandType)
        //Save update
        domain.save(flush: true)
        return response
    }
}
