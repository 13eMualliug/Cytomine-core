package be.cytomine

import be.cytomine.project.Project
import be.cytomine.security.SecUser
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclEntry
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclObjectIdentity
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclSid

import static org.springframework.security.acls.domain.BasePermission.*

abstract class CytomineDomain {

    def springSecurityService
    def cytomineService
    def sequenceService

    static def grailsApplication
    Long id
    Date created
    Date updated

    static mapping = {
        tablePerHierarchy false
        id generator: "assigned"
    }

    static constraints = {
        created nullable: true
        updated nullable: true
    }

    public beforeInsert() {
        if (!created) {
            created = new Date()
        }
        if (id == null) {
            id = sequenceService.generateID(this)
        }
    }

  def beforeValidate() {
      if (!created) {
          created = new Date()
      }
      if (id == null) {
          id = sequenceService.generateID(this)
      }
  }

    public beforeUpdate() {
        updated = new Date()
    }

    void checkAlreadyExist() {
        //do nothing ; if override by a sub-class, should throw AlreadyExist exception
    }

    /**
     * Return domain project (annotation project, ...)
     * By default, a domain has no project.
     * You need to override getProject() in domain class
     * @return Domain project
     */
    public Project projectDomain() {
        return null;
    }

    def getCallBack() {
        return null
    }

    boolean hasPermission(String permission) {
        try {
            return hasPermission(this,permission)
        } catch (Exception e) {e.printStackTrace()}
        return false
    }

    boolean hasPermission(Long id,String className, String permission) {
        try {
            def obj = grailsApplication.classLoader.loadClass(className).get(id)
            return hasPermission(obj,permission)
        } catch (Exception e) {
            log.error e.toString()
            e.printStackTrace()}
        return false
    }

    boolean hasPermission(def domain,String permissionStr) {
        try {
            SecUser currentUser = cytomineService.getCurrentUser()
            String usernameParentUser = currentUser.realUsername()
            int permission = -1
            if(permissionStr.equals("READ")) permission = READ.mask
            else if(permissionStr.equals("WRITE")) permission = WRITE.mask
            else if(permissionStr.equals("DELETE")) permission = DELETE.mask
            else if(permissionStr.equals("CREATE")) permission = CREATE.mask
            else if(permissionStr.equals("ADMIN")) permission = ADMINISTRATION.mask
            AclObjectIdentity aclObject = AclObjectIdentity.findByObjectId(domain.id)
            AclSid aclSid = AclSid.findBySid(usernameParentUser)

            if(!aclObject) return false
            if(!aclSid) return false

            boolean hasPermission = false;
            List<AclEntry> acls = AclEntry.findAllByAclObjectIdentityAndSid(aclObject,aclSid)
            acls.each { acl ->
                if(acl.mask>=permission) {
                    hasPermission=true
                }
            }

            return hasPermission

        } catch (Exception e) {
            log.error e.toString()
            e.printStackTrace()
        }
        return false
    }

}
