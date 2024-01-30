import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript
import com.onresolve.scriptrunner.runner.util.UserMessageUtil
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.security.groups.GroupManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.search.SearchRequest
import com.atlassian.jira.bc.portal.PortalPageService
import com.atlassian.jira.bc.filter.SearchRequestService
import com.atlassian.jira.bc.JiraServiceContext
import com.atlassian.jira.bc.JiraServiceContextImpl
import com.atlassian.jira.component.ComponentAccessor
import java.time.LocalDateTime
import com.atlassian.jira.security.login.LoginManager
import java.time.ZoneId
@BaseScript CustomEndpointDelegate delegate

ppsrmgr(httpMethod: "GET", groups: ["jira-administrators"]) { MultivaluedMap queryParams ->
    def userName = queryParams.getFirst("user") as String
    def type = queryParams.getFirst("type") as String
    UserManager userManager = ComponentAccessor.getComponent(UserManager)
    PortalPageService portalPageService = ComponentAccessor.getComponent(PortalPageService)
    SearchRequestService searchRequestService = ComponentAccessor.getComponent(SearchRequestService)
    GroupManager groupManager = ComponentAccessor.getComponent(GroupManager)
    LoginManager loginManager = ComponentAccessor.getComponent(LoginManager.class)
    
    ApplicationUser user = userManager.getUserByName(userName)
    boolean deleteAll = false
    JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(user)
    if (userName.equals("override"))
        {
            int days = queryParams.getFirst("days") as int
            groupManager.getUserNamesInGroup("inactive-users").each{ iaUser ->
                ComponentAccessor.jiraAuthenticationContext.setLoggedInUser(ComponentAccessor.userManager.getUserByName(iaUser))
                ApplicationUser inactiveUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
                JiraServiceContext iaJiraServiceContext = new JiraServiceContextImpl(inactiveUser)
                def aMonthAgo = days == 0 ? LocalDateTime.now() : LocalDateTime.now().minusDays(days)
                def lastLoginAttempt = loginManager.getLoginInfo(iaUser).getLastLoginTime()
                if (lastLoginAttempt)
                    {
                        def lastSuccessfulLogin = new Date(lastLoginAttempt).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        if (lastSuccessfulLogin?.isBefore(aMonthAgo))
                            {
                                switch(type)
                                {
                                    case "Private Dashboards Only":
                                    portalPageService.getOwnedPortalPages(inactiveUser).minus(portalPageService.getNonPrivatePortalPages(inactiveUser)).each{ privatePortalPages ->
                                        portalPageService.deletePortalPage(iaJiraServiceContext,privatePortalPages.id)
                                    }
                                    break
                                    case "Private Filters Only":
                                    searchRequestService.getOwnedFilters(inactiveUser).minus(searchRequestService.getNonPrivateFilters(inactiveUser)).each{ privateSearchRequest ->
                                        searchRequestService.deleteFilter(iaJiraServiceContext, privateSearchRequest.getId())
                                    }
                                    break
                                    case "Both Private Dashboards and Filters":
                                    portalPageService.getOwnedPortalPages(inactiveUser).minus(portalPageService.getNonPrivatePortalPages(inactiveUser)).each{ privatePortalPages ->
                                        portalPageService.deletePortalPage(iaJiraServiceContext,privatePortalPages.id)
                                    }

                                    searchRequestService.getOwnedFilters(inactiveUser).minus(searchRequestService.getNonPrivateFilters(inactiveUser)).each{ privateSearchRequest ->
                                        searchRequestService.deleteFilter(iaJiraServiceContext, privateSearchRequest.getId())
                                    }
                                    break
                                    case "Public Dashboards Only":
                                    portalPageService.getNonPrivatePortalPages(inactiveUser).each{ publicPortalPages ->
                                        portalPageService.deletePortalPage(iaJiraServiceContext,publicPortalPages.id)
                                    }
                                    break
                                    case "Public Filters Only":
                                    searchRequestService.getNonPrivateFilters(inactiveUser).each{ publicSearchRequest ->
                                        searchRequestService.deleteFilter(iaJiraServiceContext, publicSearchRequest.getId())
                                    }
                                    break
                                    case "Both Public Dashboards and Filters":
                                    portalPageService.getNonPrivatePortalPages(inactiveUser).each{ publicPortalPages ->
                                        portalPageService.deletePortalPage(iaJiraServiceContext,publicPortalPages.id)
                                    }

                                    searchRequestService.getNonPrivateFilters(inactiveUser).each{ publicSearchRequest ->
                                        searchRequestService.deleteFilter(iaJiraServiceContext, publicSearchRequest.getId())
                                    }
                                    break
                                    case "All Dashboards":
                                    portalPageService.deleteAllPortalPagesForUser(inactiveUser)
                                    break
                                    case "All Filters":
                                    searchRequestService.deleteAllFiltersForUser(iaJiraServiceContext, inactiveUser)
                                    break
                                    case "All Dashboards and Filters":
                                    portalPageService.deleteAllPortalPagesForUser(inactiveUser)
                                    searchRequestService.deleteAllFiltersForUser(iaJiraServiceContext, inactiveUser)
                                    break
                                }
                            }
                    }
        }
        }
    else
        {
            switch(type)
                {
                    case "Private Dashboards Only":
                    portalPageService.getOwnedPortalPages(user).minus(portalPageService.getNonPrivatePortalPages(user)).each{ privatePortalPages ->
                    	portalPageService.deletePortalPage(jiraServiceContext,privatePortalPages.id)
                    }
                    break
                    case "Private Filters Only":
                    searchRequestService.getOwnedFilters(user).minus(searchRequestService.getNonPrivateFilters(user)).each{ privateSearchRequest ->
                        searchRequestService.deleteFilter(jiraServiceContext, privateSearchRequest.getId())
                    }
                    break
                    case "Both Private Dashboards and Filters":
                    portalPageService.getOwnedPortalPages(user).minus(portalPageService.getNonPrivatePortalPages(user)).each{ privatePortalPages ->
                        portalPageService.deletePortalPage(jiraServiceContext,privatePortalPages.id)
                    }

                    searchRequestService.getOwnedFilters(user).minus(searchRequestService.getNonPrivateFilters(user)).each{ privateSearchRequest ->
                        searchRequestService.deleteFilter(jiraServiceContext, privateSearchRequest.getId())
                    }
                    break
                    case "Public Dashboards Only":
                    portalPageService.getNonPrivatePortalPages(user).each{ publicPortalPages ->
                        portalPageService.deletePortalPage(jiraServiceContext,publicPortalPages.id)
                    }
                    break
                    case "Public Filters Only":
                    searchRequestService.getNonPrivateFilters(user).each{ publicSearchRequest ->
                        searchRequestService.deleteFilter(jiraServiceContext, publicSearchRequest.getId())
                    }
                    break
                    case "Both Public Dashboards and Filters":
                    portalPageService.getNonPrivatePortalPages(user).each{ publicPortalPages ->
                        portalPageService.deletePortalPage(jiraServiceContext,publicPortalPages.id)
                    }

                    searchRequestService.getNonPrivateFilters(user).each{ publicSearchRequest ->
                        searchRequestService.deleteFilter(jiraServiceContext, publicSearchRequest.getId())
                    }
                    break
                    case "All Dashboards":
                    portalPageService.deleteAllPortalPagesForUser(user)
                    break
                    case "All Filters":
                    searchRequestService.deleteAllFiltersForUser(jiraServiceContext, user)
                    break
                    case "All Dashboards and Filters":
                    portalPageService.deleteAllPortalPagesForUser(user)
                    searchRequestService.deleteAllFiltersForUser(jiraServiceContext, user)
                    break
                }
        }
    
def redirect = "/rest/scriptrunner/latest/custom/dfmgr/${userName}"
return Response.ok().type(MediaType.TEXT_HTML).entity(redirect.toString()).build()
}