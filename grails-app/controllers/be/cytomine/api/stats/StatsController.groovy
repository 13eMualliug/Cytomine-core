package be.cytomine.api.stats

import be.cytomine.Exception.WrongArgumentException

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

import be.cytomine.api.RestController
import be.cytomine.ontology.AnnotationTerm
import be.cytomine.ontology.Term
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

import static org.springframework.security.acls.domain.BasePermission.READ

class StatsController extends RestController {

    def cytomineService
    def securityACLService
    def termService
    def jobService
    def secUserService

    /**
     * Compute for each user, the number of annotation of each term
     */
    def statUserAnnotations = {

        Map<Long, Object> result = new HashMap<Long, Object>()

        //Get project
        Project project = Project.read(params.id)
        if (project == null) {
            responseNotFound("Project", params.id)
            return
        }

        securityACLService.check(project,READ)

        //Get project terms
        def terms = Term.findAllByOntology(project.getOntology())
        if(terms.isEmpty()) {
            responseSuccess([])
            return
        }

        //compute number of annotation for each user and each term
        def nbAnnotationsByUserAndTerms = AnnotationTerm.createCriteria().list {
            inList("term", terms)
            join("userAnnotation")
            createAlias("userAnnotation", "a")
            projections {
                eq("a.project", project)
                groupProperty("a.user.id")
                groupProperty("term.id")
                count("term")
            }
        }

        //build empty result table
        secUserService.listUsers(project).each { user ->
            def item = [:]
            item.id = user.id
            item.key = user.firstname + " " + user.lastname
            item.terms = []
            terms.each { term ->
                def t = [:]
                t.id = term.id
                t.name = term.name
                t.color = term.color
                t.value = 0
                item.terms << t
            }
            result.put(user.id, item)
        }

        //complete stats for each user and term
        nbAnnotationsByUserAndTerms.each { stat ->
            def user = result.get(stat[0])
            if(user) {
                user.terms.each {
                    if (it.id == stat[1]) {
                        it.value = stat[2]
                    }
                }
            }
        }
        responseSuccess(result.values())
    }

    /**
     * Compute number of annotation for each user
     */
    def statUser = {

        Map<Long, Object> result = new HashMap<Long, Object>()

        //Get project
        Project project = Project.read(params.id)
        if (!project) {
            responseNotFound("Project", params.id)
            return
        }

        securityACLService.check(project,READ)

        //compute number of annotation for each user
        def userAnnotations = UserAnnotation.createCriteria().list {
            eq("project", project)
            join("user")  //right join possible ? it will be sufficient
            projections {
                countDistinct('id')
                groupProperty("user.id")
            }
        }

        //build empty result table
        secUserService.listLayers(project).each { user ->
            def item = [:]
            item.id = user.id
            item.key = user.firstname + " " + user.lastname
            item.username = user.username
            item.value = 0
            result.put(item.id, item)
        }

        //fill result table with number of annotation
        userAnnotations.each { item ->
            def user = result.get(item[1])
            if(user) user.value = item[0]
        }

        responseSuccess(result.values())
    }

    /**
     * Compute the number of annotation for each term
     */
    def statTerm = {

        //Get project
        Project project = Project.read(params.id)
        if (project == null) {
            responseNotFound("Project", params.id)
            return
        }

        securityACLService.check(project,READ)

        //Get leaf term (parent term cannot be map with annotation)
        def terms = project.ontology.leafTerms()

        //Get the number of annotation for each term
        def numberOfAnnotationForEachTerm = UserAnnotation.executeQuery('select t.term.id, count(t) from AnnotationTerm as t, UserAnnotation as b where b.id=t.userAnnotation.id and b.project = ? group by t.term.id', [project])

        def stats = [:]
        def color = [:]
        def ids = [:]
        def idsRevert = [:]
        def list = []

        //build empty result table
        terms.each { term ->
                stats[term.name] = 0
                color[term.name] = term.color
                ids[term.name] = term.id
                idsRevert[term.id] = term.name
        }

        //init result table with data
        numberOfAnnotationForEachTerm .each { result ->
            def name = idsRevert[result[0]]
            if(name) stats[name]=result[1]
        }

        //fill results stats tabble
        stats.each {
            list << ["id": ids.get(it.key), "key": it.key, "value": it.value, "color": color.get(it.key)]
        }
        responseSuccess(list)
    }

    /**
     * Compute the number of annotation for each sample and for each term
     */
    def statTermSlide = {

        Map<Long, Object> result = new HashMap<Long, Object>()

        //Get project
        Project project = Project.read(params.id)
        if (project == null) {
            responseNotFound("Project", params.id)
            return
        }


        securityACLService.check(project,READ)

        //Get project term
        def terms = Term.findAllByOntology(project.getOntology())

        //Check if there are user layers
        def userLayers = secUserService.listLayers(project)
        if(terms.isEmpty() || userLayers.isEmpty()) {
            responseSuccess([])
            return
        }

        def annotationsNumber = AnnotationTerm.createCriteria().list {
            inList("term", terms)
            inList("user", userLayers)
            join("userAnnotation")
            createAlias("userAnnotation", "a")
            projections {
                eq("a.project", project)
                groupProperty("a.image.id")
                groupProperty("term.id")
                count("term.id")
            }
        }

        //build empty result table
        terms.each { term ->
            def item = [:]
            item.id = term.id
            item.key = term.name
            item.value = 0
            item.color = term.color
            result.put(item.id, item)
        }

        //Fill result table
        annotationsNumber.each { item ->
            def term = item[1]
            result.get(term).value++;
        }
        responseSuccess(result.values())
    }

    /**
     * For each user, compute the number of sample where he made annotation
     */
    def statUserSlide = {
        Project project = Project.read(params.id)
        if (!project) {
            responseNotFound("Project", params.id)
            return
        }

        securityACLService.check(project,READ)

        def terms = Term.findAllByOntology(project.getOntology())
        if(terms.isEmpty()) {
            responseSuccess([])
            return
        }
        Map<Long, Object> result = new HashMap<Long, Object>()

        //numberOfAnnotationsByUserAndImage[0] = id image, numberOfAnnotationsByUserAndImage[1] = user, numberOfAnnotationsByUserAndImage[2] = number of annotation
        def numberOfAnnotationsByUserAndImage = AnnotationTerm.createCriteria().list {
            inList("term", terms)
            join("userAnnotation")
            createAlias("userAnnotation", "a")
            projections {
                eq("a.project", project)
                groupProperty("a.image.id")
                groupProperty("a.user")
                count("a.user")
            }
        }

        //build empty result table
        secUserService.listLayers(project).each { user ->
            def item = [:]
            item.id = user.id
            item.key = user.firstname + " " + user.lastname
            item.value = 0
            result.put(item.id, item)
        }

        //Fill result table
        numberOfAnnotationsByUserAndImage.each { item ->
            def user = result.get(item[1].id)
            if(user) user.value++;
        }

        responseSuccess(result.values())
    }

    /**
     * Compute user annotation number evolution over the time for a project (start = project creation, stop = today)
     * params.daysRange = number of days between each measure
     * param.term = (optional) filter on a specific term
     */
    def statAnnotationEvolution = {

        Project project = Project.read(params.id)
        if (project == null) {
            responseNotFound("Project", params.id)
            return
        }

        securityACLService.check(project,READ)

        int daysRange = params.daysRange!=null ? params.getInt('daysRange') : 1
        Term term = Term.read(params.getLong('term'))

        def data = []
        int count = 0;

        def annotations = null;
        if(term) {
            log.info "Search on term " + term.name
            //find all annotation user for this project and this term
            annotations = UserAnnotation.executeQuery("select b.created from UserAnnotation b where b.project = ? and b.id in (select x.userAnnotation.id from AnnotationTerm x where x.term = ?) order by b.created desc", [project,term])
        }
        else {
            //find all annotation user for this project
            annotations = UserAnnotation.executeQuery("select a.created from UserAnnotation a where a.project = ? order by a.created desc", [project])
        }

        //start a the project creation and stop today
        Date creation = project.created
        Date current = new Date()

        //for each day (step = daysRange), compute annotation number
        //start at the end date until the begining
        while(current.getTime()>=creation.getTime()) {

            def item = [:]
            while(count<annotations.size()) {
                //compute each annotation until the next step
                if(annotations.get(count).getTime()<current.getTime()) break;
                count++;
            }

            item.date = current.getTime()
            item.size = annotations.size()-count;
            data << item

            //add a new step
            Calendar cal = Calendar.getInstance();
            cal.setTime(current);
            cal.add(Calendar.DATE, -daysRange);
            current = cal.getTime();
        }
        responseSuccess(data)
    }

    @RestApiMethod(description="Get the total of annotations with a term by project.")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The term id")
    ])
    def statAnnotationTermedByProject() {
        Term term = Term.read(params.id)
        if (!term) {
            responseNotFound("Term", params.id)
            return
        }
        securityACLService.check(term.container(),READ)
        def projects = Project.findAllByOntology(term.ontology)
        def count = [:]
        def percentage = [:]

        //init list
        projects.each { project ->
            count[project.name] = 0
            percentage[project.name] = 0
        }

        projects.each { project ->
            def layers = secUserService.listLayers(project)
            if(!layers.isEmpty()) {
                def annotations = UserAnnotation.createCriteria().list {
                    eq("project", project)
                    inList("user", layers)
                }
                annotations.each { annotation ->
                    if (annotation.terms().contains(term)) {
                        count[project.name] = count[project.name] + 1;
                    }
                }
            }
        }

        def list = []
        count.each {
            list << ["key": it.key, "value": it.value]
        }
        responseSuccess(list)
    }

    @RestApiMethod(description="Get the total of the domains made on this instance.")
    @RestApiParams(params=[
            @RestApiParam(name="domain", type="string", paramType = RestApiParamType.PATH, description = "The domain name")
    ])
    def totalDomains() {

        securityACLService.checkAdmin(cytomineService.getCurrentUser())
        def clazz = grailsApplication.domainClasses.find { it.clazz.simpleName.toLowerCase() == params.domain.toLowerCase() }
        if(!clazz){
            throw new WrongArgumentException("This domain doesn't exist!")
        }
        Integer total = clazz.clazz.count

        responseSuccess(["total" : total]);
    }
}
