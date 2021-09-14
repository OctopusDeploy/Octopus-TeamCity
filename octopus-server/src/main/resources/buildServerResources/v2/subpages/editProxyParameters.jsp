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

<jsp:useBean id="keys" class="octopus.teamcity.common.commonstep.CommonStepPropertyNames"/>
<jsp:useBean id="params" class="octopus.teamcity.server.generic.BuildStepCollection"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<tr>
    <th>Proxy URL:<l:star/></th>
    <td>
        <props:textProperty name="${keys.proxyServerUrlPropertyName}" className="longField"/>
        <span class="error" id="error_${keys.proxyServerUrlPropertyName}"></span>
        <span class="smallNote">Specify the location of the proxy.</span>
    </td>
</tr>
<tr>
    <th>Proxy Username:</th>
    <td>
        <props:textProperty name="${keys.proxyUsernamePropertyName}" className="longField"/>
        <span class="error" id="error_${keys.proxyUsernamePropertyName}"></span>
        <span class="smallNote">Specify the username required to authenticate to the proxy
            .</span>
    </td>
</tr>
<tr>
    <th>Proxy Password:</th>
    <td>
        <props:passwordProperty name="${keys.proxyPasswordPropertyName}" className="longField"/>
        <span class="error" id="error_${keys.proxyPasswordPropertyName}"></span>
        <span class="smallNote">Specify the password rqeuired to authenticate to the
            proxy.</span>
    </td>
</tr>
