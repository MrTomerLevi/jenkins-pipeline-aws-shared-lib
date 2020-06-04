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
                    
        stage('Create CloudFormation Stack'){
            def cfTemplate = "./cloudformation.yaml"
            java.util.List<String> capabilities = ["CAPABILITY_IAM"]
            def s3Bucket = "my-s3-bucket-name"
            
            java.util.Map parameters = [
                "EnvName"          : params.environment_name,
                "OtherParameter"   : "1234"
            ]
            def stackId = aws.cloudFormationCreateOrUpdateStack("myStackName", cfPackedTemplate, parameters, capabilities)
            println("Stack id: ${stackId}")
            assert packingStatus != 2  : "CloudFormation update failed"
        }
    }
}
```

```groovy
@Library('jenkins-pipeline-aws-shared-lib')_

def aws = new com.jenkins.aws.Pipeline()

docker.image('garland/aws-cli-docker').inside {
    withAWS(credentials: 'aws-credentials',region: 'us-east-1'){
                    
        stage('Package and Create CloudFormation Stack'){
              def cfTemplate = "./cloudformation.yaml"
              def cfPackedTemplate = "./cloudformation-packed.yaml"
              java.util.List<String> capabilities = ["CAPABILITY_IAM", "CAPABILITY_NAMED_IAM" ,"CAPABILITY_AUTO_EXPAND"]

              stackExist = aws.cloudFormationStackExist("myStackName")
              println("CloudFormation Stack already exist: ${stackExist}")

              def s3Bucket = "my-s3-bucket-name"
              def packingStatus = aws.cloudFormationPackage(s3Bucket, "ready-templates-folder", cfPackedTemplate, cfPackedTemplate)
              println("CloudFormation Stack packing status code is: ${packingStatus}")
              assert packingStatus != 255  : "CloudFormation packing failed"

              def stackId = aws.cloudFormationCreateOrUpdateStack("myStackName", cfPackedTemplate, bijoInfraCfParams, capabilities)
             println("Stack id: ${stackId}")
             assert packingStatus != 2  : "CloudFormation update failed"
        }
    }
}
```

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
