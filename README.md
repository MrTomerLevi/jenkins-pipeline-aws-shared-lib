# jenkins-pipeline-aws-shared-lib

This project provides easy AWS  shared library which can be used into Jenkins pipeline.

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

stage('Print Build Info') {
    printBuildinfo {
        name = "Sample Name"
    }
} stage('Disable balancer') {
    disableBalancerUtils()
} stage('Deploy') {
    deploy()
} stage('Enable balancer') {
    enableBalancerUtils()
} stage('Check Status') {
    checkStatus()
}
```
