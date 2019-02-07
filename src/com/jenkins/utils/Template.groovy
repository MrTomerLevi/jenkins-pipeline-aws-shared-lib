package com.utils

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

def stepFunctionsRegister(Map args){
    try{
        def command = "aws stepfunctions create-state-machine --name ${args.name} --role-arn '${args.roleArn}' --definition file://${args.definitionFile}"
        return executeShToObject(command)
    } catch (Exception ex) {
        println("Unable to call StepFunctions create-state-machine got Exception: ${ex}")
        return null
    }
}