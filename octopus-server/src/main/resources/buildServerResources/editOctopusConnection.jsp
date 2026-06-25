<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="octopus.teamcity.server.connection.OctopusOidcConnectorsUiData" %>

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
<c:set var="selectedApiKeySource" value="${empty propertiesBean.properties[keys.apiKeySourcePropertyName] ? 'key' : propertiesBean.properties[keys.apiKeySourcePropertyName]}"/>
<%
  pageContext.setAttribute("oidcConnectors", OctopusOidcConnectorsUiData.availableConnectors(request));
%>

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
  <th>API key source:</th>
  <td>
    <props:selectProperty name="${keys.apiKeySourcePropertyName}" id="octopusApiKeySource" className="longField">
      <props:option value="key" selected="${selectedApiKeySource == 'key'}">Enter an API key</props:option>
      <props:option value="parameter" selected="${selectedApiKeySource == 'parameter'}">Reference a parameter</props:option>
      <c:if test="${not empty oidcConnectors}">
        <props:option value="oidc" selected="${selectedApiKeySource == 'oidc'}">Use an OIDC token</props:option>
      </c:if>
    </props:selectProperty>
    <span class="smallNote">How this connection supplies credentials to Octopus.</span>
  </td>
</tr>
<tr class="octopusApiKeyField octopusApiKeySource-key">
  <th>API key:<l:star/></th>
  <td>
    <props:passwordProperty name="${keys.apiKeyPropertyName}" className="longField"/>
    <span class="error" id="error_${keys.apiKeyPropertyName}"></span>
    <span class="smallNote">
      Create a <a href="https://oc.to/service-accounts">service account</a> in the
      Octopus web portal.
    </span>
  </td>
</tr>
<tr class="octopusApiKeyField octopusApiKeySource-parameter">
  <th>API key parameter:<l:star/></th>
  <td>
    <props:textProperty name="${keys.apiKeyParameterPropertyName}" className="longField"/>
    <span class="error" id="error_${keys.apiKeyParameterPropertyName}"></span>
    <span class="smallNote">A single parameter reference, e.g. <code>%octopus.apikey%</code>. Keep the secret in that parameter.</span>
  </td>
</tr>
<c:if test="${not empty oidcConnectors}">
  <tr class="octopusApiKeyField octopusApiKeySource-oidc">
    <th>OIDC connector:<l:star/></th>
    <td>
      <props:selectProperty name="${keys.oidcConnectionIdPropertyName}" className="longField">
        <props:option value="">-- Select an OIDC connector --</props:option>
        <c:forEach var="connector" items="${oidcConnectors}">
          <props:option value="${connector.id}"><c:out value="${connector.displayName}"/></props:option>
        </c:forEach>
      </props:selectProperty>
      <span class="error" id="error_${keys.oidcConnectionIdPropertyName}"></span>
      <span class="smallNote">
        An OIDC Identity Token connector. Using OIDC switches this connection to the new Octopus CLI
        (the current standard; the legacy CLI does not support OIDC).
      </span>
    </td>
  </tr>
</c:if>
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
    <span class="smallNote">Space name - optional. If not provided, individual build steps can specify.</span>
  </td>
</tr>

<script type="text/javascript">
  (function () {
    function toggleApiKeySourceFields() {
      var select = document.getElementById("octopusApiKeySource");
      if (!select) return;
      var source = select.value;
      var rows = document.querySelectorAll("tr.octopusApiKeyField");
      for (var i = 0; i < rows.length; i++) {
        var matches = rows[i].className.indexOf("octopusApiKeySource-" + source) !== -1;
        rows[i].style.display = matches ? "table-row" : "none";
      }
    }

    $j(document).ready(function () {
      var select = document.getElementById("octopusApiKeySource");
      if (select) {
        select.addEventListener("change", toggleApiKeySourceFields);
        toggleApiKeySourceFields();
      }
    });
  })();
</script>
