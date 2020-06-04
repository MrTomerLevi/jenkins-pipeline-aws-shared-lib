# Jenkins Pipeline AWS Shared Library

This project provides easy AWS shared library which can be used in Jenkins pipeline code.
References:
* https://jenkins.io/doc/book/pipeline/shared-libraries/#using-libraries
* https://github.com/Diabol/jenkins-pipeline-shared-library-template
* https://github.com/AndreyVMarkelov/jenkins-pipeline-shared-lib-sample

## Setup instructions

In Jenkins, go to Manage Jenkins → Configure System. Under Global Pipeline Libraries, add a library with the following settings:
* Default version: Specify a Git reference (branch or commit SHA), e.g. master
* Retrieval method: Modern SCM
* Select the Git type
* Project repository: https://github.com/MrTomerLevi/jenkins-pipeline-aws-shared-lib.git
* Credentials: (leave blank)

Then create a Jenkins job with the following pipeline (note that the underscore _ is not a typo):
```groovy
@Library('jenkins-pipeline-aws-shared-lib')_

def aws = new com.jenkins.aws.Pipeline()

docker.image('garland/aws-cli-docker').inside {
    withAWS(credentials: 'aws-credentials',region: 'us-east-1'){
                    
        stage('Get or create ECS cluster'){
            ecrLogin = aws.awsECRGetLogin()
            println ecrLogin
        }
    }
}
```
Or:
```groovy
@Library('jenkins-pipeline-aws-shared-lib') import com.jenkins.aws.Pipeline

def aws = new Pipeline()

//...
```

For template utils use:
```groovy
@Library('jenkins-pipeline-aws-shared-lib') _ 

def template = new com.jenkins.utis.Template()

node {
    stage('Generate worker from template'){
                def binding = [:]
                binding.workerId    = "12345"
                binding.workerName  = "Tomer"

    
                def templateFile = "worker.template"
                def dstFile      = "worker.json"
                template.createFromTemplate(templateFile, binding ,dstFile)
    }
}
```
