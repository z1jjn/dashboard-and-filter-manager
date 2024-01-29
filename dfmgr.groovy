import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.servlet.http.HttpServletRequest

import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.security.groups.GroupManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.bc.portal.PortalPageService
import com.atlassian.jira.bc.filter.SearchRequestService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.JiraServiceContext
import com.atlassian.jira.bc.JiraServiceContextImpl
import com.atlassian.jira.bc.user.search.UserSearchService
import com.atlassian.jira.bc.user.search.UserSearchParams
import java.time.LocalDateTime
import com.atlassian.jira.security.login.LoginManager
import java.time.ZoneId
import java.time.temporal.ChronoUnit
@BaseScript CustomEndpointDelegate delegate

dfmgr(httpMethod: "GET", groups: ["jira-administrators"]) { MultivaluedMap queryParams, String body, HttpServletRequest request ->
   	
    UserManager userManager = ComponentAccessor.getComponent(UserManager)
    PortalPageService portalPageService = ComponentAccessor.getComponent(PortalPageService)
    SearchRequestService searchRequestService = ComponentAccessor.getComponent(SearchRequestService)
    GroupManager groupManager = ComponentAccessor.getComponent(GroupManager)
    UserSearchService userSearchService = ComponentAccessor.getComponent(UserSearchService)
    LoginManager loginManager = ComponentAccessor.getComponent(LoginManager.class)
    
    StringBuilder publicDashboards = new StringBuilder()
    StringBuilder publicFilters = new StringBuilder()
    StringBuilder privateDashboards = new StringBuilder()
    StringBuilder privateFilters = new StringBuilder()
    StringBuilder userName = new StringBuilder()
    StringBuilder dataList = new StringBuilder()
    StringBuilder deleteList = new StringBuilder()
    StringBuilder overrideDeleteList = new StringBuilder()
    
    String dashboardFilterManagerHeader, dashboardFilterManagerDialog, form, deleteForm, overrideScript, overrideDelete
    
    int publicPortalPagesFlag, publicSearchRequestFlag, privatePortalPagesFlag, privateSearchRequestFlag = 0
    
  	def cs = """<p style="font-size:12px;text-align:right;">Jira Dashboard and Filter Manager 1.0.0-stable</p>"""
	def baseurl = com.atlassian.jira.component.ComponentAccessor.getApplicationProperties().getString("jira.baseurl")
	def extraPath = getAdditionalPath(request)
    
    dataList <<= """<datalist name="userlist" id="userlist">"""
    
    UserSearchParams params = new UserSearchParams.Builder() 
    	.allowEmptyQuery(true)
    	.maxResults(10000)
        .includeActive(true)
        .includeInactive(true)
        .sorted(true)
    	.ignorePermissionCheck(true)
        .build()
    List<ApplicationUser> userList = userSearchService.findUsers("",params)
    userList.each{ user ->
        dataList <<= """<option value="${user.name}">"""
    }
    dataList <<= """</datalist>"""
    
    form = """
    <form class="aui">
    &nbsp;<input class="text long-field" id="manager-result" type="text" placeholder="Input username then press enter." list="userlist" required>
    ${dataList}
    &nbsp;<button accesskey="r" id="manager-submit" class="aui-button aui-button-primary" title="You can also press alt+r.">Go</button>
    </form>
    <script>
    AJS.\$(document).on('click', '#manager-submit', function() {
    var selectedUser = AJS.\$('#manager-result').val();
    switch(selectedUser)
    	{
        	default:
            window.location.href = "${baseurl}/rest/scriptrunner/latest/custom/dfmgr/"+selectedUser;
            return false;
            break;
        }
    });
    </script>
    """
    
    dashboardFilterManagerHeader =
    """
    <head>
    <style>
    /* Style the tab */
    .tab {
      overflow: hidden;
      border: 1px solid #ccc;
      background-color: #f1f1f1;
    }

    /* Style the buttons inside the tab */
    .tab button {
      background-color: inherit;
      float: left;
      border: none;
      outline: none;
      cursor: pointer;
      padding: 14px 16px;
      transition: 0.3s;
      font-size: 17px;
    }

    /* Change background color of buttons on hover */
    .tab button:hover {
      background-color: #ddd;
    }

    /* Create an active/current tablink class */
    .tab button.active {
      background-color: #ccc;
    }

    /* Style the tab content */
    .tabcontent {
      display: none;
      padding: 6px 12px;
      border: 1px solid #ccc;
      border-top: none;
    }
    table {
        border-collapse: collapse;
        width: 100%;
        font-size: 14px;
        font-family: -apple-system, "Helvetica Neue", "Helvetica", "Arial";
    }
    th, td {
        padding: 8px;
        text-align: center;
        border-bottom: 1px solid #ddd;
        font-family: -apple-system, "Helvetica Neue", "Helvetica", "Arial";
    }
    tr:hover {
        background-color:#f5f5f5;
    }
    html {
        scroll-behavior: smooth;
    }
    body {
        font-family: -apple-system, "Helvetica Neue", "Helvetica", "Arial";
        text-align: justify;
        background: #f4f5f7;
    }
    p {
        color:#302e30;
    }
    h1,h2,h3,h4,h5 {
        font-family: -apple-system-headline, "Helvetica Neue", "Helvetica", "Arial";
        font-weight: 700;
    }

    /* CHROME SCROLL BAR */
    .scroll {
        width: 20px;
        height: 200px;
        overflow: auto;
        float: left;
        margin: 0 10px;
    }
    ::-webkit-scrollbar {
        width: 5px;
    }
    ::-webkit-scrollbar-track {
        background: #ddd;
    }
    ::-webkit-scrollbar-thumb {
        background: #666; 
    }

    /*FOR FIREFOX*/
    .scroller {
        width: 20px;
        height: 200px;
        overflow: auto;
        float: left;
        margin: 0 10px;
        scrollbar-color: #666 #ddd;
        scrollbar-width: thin;
    }
    </style>
    <meta charset="UTF-8">
    <title>Jira Dashboard and Filter Manager</title>
	<link rel="shortcut icon" href="REPLACE WITH SHORTCUT ICON LINK">

    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/aui/8.6.0/aui/aui-prototyping.min.css">
    
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/aui/8.6.0/aui/aui-prototyping.min.js"></script>
    </head>
    """
    
    if(extraPath == "" || extraPath == "/")
        {
            dashboardFilterManagerDialog = """
            <body class="aui-layout aui-theme-default page-type-dashboard">
            <nav class="aui-header" role="navigation" data-aui-responsive="true">
            <strong style="font-family:'Charlie Display'; font-size:19px; vertical-align: middle; display: inline-block;">Jira Dashboard and Filter Manager</strong>
            </nav>
            <br>
            ${form}
            ${cs}
            </body>
            """
        }
    else
        {
            String stringName = URLDecoder.decode(extraPath,"UTF-8").toString().replaceAll("[^a-zA-Z0-9_]","")
    		ApplicationUser user = userManager.getUserByName(stringName)
            if (user)
                {
					if (portalPageService.getOwnedPortalPages(user).size() > 0 || searchRequestService.getOwnedFilters(user).size() > 0)
                        {
                            
                            if (portalPageService.getNonPrivatePortalPages(user).size() > 0)
                                {
                                    publicPortalPagesFlag = 1
									portalPageService.getNonPrivatePortalPages(user).each{ publicPortalPages ->
                            			publicDashboards <<= """<a href="${baseurl}/secure/Dashboard.jspa?selectPageId=${publicPortalPages.id}" target="_blank">&nbsp;${publicPortalPages.name}</a> <br>"""
                            		}
                                }
                            else
                                {
									publicDashboards <<= "&nbsp;No Public Dashboards."
                                }
                            

                            if (searchRequestService.getNonPrivateFilters(user).size() > 0)
                                {
                                    publicSearchRequestFlag = 1
                                    searchRequestService.getNonPrivateFilters(user).each{ publicSearchRequest ->
                                        publicFilters <<= """<a href="${baseurl}/issues/?filter=${publicSearchRequest.getId()}" target="_blank">&nbsp;${publicSearchRequest.name}</a> <br>"""
                                    }
                                }
                            else
                                {
									publicFilters <<= "&nbsp;No Public Filters."
                                }
                            
							if (portalPageService.getOwnedPortalPages(user).minus(portalPageService.getNonPrivatePortalPages(user)).size() > 0)
                                {
                                    privatePortalPagesFlag = 1
									portalPageService.getOwnedPortalPages(user).minus(portalPageService.getNonPrivatePortalPages(user)).each{ privatePortalPages ->
                                		privateDashboards <<= """<a href="${baseurl}/secure/Dashboard.jspa?selectPageId=${privatePortalPages.id}" target="_blank">&nbsp;${privatePortalPages.name}</a> <br>"""
                            		}
                                }
                            else
                                {
									privateDashboards <<= "&nbsp;No Private Dashboards."
                                }
                            
							if (searchRequestService.getOwnedFilters(user).minus(searchRequestService.getNonPrivateFilters(user)).size() > 0)
                                {
                                    privateSearchRequestFlag = 1
									searchRequestService.getOwnedFilters(user).minus(searchRequestService.getNonPrivateFilters(user)).each{ privateSearchRequest ->
                                		privateFilters <<= """<a href="${baseurl}/issues/?filter=${privateSearchRequest.getId()}" target="_blank">&nbsp;${privateSearchRequest.name}</a> <br>"""
                           			}
                                }
                            else
                                {
									privateFilters <<= "&nbsp;No Private Filters."
                                }
                            
                            if (privatePortalPagesFlag == 1)
                                {
									deleteList <<= """<aui-option>Private Dashboards Only</aui-option>"""
                                }
                            if (privateSearchRequestFlag == 1)
                                {
									deleteList <<= """<aui-option>Private Filters Only</aui-option>"""
                                }
                            if (privatePortalPagesFlag == 1 && privateSearchRequestFlag == 1)
                                {
									deleteList <<= """<aui-option>Both Private Dashboards and Filters</aui-option>"""
                                }
                            
                            if (publicPortalPagesFlag == 1)
                                {
									deleteList <<= """<aui-option>Public Dashboards Only</aui-option>"""
                                }
                            if (publicSearchRequestFlag == 1)
                                {
									deleteList <<= """<aui-option>Public Filters Only</aui-option>"""
                                }
                            if (publicPortalPagesFlag == 1 && publicSearchRequestFlag == 1)
                                {
									deleteList <<= """<aui-option>Both Public Dashboards and Filters</aui-option>"""
                                }
                            
                           	if (publicPortalPagesFlag == 1 && privatePortalPagesFlag == 1)
                                {
									deleteList <<= """<aui-option>All Dashboards</aui-option>"""
                                }
                           	if (publicSearchRequestFlag == 1 && privateSearchRequestFlag == 1)
                                {
									deleteList <<= """<aui-option>All Filters</aui-option>"""
                                }
                            if ((publicPortalPagesFlag == 1 && publicSearchRequestFlag == 1) && (privatePortalPagesFlag == 1 && privateSearchRequestFlag == 1))
                                {
									deleteList <<= """<aui-option>All Dashboards and Filters</aui-option>"""
                                }
                            publicPortalPagesFlag = 0
                            publicSearchRequestFlag = 0
                            privatePortalPagesFlag = 0
                            privateSearchRequestFlag = 0
                           	deleteForm = """
                            <form class="aui">
                            <aui-label for="manager-select">&nbsp;Choose what you want to delete.</aui-label>
                            <p>
                            	&nbsp;<aui-select id="manager-select" required>
                           			${deleteList}
                            	</aui-select>
                            &nbsp;<button id="manager-delete-submit" class="aui-button aui-button-primary">Delete</button>
                            </p>
                            </form>
    						"""
                            
                            dashboardFilterManagerDialog = """
                            <body class="aui-layout aui-theme-default page-type-dashboard">
                            <nav class="aui-header" role="navigation" data-aui-responsive="true">
                            <strong style="font-family:'Charlie Display'; font-size:19px; vertical-align: middle; display: inline-block;">Jira Dashboard and Filter Manager</strong>
                            </nav>
                            <br>
                            ${form}
                            <h3><strong>&nbsp;Manage ${user.displayName} (${user.name})'s Dasboards and Filters</strong></h3><br>
                            <h4>&nbsp;Public Dashboards</h4>
                            ${publicDashboards}
                            <h4>&nbsp;Public Filters</h4>
                            ${publicFilters}
                            <h4>&nbsp;Private Dashboards</h4>
                            ${privateDashboards}
                            <h4>&nbsp;Private Filters</h4>
                            ${privateFilters}
                            ${deleteForm}
                            ${cs}
                            
                            <script>
                            AJS.\$("#manager-delete-submit").on('click', function(e) {
							e.preventDefault();
                            var toDelete = AJS.\$("#manager-select").val()
                            
							if(toDelete.trim().length >= 1)
								{
                                	var request = AJS.\$.ajax({
                                    url: "/rest/scriptrunner/latest/custom/ppsrmgr",
                                    method: "GET",
                                    data: { 
                                    "user": "${user.name}",
                                    "type": toDelete,
                                    },
                                    beforeSend: function(){

                                    window.onbeforeunload = null;

                                    deleteFlag = AJS.flag({
                                        type: 'info',
                                        close: 'never',
                                        title: 'Deleting',
                                        body: "Please do not leave or refresh the page.",
                                    });
                                    },
                                    success: function(data){
                                    window.location.replace(data)
                                    },
                                    error: function(e){
                                    deleteFlag.close()
                                    AJS.flag({
                                        type: 'error',
                                        close: 'auto',
                                        title: 'Error',
                                        body: "Reloading the page in 5 seconds.",
                                    });
                                    setInterval(function() {
                                    window.location.reload(true)
                                    }, 5000);
                                    }
                                    });
    							}
                            else
                                {
                                    AJS.flag({
                                        type: 'error',
                                        close: 'auto',
                                        title: 'Error',
                                        body: "You must choose which to delete.",
                                    });
                                }
                            });
                            </script>
                            </body>
                            """
                        }
                    else
                        {
                            dashboardFilterManagerDialog = """
                            <body class="aui-layout aui-theme-default page-type-dashboard">
                            <nav class="aui-header" role="navigation" data-aui-responsive="true">
                            <strong style="font-family:'Charlie Display'; font-size:19px; vertical-align: middle; display: inline-block;">Jira Dashboard and Filter Manager</strong>
                            </nav>
                            <br>
                            ${form}
                            <h3><strong>&nbsp;${user.displayName} (${user.name}) has no dashboards and filters.</strong></h3>
            				${cs}
                            </body>
                            """
                        }
                }
            else if(stringName.equals("override"))
                {
                    ApplicationUser currentUser
                    int allFilters, allDashboards, privateFilterCount, privateDashboardCount, sharedFilterCount, sharedDashboardCount, nameFlag = 0;
					StringBuilder fullList = new StringBuilder()
                    
                    ComponentAccessor.getGroupManager().getUserNamesInGroup("inactive-users").each{ iaUser ->
                   	ComponentAccessor.jiraAuthenticationContext.setLoggedInUser(ComponentAccessor.userManager.getUserByName(iaUser))
                    currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
					nameFlag = 0
                    if (portalPageService.getNonPrivatePortalPages(currentUser).size() > 0)
                        {
                            nameFlag == 0 ? fullList <<= """<h5>&nbsp;${currentUser.displayName} (${currentUser.name})</h5><h6>&nbsp;It has been <strong>${ChronoUnit.DAYS.between(new Date(loginManager.getLoginInfo(iaUser).getLastLoginTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(), LocalDateTime.now())}</strong> days since ${currentUser.displayName} last logged in.</h6>""" : ""
                            nameFlag = 1
                            fullList <<= """<h5>&nbsp;Public Dashboards</h5>"""
                            portalPageService.getNonPrivatePortalPages(currentUser).each{ sharedPortalPages ->
                                fullList <<= """<a href="${baseurl}/secure/Dashboard.jspa?selectPageId=${sharedPortalPages.id}" target="_blank">&nbsp;${sharedPortalPages.name}</a> <br>"""
                            	sharedDashboardCount++
                            }
                        }
                     if (searchRequestService.getNonPrivateFilters(currentUser).size() > 0)
                        {
                            nameFlag == 0 ? fullList <<= """<h5>&nbsp;${currentUser.displayName} (${currentUser.name})</h5><h6>&nbsp;It has been <strong>${ChronoUnit.DAYS.between(new Date(loginManager.getLoginInfo(iaUser).getLastLoginTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(), LocalDateTime.now())}</strong> days since ${currentUser.displayName} last logged in.</h6>""" : ""
                            nameFlag = 1
                            fullList <<= """<h5>&nbsp;Public Filters</h5>"""
                            searchRequestService.getNonPrivateFilters(currentUser).each{ sharedSearchRequest ->
                                fullList <<= """<a href="${baseurl}/issues/?filter=${sharedSearchRequest.getId()}" target="_blank">&nbsp;${sharedSearchRequest.name}</a> <br>"""
                                sharedFilterCount++
                                    }
                        }
                     if (portalPageService.getOwnedPortalPages(currentUser).minus(portalPageService.getNonPrivatePortalPages(currentUser)).size() > 0)
                        {
                            nameFlag == 0 ? fullList <<= """<h5>&nbsp;${currentUser.displayName} (${currentUser.name})</h5><h6>&nbsp;It has been <strong>${ChronoUnit.DAYS.between(new Date(loginManager.getLoginInfo(iaUser).getLastLoginTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(), LocalDateTime.now())}</strong> days since ${currentUser.displayName} last logged in.</h6>""" : ""
                            nameFlag = 1
                            fullList <<= """<h5>&nbsp;Private Dashboards</h5>"""
                            portalPageService.getOwnedPortalPages(currentUser).minus(portalPageService.getNonPrivatePortalPages(currentUser)).each{ hiddenPortalPages ->
                                fullList <<= """<a href="${baseurl}/secure/Dashboard.jspa?selectPageId=${hiddenPortalPages.id}" target="_blank">&nbsp;${hiddenPortalPages.name}</a> <br>"""
                                privateDashboardCount++
                            }
                        }
                     if (searchRequestService.getOwnedFilters(currentUser).minus(searchRequestService.getNonPrivateFilters(currentUser)).size() > 0)
                        {
                            nameFlag == 0 ? fullList <<= """<h5>&nbsp;${currentUser.displayName} (${currentUser.name})</h5><h6>&nbsp;It has been <strong>${ChronoUnit.DAYS.between(new Date(loginManager.getLoginInfo(iaUser).getLastLoginTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(), LocalDateTime.now())}</strong> days since ${currentUser.displayName} last logged in.</h6>""" : ""
                            nameFlag = 1
                            fullList <<= """<h5>&nbsp;Private Filters</h5>"""
                            searchRequestService.getOwnedFilters(currentUser).minus(searchRequestService.getNonPrivateFilters(currentUser)).each{ hiddenSearchRequest ->
                                fullList <<= """<a href="${baseurl}/issues/?filter=${hiddenSearchRequest.getId()}" target="_blank">&nbsp;${hiddenSearchRequest.name}</a> <br>"""
                                privateFilterCount++
                            }
                        }
                        
                                    
                    }
                    allDashboards = sharedDashboardCount + privateDashboardCount
                    allFilters = sharedFilterCount + privateFilterCount
                    if (privateDashboardCount > 0)
                    {
                        overrideDeleteList <<= """<aui-option>Private Dashboards Only</aui-option>"""
                    }
                    if (privateFilterCount > 0)
                    {
                        overrideDeleteList <<= """<aui-option>Private Filters Only</aui-option>"""
                    }
                    if (privateDashboardCount > 0 && privateFilterCount > 0)
                    {
                        overrideDeleteList <<= """<aui-option>Both Private Dashboards and Filters</aui-option>"""
                    }

                    if (sharedDashboardCount > 0)
                    {
                        overrideDeleteList <<= """<aui-option>Public Dashboards Only</aui-option>"""
                    }
                    if (sharedFilterCount > 0)
                    {
                        overrideDeleteList <<= """<aui-option>Public Filters Only</aui-option>"""
                    }
                    if (sharedDashboardCount > 0 && sharedFilterCount > 0)
                    {
                        overrideDeleteList <<= """<aui-option>Both Public Dashboards and Filters</aui-option>"""
                    }

                    if (sharedDashboardCount > 0 && privateDashboardCount > 0)
                    {
                        overrideDeleteList <<= """<aui-option>All Dashboards</aui-option>"""
                    }
                    if (sharedFilterCount > 0 && privateFilterCount > 0)
                    {
                        overrideDeleteList <<= """<aui-option>All Filters</aui-option>"""
                    }
                    if ((sharedDashboardCount > 0 && sharedFilterCount > 0) && (privateDashboardCount > 0 && privateFilterCount > 0))
                    {
                        overrideDeleteList <<= """<aui-option>All Dashboards and Filters</aui-option>"""
                    }
                    overrideScript = """
                    <script>
                    AJS.\$("#manager-delete-submit").on('click', function(e) {
                    e.preventDefault();
                    var toDelete = AJS.\$("#manager-select").val()
                    var days = AJS.\$("#manager-days").val()
                    if(toDelete.trim().length >= 1)
                    	{
                        	var request = AJS.\$.ajax({
                            url: "/rest/scriptrunner/latest/custom/ppsrmgr",
                            method: "GET",
                            data: { 
                            "user": "override",
                            "type": toDelete,
                            "days": days,
                            },
                            beforeSend: function(){

							window.onbeforeunload = null;

							deleteFlag = AJS.flag({
                            type: 'info',
                            close: 'never',
                            title: 'Deleting',
                            body: "Please do not leave or refresh the page.",
                            });
                            },
                            success: function(data){
                            window.location.replace(data)
                            },
                            error: function(e){
                            deleteFlag.close()
                            AJS.flag({
                            type: 'error',
                            close: 'auto',
                            title: 'Error',
                            body: "Reloading the page in 5 seconds.",
                            });
                            setInterval(function() {
                            window.location.reload(true)
                            }, 5000);
                            }
                            });
    					}
                   	else
                     	{
                    		AJS.flag({
                            type: 'error',
                            close: 'auto',
                            title: 'Error',
                            body: "You must choose which to delete.",
                     		});
                     	}
                    	});
                    </script>
                    """
                    overrideDelete = """
                    <form class="aui">
                    	<aui-label for="manager-select">&nbsp;Choose what you want to delete.</aui-label>
                            <p>
                            	&nbsp;<aui-select id="manager-select" required>
                           			${overrideDeleteList}
                            	</aui-select><br>
                            &nbsp;Delete dashboards and filters of users who have been inactive for <input class="text short-field" id="manager-days" type="number" min="0" maxlength="2" pattern = "[0-9]" value="30" oninput="this.value = !!this.value && Math.abs(this.value) >= 0 ? Math.abs(this.value) : 30; this.value=this.value.slice(0,this.maxLength);"> or more days.<br>
                            &nbsp;<button id="manager-delete-submit" class="aui-button aui-button-primary">Delete</button>
                            </p>
                     </form>
                    """
					dashboardFilterManagerDialog = """
                    <body class="aui-layout aui-theme-default page-type-dashboard">
                    <nav class="aui-header" role="navigation" data-aui-responsive="true">
                    <strong style="font-family:'Charlie Display'; font-size:19px; vertical-align: middle; display: inline-block;">Jira Dashboard and Filter Manager</strong>
                    </nav>
                    <br>
                    ${form}
                    <h3><strong style="color:red;">&nbsp;Warning! This controls all dashboards and filters of all inactive users.</strong></h3>
                    <h4><strong>&nbsp;Inactive Users' Dashboards and Filters List</strong></h4>
                        <br>
                        &nbsp;All Dashboards of Inactive Users: ${allDashboards}<br>
                        &nbsp;Shared Dashboards of Inactive Users: ${sharedDashboardCount}<br>
                        &nbsp;Total Private Dashboards of Inactive Users: ${privateDashboardCount}
                        <br><br>
                        &nbsp;All Filters of Inactive Users: ${allFilters}<br>
                        &nbsp;Shared Filters of Inactive Users: ${sharedFilterCount}<br>
                        &nbsp;Total Private Filters of Inactive Users: ${privateFilterCount}
                        <br><br>
                        <strong style="color:red;">&nbsp;Stats aren't realtime.</strong> <button class="aui-button aui-button-link" onClick="window.location.reload();">Refresh Page</button>
                        <br>
                        ${(allDashboards + allFilters) > 0 ? overrideDelete : ""}
                        <br>
                        ${fullList}
                    ${cs}
                    
                    ${(allDashboards + allFilters) > 0 ? overrideScript : ""}
                    </body>
                    """
                }
            else
                {
					dashboardFilterManagerDialog = """
                    <body class="aui-layout aui-theme-default page-type-dashboard">
                    <nav class="aui-header" role="navigation" data-aui-responsive="true">
                    <strong style="font-family:'Charlie Display'; font-size:19px; vertical-align: middle; display: inline-block;">Jira Dashboard and Filter Manager</strong>
                    </nav>
                    <br>
                    ${form}
                    ${stringName.equals("") ? "<h3><strong>&nbsp;Invalid character.</strong></h3>" : "<h3><strong>&nbsp;${stringName} is not found in the user database.</strong></h3>"}
                    ${cs}
                    </body>
                    """
                }
        }
    Response.ok().type(MediaType.TEXT_HTML).entity(dashboardFilterManagerHeader.toString() + dashboardFilterManagerDialog.toString()).build()
}
