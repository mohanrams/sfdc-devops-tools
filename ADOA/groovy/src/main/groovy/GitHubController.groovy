import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.text.SimpleDateFormat

class GitHubController {

  String todayWeek = new SimpleDateFormat("w", Locale.US).format(new Date())
  JsonSlurper slurper = new JsonSlurper()
  static final API_BASE = "https://api.github.com/repos/"
  String REPO_INFO = 'nkleffman/SFDC-Dev/'
  String access_token

  /**
   * Merge a pull request
   */
  def mergePullRequest(pullRequestId){
      def url = "${API_BASE}${REPO_INFO}pulls/${pullRequestId}/merge"
      println url

      println simplePUT(url, [commit_message: 'close'  ])
  }
  /**
   * Create a new issue
   */
  def createIssue(title, comment, assignee){
      simplePost("${API_BASE}${REPO_INFO}issues",
        [title: title, body: comment, assignee :assignee  ])
  }

  /**
   * Create a new comment in a pull request
   */
  def createPullRequestComment(pullRequest, comment){
      simplePost(
      "${API_BASE}${REPO_INFO}issues/${pullRequest}/comments",
      [body: comment])
  }

  /**
   * Create a new comment in a commit
   */
  def createCommitComment(commidId, comment){
      simplePost(
      "${API_BASE}${REPO_INFO}commits/${commidId}/comments",
      [body: comment])
  }

  /**
   * Generate a report with the number of pull request per branch in this week
   */
  def reportPullRuquestPerBranch(branchName) {

    branchName = java.net.URLEncoder.encode(branchName)
    def parameters = "per_page=100&state=all&base=${branchName}"
    def url = "${API_BASE}${REPO_INFO}pulls?${parameters}"

	def result = getRowsInThisWeek(url)
    def reportName = "pullRequestPerBranch_${branchName}.csv"
    def data = [
    'Week' : todayWeek,
    'Passed' : 0,
    'Failed' : 0,
    'Not Validated' : 0,
    'Total' : result.size() ]

    result.each{
        def statusList = parse simpleGet(it.statuses_url)
        def resentStatus = statusList.max { it.created_at }
        if( resentStatus ) {
            def state = resentStatus.state
            switch(state){
                case 'success' : data.'Passed'++
                break
                case 'failure' : data.'Failed'++
                break
                default: data.'Not Validated'++
                break
            }
        }
        else { data.'Not Validated'++ }
    }

    List reportData = [ data ]

  }

 /**
   * Retrive the link in base to a link name from
   * the GitHub Link header
   */
  def getLinkFromHeader( linkName, headerValue){
    def links = [:]
    headerValue.split(',').each{
      def linkRow = it.split(';')
      def name = linkRow[1]
      def link = linkRow[0]
      // clean name
      name = name.replaceAll(/[^"]*"([^"]*)"/, '$1').trim()
      link = link.replaceAll(/[^<]*<([^>]*)>/, '$1')

      links[name]=link
    }
    links[linkName]
  }

  def simplePost(url, message){
      simpleRequest('POST', url, message)
  }
  def simplePUT(url, message){
      simpleRequest('PUT', url, message)
  }

  def simpleRequest(httpOperation, url, message){
      def connection = url.toURL().openConnection()
      String basicAuth = "token " + access_token
      connection.setRequestProperty ("Authorization", basicAuth)
      connection.setRequestMethod(httpOperation)

      def urlParameters = JsonOutput.toJson(message)
      // Send post request
      connection.setDoOutput(true)
      DataOutputStream wr = new DataOutputStream(connection.getOutputStream())
      wr.writeBytes(urlParameters)
      wr.flush()
      wr.close()

      connection.getResponseCode()
  }

  def simpleGet(url, callBack = {} ){
    String basicauth = "token " + access_token
    def connection = url.toURL().openConnection()
    connection.setRequestProperty ("Authorization", basicauth)
    connection.setRequestMethod("GET")
    BufferedReader br = new BufferedReader(
      new InputStreamReader(connection.getInputStream()))
    String inputline
    StringBuffer response = new StringBuffer()
        while ((inputline = br.readLine()) != null) {
        response.append(inputline)
    }
    callBack(connection)
    br.close()
    response.toString()
  }

  /**
   * Retrieve all the statuses per pull request
   */
  def  getPullRequestStatuses(pullRequestId){
      String url="${API_BASE}${REPO_INFO}pulls/${pullRequestId}"
      String pullRequest = parse simpleGet(url)
      String statusUrl = pullRequest.statuses_url

       // Return the list of statuses
       parse simpleGet(statusUrl)
  }

  def parse(jsonString){
	slurper.parseText(jsonString)
  }

}
