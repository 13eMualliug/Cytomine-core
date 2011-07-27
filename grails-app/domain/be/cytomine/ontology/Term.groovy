package be.cytomine.ontology

import grails.converters.JSON
import be.cytomine.SequenceDomain
import org.perf4j.StopWatch
import org.perf4j.LoggingStopWatch

class Term extends SequenceDomain implements Serializable {

    String name
    String comment

    Ontology ontology
    String color

    static belongsTo = [ontology:Ontology]

    //static belongsTo = Annotation
    static hasMany = [annotationTerm:AnnotationTerm,relationTerm1:RelationTerm, relationTerm2:RelationTerm]

    //must be done because RelationTerm has two Term attribute
    static mappedBy = [relationTerm1:'term1', relationTerm2:'term2']

    static constraints = {
        comment(blank:true,nullable:true)
    }
    static mapping = {
        id (generator:'assigned', unique : true)
    }

    def annotations() {
        return annotationTerm.collect{it.annotation}
    }

    def relationAsTerm1() {
        def relations = []
        relationTerm1.each {
            def map = [:]
            map.put(it.relation,it.term2)
            relations.add(map)
        }
        return relations
    }

    def hasChildren() {
        boolean hasChildren=false
        this.relationTerm1.each {
            if(it.getRelation().getName().equals(RelationTerm.names.PARENT)) {
                hasChildren=true
                return
            }
        }
        return hasChildren
    }

    def isRoot() {
        def isRoot = true;
        this.relationTerm2.each {
            isRoot &= (it.getRelation().getName() != RelationTerm.names.PARENT)
        }
        return isRoot
    }

    def isChild() {
        def isChild = false;
        this.relationTerm2.each {
            isChild |= (it.getRelation().getName() == RelationTerm.names.PARENT)
        }
        return isChild
    }

    def relationAsTerm2() {
        def relations = []
        relationTerm2.each {
            def map = [:]
            map.put(it.relation,it.term1)
            relations.add(map)
        }
        return relations
    }

    static Term createFromData(jsonTerm) {
        def term = new Term()
        getFromData(term,jsonTerm)
    }

    static Term getFromData(term,jsonTerm) {
        if(!jsonTerm.name.toString().equals("null"))
            term.name = jsonTerm.name
        else throw new IllegalArgumentException("Term name cannot be null")
        term.comment = jsonTerm.comment

        String ontologyId = jsonTerm.ontology.toString()
        if(!ontologyId.equals("null")) {
            term.ontology = Ontology.get(ontologyId)
            if(term.ontology==null) throw new IllegalArgumentException("Ontology was not found with id:"+ ontologyId)
        }
        else term.ontology = null

        term.color = jsonTerm.color
        return term;
    }

    def getIdOntology() {
        if(this.ontologyId) return this.ontologyId
        else return this.ontology?.id
    }

    static void registerMarshaller() {
        println "Register custom JSON renderer for " + Term.class
        JSON.registerObjectMarshaller(Term) {
            def returnArray = [:]
            returnArray['id'] = it.id
            returnArray['name'] = it.name
            returnArray['comment'] = it.comment
            returnArray['ontology'] = it.getIdOntology()

            RelationTerm rt = RelationTerm.findByRelationAndTerm2(Relation.findByName(RelationTerm.names.PARENT),Term.get(it.id))

            returnArray['parent'] = rt?.getIdTerm1()
            if(it.color) returnArray['color'] = it.color

            /*def children = [:]
          it.child.each { child ->
            def childArray = [:]
            childArray['name'] = child.name
            children[child.id] = childArray
          }
          returnArray['children'] = children*/
            return returnArray
        }
    }

}
