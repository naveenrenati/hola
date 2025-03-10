pipeline {
    agent any


    stages {
        stage('Create Jira Issue') {
            steps {
                script {
                    def payload = """
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
                    sh """
                        curl -s -X POST "${JIRA_URL}/issue/" \
                        -H "Authorization: Bearer ${JIRA_TOKEN}" \
                        -H "Content-Type: application/json" \
                        -d '${payload}' > create_response.json
                    """
                    script {
                        env.NEW_ISSUE_KEY = sh(script: "jq -r .key create_response.json", returnStdout: true).trim()
                        echo "✅ Created Jira Issue: ${env.NEW_ISSUE_KEY}"
                    }
                }
            }
        }

        stage('Link to Existing Issue') {
            steps {
                script {
                    def linkPayload = """
                    {
                        "type": { "name": "Relates" },
                        "inwardIssue": { "key": "${env.NEW_ISSUE_KEY}" },
                        "outwardIssue": { "key": "${JIRA_EXISTING_ISSUE_KEY}" },
                        "comment": {
                            "body": "Automatically linked issue ${env.NEW_ISSUE_KEY} to ${JIRA_EXISTING_ISSUE_KEY}"
                        }
                    }
                    """
                    sh """
                        curl -s -X POST "${JIRA_URL}/issueLink" \
                        -H "Authorization: Bearer ${JIRA_TOKEN}" \
                        -H "Content-Type: application/json" \
                        -d '${linkPayload}' > link_response.json
                        echo "✅ Link API response:"
                        cat link_response.json
                    """
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
