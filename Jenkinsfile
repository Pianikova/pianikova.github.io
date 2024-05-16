#!/usr/bin/env groovy

pipeline {
    
  agent {
    label {
      label 'linux'
    }
  }

  tools {
    maven 'Maven 3.6.0'
    jdk 'jdk11'
  }
  
  options {
    disableConcurrentBuilds()
    timestamps()
    buildDiscarder(logRotator(numToKeepStr:'10'))    // Keeps the 10 most recent builds
    timeout(time: 30, unit: 'MINUTES')
  }
    
  parameters {
    booleanParam(name: 'DEBUG_BUILD', defaultValue: false, description: '')
  }

  stages {
  
    stage("Initialize") {
      steps {
        
        dir('bom') {
            script {
                env.CODEAI_VERSION = getVersion()

                env.CODEAI_P2_PATH=determineP2RepositoryPath(name: 'codeai', version: "${env.CODEAI_VERSION}")
                env.CODEAI_P2_SDK_PATH=determineP2RepositoryPath(name: 'codeai', version: "${env.CODEAI_VERSION}", sdk: true)
            }
        }
        

        echo sh(returnStdout: true, script: 'env')
        
        // force ignore auto archiving of artifacts
        doNotAttachMavenArtifacts()
      }
    }

    stage("Build") {
      steps {
        mvn(maven: "Maven 3.6.0", jdk: "jdk11", sign: true, profiles: 'find-bugs,SDK', useVirtualDisplay: true)
        injectProperties('CODEAI', 'target/build.properties')
      }
    }

    stage("Publish") {
      when { expression { isDevelopOrReleaseBranch() }}
      steps {
        publishP2Repository(name: 'CodeAI', source: 'repositories/org.e1c.edt.ai.repository/target/repository', target: "${env.CODEAI_P2_PATH}", qualifier: "${env.CODEAI_QUALIFIER}")
        publishP2Repository(name: 'CodeAISDK', source: 'repositories/org.e1c.edt.ai.repository.sdk/target/repository', target: "${env.CODEAI_P2_SDK_PATH}", qualifier: "${env.CODEAI_QUALIFIER}")        
      }
    }
  }

  post {
    always {
      dir('bundles') {
          junit allowEmptyResults: true, testResults: '**/target/surefire-reports/**/*.xml'
      }
      cleanWs()
    }
    aborted {
      // always clean ws
      cleanWs()
    }
  }
}