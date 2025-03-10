pipeline {
    agent any


    stages {
        stage('Create Jira Issue') {
            steps {
                script {
                    def createIssuePayload = """
                    {
                        "fields": {
                            "project": {
                                "key": "${JIRA_PROJECT_KEY}"
                            },
                            "summary": "Test issue from Jenkins Pipeline",
                            "description": "Creating an issue using Jira REST API from Jenkins",
                            "issuetype": {
                                "name": "Task"
                            },
                            "assignee": {
                                "name": "${ASSIGNEE}"
                            }
                        }
                    }
                    """

                    def responseCode = sh(
                        script: """curl -s -w "%{http_code}" -o create_response.json -X POST "${JIRA_URL}/issue/" \
                          -H "Authorization: Bearer ${JIRA_TOKEN}" \
                          -H "Content-Type: application/json" \
                          -d '${createIssuePayload}'""",
                        returnStdout: true
                    ).trim()

                    if (responseCode == "201") {
                        def issueKey = sh(script: "jq -r .key create_response.json", returnStdout: true).trim()
                        echo "✅ Jira Issue Created: ${issueKey}"
                        env.NEW_ISSUE_KEY = issueKey
                    } else {
                        echo "🔍 Jira Create API returned status: ${responseCode}"
                        sh "cat create_response.json"
                        error "❌ Failed to create Jira Issue"
                    }
                }
            }
        }

        stage('Link to Existing Jira Issue') {
            steps {
                script {
                    def linkPayload = """
                    {
                        "type": {
                            "name": "Relates"
                        },
                        "inwardIssue": {
                            "key": "${env.NEW_ISSUE_KEY}"
                        },
                        "outwardIssue": {
                            "key": "${JIRA_EXISTING_ISSUE_KEY}"
                        },
                        "comment": {
                            "body": "Linking issue ${env.NEW_ISSUE_KEY} to existing issue ${JIRA_EXISTING_ISSUE_KEY}"
                        }
                    }
                    """

                    def linkResponseCode = sh(
                        script: """curl -s -o link_response.json -w "%{http_code}" -X POST "${JIRA_URL}/issueLink" \
                          -H "Authorization: Bearer ${JIRA_TOKEN}" \
                          -H "Content-Type: application/json" \
                          -d '${linkPayload}'""",
                        returnStdout: true
                    ).trim()

                    if (linkResponseCode == "200" || linkResponseCode == "201") {
                        def errorMsg = sh(script: "jq '.errorMessages' link_response.json", returnStdout: true).trim()
                        if (errorMsg != "null" && errorMsg != "[]") {
                            echo "⚠️ Link response contains errorMessages: ${errorMsg}"
                            error "❌ Jira returned error during linking."
                        } else {
                            echo "✅ Issue ${env.NEW_ISSUE_KEY} successfully linked to ${JIRA_EXISTING_ISSUE_KEY}"
                        }
                    } else {
                        echo "🔍 Jira Link API returned status: ${linkResponseCode}"
                        sh "cat link_response.json"
                        error "❌ Failed to link Jira issues"
                    }
                }
            }
        }
    }

    post {
        always {
            sh "rm -f create_response.json link_response.json"
        }
    }
}
