pipeline {
    agent any

    parameters {
        string(name: 'SERVICE_NAME', defaultValue: 'example-service', description: 'Enter the name of the service to remove from scans')
    }

    stages {
        stage('Create Jira Ticket - BlackDuck') {
            steps {
                script {
                    def summary = "Remove the service '${params.SERVICE_NAME}' from BlackDuck"
                    def description = "The service '${params.SERVICE_NAME}' needs to be removed from BlackDuck scan tools. Linked to parent ticket ${JIRA_EXISTING_ISSUE_KEY}"
                    def assignee = "userA"  // 🔁 Replace with actual Jira username

                    def payload = """
                    {
                        "fields": {
                            "project": { "key": "${JIRA_PROJECT_KEY}" },
                            "summary": "${summary}",
                            "description": "${description}",
                            "issuetype": { "name": "Task" },
                            "assignee": { "name": "${assignee}" }
                        }
                    }
                    """

                    sh """
                        curl -s -X POST "${JIRA_URL}/issue/" \
                        -H "Authorization: Bearer ${JIRA_TOKEN}" \
                        -H "Content-Type: application/json" \
                        -d '${payload}' > bd_create_response.json
                    """
                    script {
                        env.BD_ISSUE_KEY = sh(script: "jq -r .key bd_create_response.json", returnStdout: true).trim()
                        echo "✅ Created BlackDuck Ticket: ${env.BD_ISSUE_KEY}"
                    }

                    def linkPayload = """
                    {
                        "type": { "name": "Relates" },
                        "inwardIssue": { "key": "${env.BD_ISSUE_KEY}" },
                        "outwardIssue": { "key": "${JIRA_EXISTING_ISSUE_KEY}" },
                        "comment": { "body": "Linked BlackDuck removal ticket ${env.BD_ISSUE_KEY} to parent ${JIRA_EXISTING_ISSUE_KEY}" }
                    }
                    """

                    sh """
                        curl -s -X POST "${JIRA_URL}/issueLink" \
                        -H "Authorization: Bearer ${JIRA_TOKEN}" \
                        -H "Content-Type: application/json" \
                        -d '${linkPayload}' > bd_link_response.json
                        echo "✅ Linked BlackDuck ticket to parent issue"
                    """
                }
            }
        }

        stage('Create Jira Ticket - Fortify') {
            steps {
                script {
                    def summary = "Remove the service '${params.SERVICE_NAME}' from Fortify scan"
                    def description = "The service '${params.SERVICE_NAME}' needs to be removed from Fortify scan tools. Linked to parent ticket ${JIRA_EXISTING_ISSUE_KEY}"
                    def assignee = "userB"  // 🔁 Replace with actual Jira username

                    def payload = """
                    {
                        "fields": {
                            "project": { "key": "${JIRA_PROJECT_KEY}" },
                            "summary": "${summary}",
                            "description": "${description}",
                            "issuetype": { "name": "Task" },
                            "assignee": { "name": "${assignee}" }
                        }
                    }
                    """

                    sh """
                        curl -s -X POST "${JIRA_URL}/issue/" \
                        -H "Authorization: Bearer ${JIRA_TOKEN}" \
                        -H "Content-Type: application/json" \
                        -d '${payload}' > fortify_create_response.json
                    """
                    script {
                        env.FORTIFY_ISSUE_KEY = sh(script: "jq -r .key fortify_create_response.json", returnStdout: true).trim()
                        echo "✅ Created Fortify Ticket: ${env.FORTIFY_ISSUE_KEY}"
                    }

                    def linkPayload = """
                    {
                        "type": { "name": "Relates" },
                        "inwardIssue": { "key": "${env.FORTIFY_ISSUE_KEY}" },
                        "outwardIssue": { "key": "${JIRA_EXISTING_ISSUE_KEY}" },
                        "comment": { "body": "Linked Fortify removal ticket ${env.FORTIFY_ISSUE_KEY} to parent ${JIRA_EXISTING_ISSUE_KEY}" }
                    }
                    """

                    sh """
                        curl -s -X POST "${JIRA_URL}/issueLink" \
                        -H "Authorization: Bearer ${JIRA_TOKEN}" \
                        -H "Content-Type: application/json" \
                        -d '${linkPayload}' > fortify_link_response.json
                        echo "✅ Linked Fortify ticket to parent issue"
                    """
                }
            }
        }
    }

    post {
        always {
            sh "rm -f bd_create_response.json bd_link_response.json fortify_create_response.json fortify_link_response.json"
        }
    }
}
