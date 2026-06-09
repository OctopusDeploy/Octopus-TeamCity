<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--
  ~ Copyright (c) Octopus Deploy and contributors. All rights reserved.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License"); you may not use
  ~  these files except in compliance with the License. You may obtain a copy of the
  ~ License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software distributed
  ~ under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
  ~ CONDITIONS OF ANY KIND, either express or implied. See the License for the
  ~ specific language governing permissions and limitations under the License.
  --%>
<jsp:useBean id="keys" class="octopus.teamcity.common.connection.ConnectionPropertyNames"/>
<jsp:useBean id="versionKeys" class="octopus.teamcity.common.OctopusConstants"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<c:set var="selectedOctopusVersion" value="${propertiesBean.properties[keys.versionPropertyName]}"/>

<tr>
  <th>Connection Name:</th>
  <td>
    <props:textProperty name="${keys.displayName}" className="longField"/>
    <span class="error" id="error_${keys.displayName}"></span>
    <span class="smallNote">A uniquely distinguishable name for this connection.</span>
  </td>
</tr>
<tr>
  <th>Octopus URL:<l:star/></th>
  <td>
    <props:textProperty name="${keys.serverUrlPropertyName}" className="longField"/>
    <span class="error" id="error_${keys.serverUrlPropertyName}"></span>
    <span class="smallNote">Specify the Octopus server URL (e.g. http(s)://{hostname}:{port}).</span>
  </td>
</tr>
<tr>
  <th>API key:<l:star/></th>
  <td>
    <props:passwordProperty name="${keys.apiKeyPropertyName}" className="longField"/>
    <span class="error" id="error_${keys.apiKeyPropertyName}"></span>
    <span class="smallNote">
      Create a <a href="https://octopus.com/docs/security/users-and-teams/service-accounts">service account</a> in the
      Octopus web portal.
    </span>
  </td>
</tr>
<tr>
  <th>Octopus version:<l:star/></th>
  <td>
    <props:selectProperty name="${keys.versionPropertyName}" multiple="false">
      <c:forEach var="version" items="${versionKeys.octopusVersions}">
        <c:set var="selected" value="false"/>
        <c:if test="${selectedOctopusVersion == version}">
          <c:set var="selected" value="true"/>
        </c:if>
        <props:option value="${version}" selected="${selected}"><c:out value="${version}"/></props:option>
      </c:forEach>
    </props:selectProperty>
    <span class="error" id="error_${keys.versionPropertyName}"></span>
    <span class="smallNote">Which version of the Octopus Deploy server are you using?</span>
  </td>
</tr>
<tr>
  <th>Space name:</th>
  <td>
    <props:textProperty name="${keys.spaceNamePropertyName}" className="longField"/>
    <span class="error" id="error_${keys.spaceNamePropertyName}"></span>
    <span class="smallNote">Optional Space. Individual build steps may override this.</span>
  </td>
</tr>
