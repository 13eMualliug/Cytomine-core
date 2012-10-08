package be.cytomine

import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.security.User
import be.cytomine.security.UserJob
import be.cytomine.test.BasicInstance
import be.cytomine.test.Infos
import be.cytomine.test.http.AnnotationAPI
import be.cytomine.test.http.AnnotationTermAPI
import be.cytomine.test.http.DomainAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import be.cytomine.ontology.*

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 8/02/11
 * Time: 9:01
 * To change this template use File | Settings | File Templates.
 */
class AnnotationTests extends functionaltestplugin.FunctionalTestCase {

    void testGetAnnotationWithCredential() {
        def annotation = BasicInstance.createOrGetBasicAnnotation()
        def result = AnnotationAPI.show(annotation.id, Infos.GOODLOGIN,Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testListAnnotationsWithCredential() {
        BasicInstance.createOrGetBasicAnnotation()
        def result = AnnotationAPI.list(Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        def json = JSON.parse(result.data)
        assert json instanceof JSONArray
    }

    void testListAnnotationsByImageWithCredential() {
        Annotation annotation = BasicInstance.createOrGetBasicAnnotation()
        def result = AnnotationAPI.listByImage(annotation.image.id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        def json = JSON.parse(result.data)
        assert json instanceof JSONArray
    }

    void testListAnnotationsByImageAndUserWithCredential() {
        Annotation annotation = BasicInstance.createOrGetBasicAnnotation()
        def result = AnnotationAPI.listByImageAndUser(annotation.image.id, annotation.user.id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        def json = JSON.parse(result.data)
        assert json instanceof JSONArray
    }
    void testListAnnotationsByProjectAndTermAndUserWithCredential() {
        AnnotationTerm annotationTerm = BasicInstance.createOrGetBasicAnnotationTerm()
        Infos.addUserRight(Infos.GOODLOGIN,annotationTerm.annotation.project)
        def result = AnnotationAPI.listByProjectAndTerm(annotationTerm.annotation.project.id, annotationTerm.term.id, annotationTerm.annotation.user.id,Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        def json = JSON.parse(result.data)
        //assert json instanceof JSONArray
    }
    
    void testListAnnotationsByProjectAndTermWithUserNullWithCredential() {
        AnnotationTerm annotationTerm = BasicInstance.createOrGetBasicAnnotationTerm()
        def result = AnnotationAPI.listByProjectAndTerm(annotationTerm.annotation.project.id, annotationTerm.term.id, -1, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
    }    

    void testListAnnotationsByProjectAndUsersWithCredential() {
        Annotation annotation = BasicInstance.createOrGetBasicAnnotation()
        def result = AnnotationAPI.listByProjectAndUsers(annotation.project.id, annotation.user.id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        def json = JSON.parse(result.data)
        //assert json instanceof JSONArray
    }

    void testListAnnotationsByTerm() {
        AnnotationTerm annotationTerm = BasicInstance.createOrGetBasicAnnotationTerm()

        def result = AnnotationAPI.listByTerm(annotationTerm.term.id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        def json = JSON.parse(result.data)
        assert json instanceof JSONArray
    }
    
    void testDownloadAnnotationsDocument() {
        AnnotationTerm annotationTerm = BasicInstance.createOrGetBasicAnnotationTerm()
        def result = AnnotationAPI.downloadDocumentByProject(annotationTerm.annotation.project.id,annotationTerm.annotation.user.id,annotationTerm.term.id, annotationTerm.annotation.image.id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
    }

    void testAddAnnotationCorrect() {
        def annotationToAdd = BasicInstance.createOrGetBasicAnnotation()
        def result = AnnotationAPI.create(annotationToAdd.encodeAsJSON(), Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        int idAnnotation = result.data.id

        result = AnnotationAPI.show(idAnnotation, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)

        result = AnnotationAPI.undo()
        assertEquals(200, result.code)

        result = AnnotationAPI.show(idAnnotation, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(404, result.code)

        result = AnnotationAPI.redo()
        assertEquals(200, result.code)

        result = AnnotationAPI.show(idAnnotation, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
    }

    void testAddAnnotationMultipleCorrect() {
        def annotationToAdd1 = BasicInstance.createOrGetBasicAnnotation()
        def annotationToAdd2 = BasicInstance.createOrGetBasicAnnotation()
        def annotations = []
        annotations << JSON.parse(annotationToAdd1.encodeAsJSON())
        annotations << JSON.parse(annotationToAdd2.encodeAsJSON())
        def result = AnnotationAPI.create(annotations.encodeAsJSON() , Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
    }

    void testAddAnnotationCorrectWithoutProject() {
        def annotationToAdd = BasicInstance.createOrGetBasicAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.project = null
        def result = AnnotationAPI.create(updateAnnotation.encodeAsJSON(), Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
    }

    void testAddAnnotationCorrectWithTerm() {
        def annotationToAdd = BasicInstance.createOrGetBasicAnnotation()
        Long idTerm1 = BasicInstance.createOrGetBasicTerm().id
        Long idTerm2 = BasicInstance.createOrGetAnotherBasicTerm().id

        def annotationWithTerm = JSON.parse((String)annotationToAdd.encodeAsJSON())
        annotationWithTerm.term = [idTerm1, idTerm2]

        def result = AnnotationAPI.create(annotationWithTerm.encodeAsJSON(), Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        int idAnnotation = result.data.id

        result = AnnotationAPI.show(idAnnotation, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)

        result = AnnotationAPI.undo()
        assertEquals(200, result.code)

        result = AnnotationAPI.show(idAnnotation, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(404, result.code)

        result = AnnotationAPI.redo()
        assertEquals(200, result.code)

        result = AnnotationAPI.show(idAnnotation, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
    }

    void testAddAnnotationBadGeom() {
        def annotationToAdd = BasicInstance.createOrGetBasicAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = 'POINT(BAD GEOMETRY)'

        Long idTerm1 = BasicInstance.createOrGetBasicTerm().id
        Long idTerm2 = BasicInstance.createOrGetAnotherBasicTerm().id
        updateAnnotation.term = [idTerm1, idTerm2]

        def result = AnnotationAPI.create(updateAnnotation.encodeAsJSON(), Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(400, result.code)
    }

    void testAddAnnotationBadGeomEmpty() {
        def annotationToAdd = BasicInstance.createOrGetBasicAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = 'POLYGON EMPTY'
        def result = AnnotationAPI.create(updateAnnotation.encodeAsJSON(), Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(400, result.code)
    }

    void testAddAnnotationBadGeomNull() {
        def annotationToAdd = BasicInstance.createOrGetBasicAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = null
        def result = AnnotationAPI.create(updateAnnotation.encodeAsJSON(), Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(400, result.code)
    }

    void testAddAnnotationImageNotExist() {
        def annotationToAdd = BasicInstance.createOrGetBasicAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.image = -99
        def result = AnnotationAPI.create(updateAnnotation.encodeAsJSON(), Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(400, result.code)
    }

    void testEditAnnotation() {
        Annotation annotationToAdd = BasicInstance.createOrGetBasicAnnotation()
        def result = AnnotationAPI.update(annotationToAdd, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idAnnotation = json.annotation.id

        def showResult = AnnotationAPI.show(idAnnotation, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstance.compareAnnotation(result.mapNew, json)

        showResult = AnnotationAPI.undo()
        assertEquals(200, result.code)
        showResult = AnnotationAPI.show(idAnnotation, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        BasicInstance.compareAnnotation(result.mapOld, JSON.parse(showResult.data))

        showResult = AnnotationAPI.redo()
        assertEquals(200, result.code)
        showResult = AnnotationAPI.show(idAnnotation, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        BasicInstance.compareAnnotation(result.mapNew, JSON.parse(showResult.data))
    }

    void testEditAnnotationNotExist() {
        Annotation annotationToAdd = BasicInstance.createOrGetBasicAnnotation()
        Annotation annotationToEdit = Annotation.get(annotationToAdd.id)
        def jsonAnnotation = JSON.parse((String)annotationToEdit.encodeAsJSON())
        jsonAnnotation.id = "-99"
        def result = AnnotationAPI.update(annotationToAdd.id, jsonAnnotation.encodeAsJSON(), Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(404, result.code)
    }

    void testEditAnnotationWithBadGeometry() {
        Annotation annotationToAdd = BasicInstance.createOrGetBasicAnnotation()
        def jsonAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        jsonAnnotation.location = "POINT (BAD GEOMETRY)"
        def result = AnnotationAPI.update(annotationToAdd.id, jsonAnnotation.encodeAsJSON(), Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(400, result.code)
    }

    void testDeleteAnnotation() {
        def annotationToDelete = BasicInstance.getBasicAnnotationNotExist()
        assert annotationToDelete.save(flush: true)  != null
        def id = annotationToDelete.id
        def result = AnnotationAPI.delete(id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)

        def showResult = AnnotationAPI.show(id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(404, showResult.code)

        result = AnnotationAPI.undo()
        assertEquals(200, result.code)

        result = AnnotationAPI.show(id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)

        result = AnnotationAPI.redo()
        assertEquals(200, result.code)

        result = AnnotationAPI.show(id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(404, result.code)
    }

    void testDeleteAnnotationNotExist() {
        def result = AnnotationAPI.delete(-99, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(404, result.code)
    }

    void testDeleteAnnotationWithData() {
        def annotTerm = BasicInstance.createOrGetBasicAnnotationTerm()
        def annotationToDelete = annotTerm.annotation
        def result = AnnotationAPI.delete(annotationToDelete.id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
    }

    void testCopyAnnotationFromUser() {
        //create annotation with user = userjob
        def annotationTerm = BasicInstance.createOrGetBasicAnnotationTerm()
        def annotation = annotationTerm.annotation
        annotation.user = BasicInstance.createOrGetBasicUser()
        assert annotation.save(flush: true)  != null

        //call service to allow copy annotation
        def result = AnnotationAPI.copy(annotation.id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)

        //check annotation
        int idAnnotation = result.data.id
        result = AnnotationAPI.show(idAnnotation, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)

        //check term is added!
        println "idAnnotation : $idAnnotation, annotationTerm.term.id : $annotationTerm.term.id"
        println "annotations : " + AnnotationTerm.findAllByAnnotationAndTerm(Annotation.read(idAnnotation), Term.read(annotationTerm.term.id))
        result = AnnotationTermAPI.showAnnotationTerm(idAnnotation,annotationTerm.term.id,User.findByUsername(Infos.GOODLOGIN).id,Infos.GOODLOGIN,Infos.GOODPASSWORD)
        assertEquals(200,result.code)
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject

    }

    void testListingAnntotationWithoutTerm() {
        //create annotation without term
        User user = BasicInstance.getNewUser()
        Project project = BasicInstance.getBasicProjectNotExist()
        Ontology ontology = BasicInstance.createOrGetBasicOntology()
        project.ontology = ontology
        project.save(flush: true)
        Infos.addUserRight(user.username,project)
        ImageInstance image = BasicInstance.getBasicImageInstanceNotExist()
        image.project = project
        image.save(flush: true)


        Annotation annotationWithoutTerm = BasicInstance.getBasicAnnotationNotExist()
        annotationWithoutTerm.project = project
        annotationWithoutTerm.image = image
        annotationWithoutTerm.user = user
        assert annotationWithoutTerm.save(flush: true)

        AnnotationTerm at = BasicInstance.getBasicAnnotationTermNotExist("")
        at.term.ontology = ontology
        at.term.save(flush: true)
        at.user = user
        at.save(flush: true)
        Annotation annotationWithTerm = at.annotation
        annotationWithTerm.user = user
        annotationWithTerm.project = project
        annotationWithTerm.image = image
        assert annotationWithTerm.save(flush: true)

        AnnotationTerm at2 = BasicInstance.getBasicAnnotationTermNotExist("")
        at2.term.ontology = ontology
        at2.term.save(flush: true)
        at2.user = BasicInstance.getOldUser()
        at2.save(flush: true)
        Annotation annotationWithTermFromOtherUser = at.annotation
        annotationWithTermFromOtherUser.user = user
        annotationWithTermFromOtherUser.project = project
        annotationWithTermFromOtherUser.image = image
        assert annotationWithTermFromOtherUser.save(flush: true)

        //list annotation without term with this user
        def result = AnnotationAPI.listByProjectAndUsersWithoutTerm(project.id, user.id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        def json = JSON.parse(result.data)
        assert json instanceof JSONArray

        assert DomainAPI.containsInJSONList(annotationWithoutTerm.id,json)
        assert !DomainAPI.containsInJSONList(annotationWithTerm.id,json)
    }

    void testListingAnntotationWithoutTermAlgo() {
        //create annotation without term
        UserJob user = BasicInstance.createOrGetBasicUserJob()
        Project project = BasicInstance.getBasicProjectNotExist()
        Ontology ontology = BasicInstance.createOrGetBasicOntology()
        project.ontology = ontology
        project.save(flush: true)
        try {Infos.addUserRight(user.user,project) }catch(Exception e){println e}
        ImageInstance image = BasicInstance.getBasicImageInstanceNotExist()
        image.project = project
        image.save(flush: true)

        Annotation annotationWithoutTerm = BasicInstance.getBasicAnnotationNotExist()
        annotationWithoutTerm.project = project
        annotationWithoutTerm.image = image
        annotationWithoutTerm.user = user
        assert annotationWithoutTerm.save(flush: true)

        AlgoAnnotationTerm at = BasicInstance.getBasicAlgoAnnotationTermNotExist()
        at.term.ontology = ontology
        at.term.save(flush: true)
        at.userJob = user
        at.save(flush: true)
        Annotation annotationWithTerm = at.annotation
        annotationWithTerm.user = user
        annotationWithTerm.project = project
        annotationWithTerm.image = image
        assert annotationWithTerm.save(flush: true)

        //list annotation without term with this user
        def result = AnnotationAPI.listByProjectAndUsersWithoutTerm(project.id, user.id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        def json = JSON.parse(result.data)
        assert json instanceof JSONArray

        assert DomainAPI.containsInJSONList(annotationWithoutTerm.id,json)
        assert !DomainAPI.containsInJSONList(annotationWithTerm.id,json)
    }


    void testListingAnntotationWithSeveralTerm() {
        //create annotation without term
        User user = BasicInstance.getNewUser()
        Project project = BasicInstance.getBasicProjectNotExist()
        Ontology ontology = BasicInstance.createOrGetBasicOntology()
        project.ontology = ontology
        project.save(flush: true)
        Infos.addUserRight(user.username,project)
        ImageInstance image = BasicInstance.getBasicImageInstanceNotExist()
        image.project = project
        image.save(flush: true)

        //annotation with no multiple term
        Annotation annotationWithNoTerm = BasicInstance.getBasicAnnotationNotExist()
        annotationWithNoTerm.project = project
        annotationWithNoTerm.image = image
        annotationWithNoTerm.user = user
        assert annotationWithNoTerm.save(flush: true)

        //annotation with multiple term
        AnnotationTerm at = BasicInstance.getBasicAnnotationTermNotExist("")
        at.term.ontology = ontology
        at.term.save(flush: true)
        at.user = user
        at.save(flush: true)
        Annotation annotationWithMultipleTerm = at.annotation
        annotationWithMultipleTerm.user = user
        annotationWithMultipleTerm.project = project
        annotationWithMultipleTerm.image = image
        assert annotationWithMultipleTerm.save(flush: true)
        AnnotationTerm at2 = BasicInstance.getBasicAnnotationTermNotExist("")
        at2.term.ontology = ontology
        at2.term.save(flush: true)
        at2.user = user
        at2.annotation=annotationWithMultipleTerm
        at2.save(flush: true)

        //list annotation without term with this user
        def result = AnnotationAPI.listByProjectAndUsersSeveralTerm(project.id, user.id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        def json = JSON.parse(result.data)
        assert json instanceof JSONArray

        assert !DomainAPI.containsInJSONList(annotationWithNoTerm.id,json)
        assert DomainAPI.containsInJSONList(annotationWithMultipleTerm.id,json)
    }

    void testListingAnntotationWithSeveralTermAlgo() {
        //create annotation without term
        UserJob user = BasicInstance.createOrGetBasicUserJob()
        Project project = BasicInstance.getBasicProjectNotExist()
        Ontology ontology = BasicInstance.createOrGetBasicOntology()
        project.ontology = ontology
        project.save(flush: true)
        try {Infos.addUserRight(user.user,project) }catch(Exception e){println e}
        ImageInstance image = BasicInstance.getBasicImageInstanceNotExist()
        image.project = project
        image.save(flush: true)

        //annotation with no multiple term
        Annotation annotationWithNoTerm = BasicInstance.getBasicAnnotationNotExist()
        annotationWithNoTerm.project = project
        annotationWithNoTerm.image = image
        annotationWithNoTerm.user = user
        assert annotationWithNoTerm.save(flush: true)

        //annotation with multiple term
        AlgoAnnotationTerm at = BasicInstance.getBasicAlgoAnnotationTermNotExist()
        at.term.ontology = ontology
        at.term.save(flush: true)
        at.userJob = user
        at.save(flush: true)
        Annotation annotationWithMultipleTerm = at.annotation
        annotationWithMultipleTerm.user = user
        annotationWithMultipleTerm.project = project
        annotationWithMultipleTerm.image = image
        assert annotationWithMultipleTerm.save(flush: true)
        AlgoAnnotationTerm at2 = BasicInstance.getBasicAlgoAnnotationTermNotExist()
        at2.term.ontology = ontology
        at2.term.save(flush: true)
        at2.userJob = user
        at2.annotation=annotationWithMultipleTerm
        at2.save(flush: true)

        //list annotation without term with this user
        def result = AnnotationAPI.listByProjectAndUsersSeveralTerm(project.id, user.id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        def json = JSON.parse(result.data)
        assert json instanceof JSONArray

        assert !DomainAPI.containsInJSONList(annotationWithNoTerm.id,json)
        assert DomainAPI.containsInJSONList(annotationWithMultipleTerm.id,json)
    }

}
