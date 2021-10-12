<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page import="jetbrains.buildServer.serverSide.auth.Permission" %>
<%--
  ~ Copyright 2000-2012 Octopus Deploy Pty. Ltd.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<jsp:useBean id="stepTypeKeys" class="octopus.teamcity.common.commonstep.StepTypeConstants"/>
<jsp:useBean id="keys" class="octopus.teamcity.common.commonstep.CommonStepPropertyNames"/>
<jsp:useBean id="propertiesBean" scope="request"
             type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="params" class="octopus.teamcity.server.generic.BuildStepCollection"/>
<jsp:useBean id="octopusConnections"
             type="octopus.teamcity.server.connection.OctopusConnectionsBean" scope="request"/>

<jsp:useBean id="teamcityPluginResourcesPath" scope="request" type="java.lang.String"/>
<jsp:useBean id="rootProject" scope="request" type="jetbrains.buildServer.serverSide.SProject"/>
<jsp:useBean id="editConnectionUrl" scope="request" type="java.lang.String"/>
<jsp:useBean id="user" scope="request" type="jetbrains.buildServer.users.User"/>
<c:set var="paramHelpUrl">net#</c:set>
<c:set var="commandTitle">Command:<bs:help file="${paramHelpUrl}BuildRunnerOptions"/></c:set>

<l:settingsGroup title="Octopus Connection">
    <tr>
        <th>Connection:<l:star/></th>
        <td>
            <c:choose>
                <c:when test="${empty octopusConnections.connections}">
                    No suitable Octopus connections were found.
                    <br/>
                    To receive notifications for all projects, configure Octopus connection in the
                    <c:choose>
                        <c:when test='${user.isPermissionGrantedForProject(rootProject.projectId, Permission.EDIT_PROJECT)}'>
                            <a href="${editConnectionUrl}">Root project's settings</a>.
                        </c:when>
                        <c:otherwise>
                            Root project's settings.
                        </c:otherwise>
                    </c:choose>
                    To receive notifications for a specific project, configure the connection directly in that project's settings instead.
                    <br/>
                </c:when>
                <c:otherwise>
                    <props:selectProperty
                            name="${keys.connectionIdPropertyName}"
                            id="${keys.connectionIdPropertyName.replace(':', '-')}"
                            className="longField">
                        <props:option value="">-- Select Octopus connection --</props:option>
                        <c:forEach var="connection" items="${octopusConnections.connections}">
                            <props:option value="${connection.id}">
                                <c:out value="${connection.connectionDisplayName}"/>
                            </props:option>
                        </c:forEach>
                    </props:selectProperty>
                </c:otherwise>
            </c:choose>

            <span class="error" id="error_${keys.connectionIdPropertyName}"></span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Operation">
    <tr>
        <th>Verbose logging:</th>
        <td>
            <props:checkboxProperty name="${keys.verboseLoggingPropertyName}" />
            <span class="error" id="error_${keys.verboseLoggingPropertyName}"></span>
            <span class="smallNote">Set this to get more verbose logging.</span>
        </td>
    </tr>

    <tr>
        <th>Space name:<l:star/></th>
        <td>
            <props:textProperty name="${keys.spaceNamePropertyName}" className="longField"/>
            <span class="error" id="error_${keys.spaceNamePropertyName}"></span>
            <span class="smallNote">Specify Octopus Space name.</span>
        </td>
    </tr>

    <props:selectSectionProperty name="${stepTypeKeys.stepTypePropertyName}" title="${commandTitle}"
                                 note="">
        <c:forEach items="${params.subSteps}" var="type">
            <props:selectSectionPropertyContent value="${type.name}" caption="${type.description}">
                <jsp:include page="${teamcityPluginResourcesPath}/v2/subpages/${type.editPage}"/>
            </props:selectSectionPropertyContent>
        </c:forEach>
    </props:selectSectionProperty>
</l:settingsGroup>
