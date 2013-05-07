package be.cytomine.processing

import be.cytomine.CytomineDomain
import be.cytomine.command.ResponseService
import be.cytomine.project.Project
import be.cytomine.security.UserJob
import be.cytomine.utils.JSONUtils
import grails.converters.JSON
import org.apache.log4j.Logger

/**
 * A job is a software instance
 * This is the execution of software with some parameters
 */
class Job extends CytomineDomain  {
    /**
     * Job status (enum type are too heavy with GORM)
     */
    public static int NOTLAUNCH = 0
    public static int INQUEUE = 1
    public static int RUNNING = 2
    public static int SUCCESS = 3
    public static int FAILED = 4
    public static int INDETERMINATE = 5
    public static int WAIT = 6

    /**
     * Job progression
     */
    int progress = 0

    /**
     * Job status (see static int)
     */
    int status = 0

    /**
     * Job Indice for this software in this project
     */
    int number

    /**
     * Text comment for the job status
     */
    String statusComment

    /**
     * Job project
     */
    Project project

    /**
     * Generic field for job rate info
     * The rate is a quality value about the job works
     */
    Double rate = null

    /**
     * Flag to see if data generate by this job are deleted
     */
    boolean dataDeleted = false

    static transients = ["url"]

    static belongsTo = [software: Software]

    static constraints = {
        progress(min: 0, max: 100)
        project(nullable:true)
        statusComment(nullable:true)
        status(range: 0..6)
        rate(nullable: true)
    }

    static mapping = {
        tablePerHierarchy(true)
        id(generator: 'assigned', unique: true)
        sort "id"
        software fetch: 'join'
    }

    public beforeInsert() {
        super.beforeInsert()
        //Get the last job with the same software and same project to incr job number
        List<Job> previousJob = Job.findAllBySoftwareAndProject(software,project,[sort: "number", order: "desc",max: 1])
        if(!previousJob.isEmpty()) {
            number = previousJob.get(0).number+1
        } else {
            number = 1
        };
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */    
    static def insertDataIntoDomain(def json,def domain = new Job()) {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.status = JSONUtils.getJSONAttrInteger(json, 'status', 0)
        domain.progress = JSONUtils.getJSONAttrInteger(json, 'progress', 0)
        domain.statusComment = JSONUtils.getJSONAttrStr(json, 'statusComment')
        domain.project = JSONUtils.getJSONAttrDomain(json, "project", new Project(), true)
        domain.software = JSONUtils.getJSONAttrDomain(json, "software", new Software(), true)
        domain.rate = JSONUtils.getJSONAttrDouble(json, 'rate', -1)
        domain.dataDeleted =  JSONUtils.getJSONAttrBoolean(json,'dataDeleted', false)
        domain.created = JSONUtils.getJSONAttrDate(json, 'created')
        domain.updated = JSONUtils.getJSONAttrDate(json, 'updated')
        return domain;
    }

    /**
     * Define fields available for JSON response
     * This Method is called during application start
     */
    static void registerMarshaller() {
        Logger.getLogger(this).info("Register custom JSON renderer for " + Job.class)
        JSON.registerObjectMarshaller(Job) {
            def job = [:]
            job.id = it.id
            job.algoType = ResponseService.getClassName(it).toLowerCase()
            job.progress = it.progress
            job.status = it.status
            job.number = it.number
            job.statusComment = it.statusComment
            job.project = it.project?.id
            job.software = it.software?.id
            job.softwareName = it.software?.name
            job.rate = it.rate
            job.created = it.created?.time?.toString()
            job.updated = it.updated?.time?.toString()
            job.dataDeleted = it.dataDeleted
            try {
                UserJob user = UserJob.findByJob(it)
                job.username = user?.humanUsername()
                job.userJob = user.id
                job.jobParameters = it.parameters()
            } catch (Exception e) {
                log.info e
            }
            return job
        }
    }

    public List<JobParameter> parameters() {
        if(this.version!=null) {
            return JobParameter.findAllByJob(this,[sort: 'created'])
        } else {
            return []
        }
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return project.container()
    }

    public String toString() {
        software.getName() + "[" + software.getId() + "]"
    }

}
