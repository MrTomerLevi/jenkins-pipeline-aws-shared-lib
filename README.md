# jenkins-pipeline-aws-shared-lib

This project provides easy AWS  shared library which can be used into Jenkins pipeline.
References:
* https://jenkins.io/doc/book/pipeline/shared-libraries/#using-libraries
* https://github.com/Diabol/jenkins-pipeline-shared-library-template
* https://github.com/AndreyVMarkelov/jenkins-pipeline-shared-lib-sample

Setup instructions

In Jenkins, go to Manage Jenkins → Configure System. Under Global Pipeline Libraries, add a library with the following settings:
* Default version: Specify a Git reference (branch or commit SHA), e.g. master
* Retrieval method: Modern SCM
* Select the Git type
* Project repository: https://github.com/AndreyVMarkelov/jenkins-pipeline-shared-lib-sample.git
* Credentials: (leave blank)

Then create a Jenkins job with the following pipeline (note that the underscore _ is not a typo):
```
@Library('jenkins-pipeline-aws-shared-lib')_

def aws = new com.aws.Pipeline()

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
```
@Library('jenkins-pipeline-aws-shared-lib') import com.aws.Pipeline

def aws = new Pipeline()

...
```