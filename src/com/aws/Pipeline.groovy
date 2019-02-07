package com.aws
import groovy.json.JsonSlurper


/**
 Executes the given command and parse the output into object.
 *Assumes the command returns Json as output.
 */
def executeShToObject(String command){
    def output = sh(script: command, returnStdout:true).trim()
    def jsonSlurper = new JsonSlurper()
    return jsonSlurper.parseText(output)
}

//TODO: move all methods to lib
/**
 Execute AWS ECR commands.
 Basically calls aws-cli like: aws ecr 'operation' ...
 Example: aws ecr create-repository --repository-name dataops/bi-jobs-worker
 Returns: repositoryUri
 */
def awsECR(String operation, String repositoryName){
    try{
        def command = "aws ecr ${operation} --repository-name ${repositoryName}"
        return executeShToObject(command)
    } catch (Exception ex) {
        println("Unable to call ECR ${operation} got Exception: ${ex}")
        return null
    }
}

/**
 Tries to create ECR repo, if fails will try to find it.
 Returns: repositoryUri
 */
def awsECRGetOrCreateRepo(String repositoryName){
    def ecrRepositoryUri
    def ecrCreation = awsECR('create-repository', repositoryName)
    if(ecrCreation){
        ecrRepositoryUri =  ecrCreation['repository']['repositoryUri']
    } else {
        def ecrDescribe = awsECR('describe-repositories', repositoryName)
        ecrRepositoryUri = ecrDescribe.repositories[0]['repositoryUri']
    }
    return ecrRepositoryUri
}

def awsECRGetLogin(){
    return sh(script: "aws ecr get-login --no-include-email", returnStdout:true).trim()
}

/**
 Execute AWS ECS command.
 Basically calls aws-cli like: aws ecs 'operation' ...
 Example: aws ecs create-cluster --cluster-name "dwh-bi-jobs"
 Returns: status code
 */
def awsECS(String operation, String clusterName){
    try{
        def command = "aws ecs ${operation} --cluster-name ${clusterName}"
        return executeShToObject(command)
    } catch (Exception ex) {
        println("Unable to call ECS ${operation} got Exception: ${ex}")
        return null
    }
}

def awsECSCreateCluster(String clusterName){
    def clusterDetails = awsECS('create-cluster',clusterName)
    return clusterDetails['cluster']['clusterArn']
}

/**
 Execute AWS logs command.
 Basically calls aws-cli like: aws logs 'operation' ...
 Example: aws logs create-log-group --log-group-name dataops/stepfunctions-bi-jobs
 Returns: status code
 */
def awsLogs(String operation, String logGroupName){
    def status = sh(script:"aws logs ${operation} --log-group-name ${logGroupName}", returnStatus:true)
    println("awsLogs status code is: ${status}")
    return status
}

def awsECSRegisterTask(String cliInputJsonFile){
    def command = "aws ecs register-task-definition --cli-input-json file://${cliInputJsonFile}"
    def responseObject = executeShToObject(command)
    def taskDefinitionArn = responseObject.taskDefinition.taskDefinitionArn
    return taskDefinitionArn
}

def awsLogsGetOrCreateLogGroup(String logGroupName){
    def ok = 0
    def alreadyExist = 255
    // 0 and 255 are ok
    def logsCreationStatus = awsLogs('create-log-group', logGroupName)
    if(logsCreationStatus != alreadyExist && logsCreationStatus != ok){
        error("Unable to create CloudWatch log group: ${logGroupName}, return status from aws logs is: ${logsCreationStatus}")
    }
}

/**
 Replaces placeholders in text and returns the output.
 Placeholders must follow this pattern: ${someVariable}
 binding as a map providing values to placeholders. For example: binding['someVariable'] = 100
 */
def createTextFromTemplate(String text, Map binding) {
    _createTextFromTemplate(text, binding)
}

@NonCPS
def _createTextFromTemplate(String text, Map binding){
    def engine = new groovy.text.GStringTemplateEngine().createTemplate(text)
    def s = engine.make(binding).toString()
    engine = null
    return s
}

/**
 Replaces placeholders in file and write the output into a new file.
 Placeholders must follow this pattern: ${someVariable}
 binding as a map providing values to placeholders. For example: binding['someVariable'] = 100
 */
def createFromTemplate(String templateFile, java.util.Map binding, String destFile){
    def templateText = readFile(templateFile)
    def text = createTextFromTemplate(templateText, binding)
    writeFile(file: destFile, text: text, encoding: "UTF-8")
    println("New file: ${destFile} created from template: ${templateFile}")
}

def awsStepFunctionsRegister(Map args){
    try{
        def command = "aws stepfunctions create-state-machine --name ${args.name} --role-arn '${args.roleArn}' --definition file://${args.definitionFile}"
        return executeShToObject(command)
    } catch (Exception ex) {
        println("Unable to call StepFunctions create-state-machine got Exception: ${ex}")
        return null
    }
}