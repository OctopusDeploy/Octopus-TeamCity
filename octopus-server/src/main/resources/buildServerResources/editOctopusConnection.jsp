<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
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

<tr>
    <th>Connection Name:</th>
    <td>
        <props:textProperty name="${keys.displayName}" className="longField"/>
        <span class="error" id="error_displayName"></span>
        <span class="smallNote">Provide some name to distinguish this connection from others.</span>
    </td>
</tr>
<l:settingsGroup title="Server">
    <tr>
        <th>Octopus URL:<l:star/></th>
        <td>
            <props:textProperty name="${keys.serverUrlPropertyName}" className="longField"/>
            <span class="error" id="error_${keys.serverUrlPropertyName}"></span>
            <span class="smallNote">Specify Octopus API key. You can get this from your user page in the Octopus web portal.</span>
        </td>
    </tr>
    <tr>
        <th>API key:<l:star/></th>
        <td>
            <props:passwordProperty name="${keys.apiKeyPropertyName}" className="longField"/>
            <span class="error" id="error_${keys.apiKeyPropertyName}"></span>
            <span class="smallNote">Specify Octopus API key. You can get this from your user page in the Octopus web portal.</span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Proxy">
    <props:selectSectionProperty name="${keys.proxyRequiredPropertyName}" title="Proxy Server Requried" note="">
        <props:selectSectionPropertyContent value="false" caption="<No Proxy Required>"/>
        <props:selectSectionPropertyContent value="true" caption="Use Proxy Server">
            <jsp:include page="${teamcityPluginResourcesPath}/v2/subpages/editProxyParameters.jsp"/>
        </props:selectSectionPropertyContent>
    </props:selectSectionProperty>
</l:settingsGroup>
