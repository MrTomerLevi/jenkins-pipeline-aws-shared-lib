package com.aws
import groovy.json.JsonSlurper


/**
 * Runs an sh command and returns both the status and output as a tuple.
 * Example:
 * (status, output) = runShCommand('ls -ltr')
 * @param script - the script to execute
 * @return a tuple of (status, output)
 */
def runShCommand(String script){

    Random rand = new Random()
    def random_num = rand.nextInt(100000)

    def file_name = "script_output_${random_num}.txt"
    def status = sh(returnStatus: true, script: "$script > $file_name")
    def output = readFile(file_name).trim()
    sh "rm $file_name"

    return [status, output]
}

/**
 Executes the given command and parse the output into an object.
 *Assumes the command returns Json as output.
 */
def executeShToObject(String command){
    def (status, output) = runShCommand(command)

    if (status != 0){
        throw new Exception(output)
    }else {
        def jsonSlurper = new JsonSlurper()
        return jsonSlurper.parseText(output)
    }

}

/**
 Execute AWS ECR commands.
 Basically calls aws-cli like: aws ecr 'operation' ...
 Example: aws ecr create-repository --repository-name dataops/bi-jobs-worker
 Returns: repositoryUri
 */
def ecr(String operation, String repositoryName){
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
def ecrGetOrCreateRepo(String repositoryName){
    def ecrRepositoryUri
    def ecrCreation = ecr('create-repository', repositoryName)
    if(ecrCreation){
        ecrRepositoryUri =  ecrCreation['repository']['repositoryUri']
    } else {
        def ecrDescribe = ecr('describe-repositories', repositoryName)
        ecrRepositoryUri = ecrDescribe.repositories[0]['repositoryUri']
    }
    return ecrRepositoryUri
}

def ecrGetLogin(){
    return sh(script: "aws ecr get-login --no-include-email", returnStdout:true).trim()
}

/**
 Execute AWS ECS command.
 Basically calls aws-cli like: aws ecs 'operation' ...
 Example: aws ecs create-cluster --cluster-name "dwh-bi-jobs"
 Returns: status code
 */
def ecs(String operation, String clusterName){
    try{
        def command = "aws ecs ${operation} --cluster-name ${clusterName}"
        return executeShToObject(command)
    } catch (Exception ex) {
        println("Unable to call ECS ${operation} got Exception: ${ex}")
        return null
    }
}

def ecsCreateCluster(String clusterName){
    def clusterDetails = ecs('create-cluster',clusterName)
    return clusterDetails['cluster']['clusterArn']
}

/**
 Execute AWS logs command.
 Basically calls aws-cli like: aws logs 'operation' ...
 Example: aws logs create-log-group --log-group-name dataops/stepfunctions-bi-jobs
 Returns: status code
 */
def logs(String operation, String logGroupName){
    def status = sh(script:"aws logs ${operation} --log-group-name ${logGroupName}", returnStatus:true)
    println("awsLogs status code is: ${status}")
    return status
}

def ecsRegisterTask(String cliInputJsonFile){
    def command = "aws ecs register-task-definition --cli-input-json file://${cliInputJsonFile}"
    def responseObject = executeShToObject(command)
    def taskDefinitionArn = responseObject.taskDefinition.taskDefinitionArn
    return taskDefinitionArn
}

def logsGetOrCreateLogGroup(String logGroupName){
    def ok = 0
    def alreadyExist = 255
    // 0 and 255 are ok
    def logsCreationStatus = logs('create-log-group', logGroupName)
    if(logsCreationStatus != alreadyExist && logsCreationStatus != ok){
        error("Unable to create CloudWatch log group: ${logGroupName}, return status from aws logs is: ${logsCreationStatus}")
    }
}

/**
 * Executes AWS CloudFormation create stack command
 * @param stackName
 * @param templateFile - path to a CloudFormation template .yaml or .json file
 * @param parameters   - example:
 * java.util.Map parameters = [
 *                              "param1"     : 1,
 *                              "param2"     : "some-value",
 *                            ]
 * @param capabilities - list of capabilities: CAPABILITY_IAM, CAPABILITY_NAMED_IAM, CAPABILITY_AUTO_EXPAND
 * @returns stack id as a String
 */
def cloudFormationCreateStack(String stackName, String templateFile, java.util.Map parameters, java.util.List<String> capabilities=[]){
    def parametersString = ""
    parameters.each{ key, value ->
        parametersString += "ParameterKey=${key},ParameterValue='${value}' "
    }
    def capabilitiesString = ""
    capabilities.each { c ->
        capabilitiesString += "${c} "
    }
    def command = "aws cloudformation create-stack --stack-name ${stackName} --capabilities ${capabilitiesString.trim()} --template-body file://${templateFile} --parameters ${parametersString.trim()}"
    def responseObject = executeShToObject(command)

    return responseObject.StackId
}

/**
 * Executes AWS CloudFormation package command
 * More details: https://docs.aws.amazon.com/cli/latest/reference/cloudformation/package.html
 *
 * @param s3Bucket - The name of the S3 bucket where this command uploads the artifacts that are referenced in your template
 * @param s3Prefix - A prefix name that the command adds to the artifacts' name when it uploads them to the S3 bucket. The prefix name is a path name (folder name) for the S3 bucket.
 * @param templateFile - The path where your AWS CloudFormation template is located.
 * @param outputTemplateFile - The path to the file where the command writes the output AWS CloudFormation template.
 *
 * @returns command status code
 */
def cloudFormationPackage(String s3Bucket, String s3Prefix, String templateFile, String outputTemplateFile ){
    def command = "aws cloudformation package --s3-bucket ${s3Bucket} --s3-prefix ${s3Prefix} --template-file ${templateFile} --output-template-file ${outputTemplateFile.trim()}"
    def status = sh(script: command, returnStatus:true)
    return status
}

/**
 * Executes AWS CloudFormation describe stack command
 * @param stackName
 * @returns object representing the return value of the corresponding CLI command
 */
def cloudFormationDescribeStacks(String stackName){
    def command = "aws cloudformation describe-stacks --stack-name ${stackName}"
    def responseObject = executeShToObject(command)
    return responseObject
}

/**
 * Executes AWS CloudFormation update stack command
 * @param stackName
 * @param templateFile - path to a CloudFormation template .yaml or .json file
 * @param parameters   - example:
 * @param capabilities - list of capabilities: CAPABILITY_IAM, CAPABILITY_NAMED_IAM, CAPABILITY_AUTO_EXPAND
 * java.util.Map parameters = [
 *                              "param1"     : 1,
 *                              "param2"     : "some-value",
 *                            ]
 *
 * @returns cli command status code if return status is 0, otherwise throws Exception with command output as body
 */
def cloudFormationUpdateStack(String stackName, String templateFile, java.util.Map parameters, java.util.List<String> capabilities=[], boolean returnStatus = true){
    def parametersString = ""
    parameters.each{ key, value ->
        parametersString += "ParameterKey=${key},ParameterValue='${value}' "
    }
    def capabilitiesString = ""
    capabilities.each { c ->
        capabilitiesString += "${c} "
    }
    def command = "aws cloudformation update-stack --stack-name ${stackName} --capabilities ${capabilitiesString.trim()} --template-body file://${templateFile} --parameters ${parametersString.trim()}"
    def (status, output) = runShCommand(command)

    println("cloudformation update-stack status code is: ${status}")
    println("cloudformation update-stack output is: ${output}")

    if(status != 0 && !returnStatus){
        throw new Exception(output)
    }
    return status
}



/**
 * Executes AWS CloudFormation wait stack-create-completed
 * @param stackName
 *
 * @returns cli command status code
 */
def cloudFormationWaitStackCreateComplete(String stackName){
    def waitCommand = "aws cloudformation wait stack-create-complete --stack-name ${stackName}"
    def status = sh(waitCommand)
    println("cloudformation wait stack-create-complete status code is: ${status}")

    return status
}


/**
 * Executes AWS CloudFormation wait stack-update-completeed
 * @param stackName
 *
 * @returns cli command status code
 */
def cloudFormationWaitStackUpdateComplete(String stackName){
    def waitCommand = "aws cloudformation wait stack-update-complete --stack-name ${stackName}"
    def status = sh(waitCommand)
    println("cloudformation wait stack-update-complete status code is: ${status}")

    return status
}

/**
 * Checks an AWS CloudFormation stack exists
 * @param stackName
 *
 * @returns true if the given stack exist regardless of its state
 */
boolean cloudFormationStackExist(String stackName){
    try{
        def waitCommand = "aws cloudformation describe-stacks --stack-name ${stackName}"
        def status = sh(waitCommand)
        println("cloudformation wait describe-stacks status code is: ${status}")

        return status != 255 ? true : false
    } catch (Exception ex) {
        // if stack does not exist an exception is thrown
        return false
    }
}

/**
 * Checks an AWS CloudFormation delete stack
 * @param stackName
 *
 * @returns true on success
 */
boolean cloudFormationeDeleteStack(String stackName){
    def command = "aws cloudformation delete-stack --stack-name ${stackName}"
    def status = sh(script: command, returnStatus:true)
    println("cloudformation delete-stack status code is: ${status}")

    return status
}

/**
 * Executes AWS CloudFormation list stacks
 * see https://docs.aws.amazon.com/cli/latest/reference/cloudformation/list-stacks.html
 *
 * @param stackStatusFilter Stack status to use as a filter.
 *        Specify one or more stack status codes to list only stacks with the specified status codes.
 *        For a complete list of stack status codes, see the StackStatus parameter of the Stack data type.
 *
 * @returns A list of StackSummary structures containing information about the specified stacks.
 */
boolean cloudFormationeListStacks(String stackStatusFilter = null){
    def command = "aws cloudformation list-stacks"
    if (stackStatusFilter != null){
        command += " --stack-status-filter ${stackStatusFilter}"
    }
    return executeShToObject(command)
}

/**
 * Executes AWS CloudFormation wait stack-delete-complete
 * @param stackName
 *
 * @returns cli command status code
 */
def cloudFormationWaitStackDeleteComplete(String stackName){
    def waitCommand = "aws cloudformation wait stack-delete-complete --stack-name ${stackName}"
    def status = sh(waitCommand)
    println("cloudformation wait stack-delete-complete status code is: ${status}")

    return status
}

/**
 * Executes AWS CloudFormation create or update stack command
 * @param stackName
 * @param templateFile - path to a CloudFormation template .yaml or .json file
 * @param parameters   - example:
 * @param capabilities - list of capabilities: CAPABILITY_IAM, CAPABILITY_NAMED_IAM, CAPABILITY_AUTO_EXPAND
 * java.util.Map parameters = [
 *                              "param1"     : 1,
 *                              "param2"     : "some-value",
 *                            ]
 *
 * @returns cli command status code
 */
def cloudFormationCreateOrUpdateStack(String stackName, String templateFile, java.util.Map parameters, java.util.List<String> capabilities = []){
    def parametersString = ""
    parameters.each{ key, value ->
        parametersString += "ParameterKey=${key},ParameterValue='${value}' "
    }
    def capabilitiesString = ""
    capabilities.each { c ->
        capabilitiesString += "${c} "
    }

    if (cloudFormationStackExist(stackName)){
        println("cloudformation stack ${stackName} exist, executing update-stack command")
        return cloudFormationUpdateStack(stackName, templateFile, parameters, capabilities)
    }
    return cloudFormationCreateStack(stackName, templateFile, parameters, capabilities)

}

/**
 * Executes AWS ECR delete repository command
 * @param repositoryName - The name of the repository to delete.
 * @param force - If a repository contains images, forces the deletion.
 *
 * @returns cli command status code, 0 on success otherwise 255
 */
def ecrDeleteRepository(String repositoryName, Boolean force){
    String command = "aws ecr delete-repository --repository-name ${repositoryName}"
    if (force){
        command += " --force"
    }
    def status = sh(script: command, returnStatus:true)
    println("ECR delete repository status code is: ${status}")

    return status
}

/**
 * Executes AWS SSM put parameter command
 * see: https://docs.aws.amazon.com/systems-manager/latest/userguide/param-create-cli.html
 *
 * @param name - parameter_name
 * @param value - The parameter value that you want to add to the system.
 * @param type - String or StringList
 * @param description - Information about the parameter that you want to add to the system. Optional but recommended.
 * @param overwrite -
 *
 * @returns cli command status code, 0 on success otherwise 255
 *
 * Example:
 * ssmPutParameter("/IAD/ERP/Oracle/addUsers", "Milana,Mariana,Mark,Miguel","StringList")
 */
def ssmPutParameter(String name, String value, String type, String description = '', boolean overwrite = false){
    String command = "aws ssm put-parameter --name ${name} --value ${value} --type ${type} --description ${description}"
    if (overwrite){
        command += " --overwrite"
    }
    def status = sh(script: command, returnStatus:true)
    println("SSM put parameter status code is: ${status}")

    return status
}

/**
 * Executes AWS SSM get parameter command
 * see: https://docs.aws.amazon.com/cli/latest/reference/ssm/get-parameter.html
 *
 * @param name - The name of the parameter you want to query.
 * @returns cli command status code, 0 on success otherwise 255
 *
 * Example:
 * ssmGetParameter("/IAD/ERP/Oracle/sunshine")
 *
 * output:
 *   {
 *       "Name": "/IAD/ERP/Oracle/addUsers",
 *       "Type": "String",
 *       "Value": "Good day sunshine",
 *       "Version": 1,
 *       "LastModifiedDate": 1530018761.888,
 *       "ARN": "arn:aws:ssm:us-east-1:123456789012:parameter/helloWorld"
 *   }
 */
def ssmGetParameter(String name){
    String command = "aws ssm get-parameter --name ${name}"
    return executeShToObject(command)['Parameter']
}
