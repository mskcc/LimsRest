// Replace these with your credentials and details
const JIRA_BASE_URL = 'https://mskjira.mskcc.org';
const JIRA_EMAIL = 'timalr@mskcc.org';
const JIRA_API_TOKEN = 'ATATT3xFfGF0Ks0OJJHTKjNl4bZme7DtJbDpYmV7kZ9ejjJs6SJMPmiaWTBaiuPJB4mXmv3PChtAaE4wu9y3WuaMsKMEJ4FnzougEEt4z_1LKb3jJPOj9_q5T_1cXj1d7W2AbOvBtEP4ryfuC3sjT3hUsmYeO3TGHhAjAUyeYeO_Hv8p6aJATMk';
const PROJECT_KEY = 'IGODATA';
const ISSUE_SUMMARY = 'Test ticket from script';
const ISSUE_DESCRIPTION = 'This ticket was created using a pure JavaScript script.';
const ISSUE_TYPE = 'Task'; // Can be "Story", "Bug", etc.

async function createJiraTicket() {
  const auth = Buffer.from(`${JIRA_EMAIL}:${JIRA_API_TOKEN}`).toString('base64');

  const response = await fetch(`${JIRA_BASE_URL}/rest/api/3/issue`, {
    method: 'POST',
    headers: {
      'Authorization': `Basic ${auth}`,
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      fields: {
        project: { key: PROJECT_KEY },
        summary: ISSUE_SUMMARY,
        description: ISSUE_DESCRIPTION,
        issuetype: { name: ISSUE_TYPE }
      }
    })
  });

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`Failed to create issue: ${response.status} ${response.statusText} - ${errorBody}`);
  }

  const data = await response.json();
  console.log('Ticket created:', data.key);
}

// Run the script
createJiraTicket().catch(console.error);
