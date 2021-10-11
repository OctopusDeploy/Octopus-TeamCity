<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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

<jsp:useBean id="connectionKeys"
             class="octopus.teamcity.common.connection.ConnectionPropertyNames"/>
<jsp:useBean id="keys" class="octopus.teamcity.common.commonstep.CommonStepPropertyNames"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="params" class="octopus.teamcity.server.generic.BuildStepCollection"/>


<jsp:useBean id="teamcityPluginResourcesPath" scope="request" type="java.lang.String"/>
<c:set var="paramHelpUrl">net#</c:set>
<c:set var="commandTitle">Command:<bs:help file="${paramHelpUrl}BuildRunnerOptions"/></c:set>

<l:settingsGroup title="Octopus Connection">
    <tr>
        <th>Octopus URL:<l:star/></th>
        <td>
            <props:textProperty name="${connectionKeys.serverUrlPropertyName}" className="longField"/>
            <span class="error" id="error_${connectionKeys.serverUrlPropertyName}"></span>
            <span class="smallNote">Specify Octopus web portal URL</span>
        </td>
    </tr>
    <tr>
        <th>API key:<l:star/></th>
        <td>
            <props:passwordProperty name="${connectionKeys.apiKeyPropertyName}" className="longField"/>
            <span class="error" id="error_${connectionKeys.apiKeyPropertyName}"></span>
            <span class="smallNote">Specify Octopus API key. You can get this from your user page in the Octopus web portal.</span>
        </td>
    </tr>
    <tr>
        <th>Space name:</th>
        <td>
            <props:textProperty name="${keys.spaceNamePropertyName}" className="longField"/>
            <span class="error" id="error_${keys.spaceNamePropertyName}"></span>
            <span class="smallNote">Specify Octopus Space name. Leave blank to use the default space.</span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th>Verbose logging:</th>
        <td>
            <props:checkboxProperty name="${keys.verboseLoggingPropertyName}"/>
            <span class="error" id="error_${keys.verboseLoggingPropertyName}"></span>
            <span class="smallNote">Set this to get more verbose logging.</span>
        </td>
    </tr>

</l:settingsGroup>

<l:settingsGroup title="Proxy Server">
    <props:selectSectionProperty name="${connectionKeys.proxyRequiredPropertyName}" title="Proxy Server Requried" note="">
        <props:selectSectionPropertyContent value="false" caption="<No Proxy Required>"/>
            <props:selectSectionPropertyContent value="true" caption="Use Proxy Server">
                <jsp:include page="${teamcityPluginResourcesPath}/v2/subpages/editProxyParameters.jsp"/>
            </props:selectSectionPropertyContent>
    </props:selectSectionProperty>
</l:settingsGroup>


<l:settingsGroup title="Operation">
    <props:selectSectionProperty name="${keys.stepTypePropertyName}" title="${commandTitle}" note="">
        <c:forEach items="${params.subSteps}" var="type">
            <props:selectSectionPropertyContent value="${type.name}" caption="${type.description}">
                <jsp:include page="${teamcityPluginResourcesPath}/v2/subpages/${type.editPage}"/>
            </props:selectSectionPropertyContent>
        </c:forEach>
    </props:selectSectionProperty>
</l:settingsGroup>
