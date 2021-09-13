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

<jsp:useBean id="params" class="octopus.teamcity.server.generic.SubStepCollection"/>
<jsp:useBean id="keys" class="octopus.teamcity.common.buildinfo.BuildInfoKeys"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>


<l:settingsGroup title="Package">

    <tr>
        <th>Package IDs:<l:star/></th>a
        <td>
            <props:multilineProperty name="${keys.packageIdKey}" rows="5" cols="50" linkTitle="Package IDs"
                                     expanded="true"/>
            <span class="error" id="error_${keys.packageIdKey}"></span>
            <span class="smallNote">Newline-separated package IDs; e.g.<br/>MyCompany.MyApp<br/>MyCompany.MyApp2</span>
        </td>
    </tr>
    <tr>
        <th>Package version:<l:star/></th>
        <td>
            <props:textProperty name="${keys.packageVersionKey}" className="longField"/>
            <span class="error" id="error_${keys.packageVersionKey}"></span>
            <span class="smallNote">The package's version.</span>
        </td>
    </tr>

    <tr class="advancedSetting">
        <th>Overwrite Mode:</th>
        <td>
            <props:selectProperty name="${keys.overwriteModeKey}">
                <c:forEach items="${params.overwriteModes}" var="item">
                    <props:option value="${item.key}">${item.value}</props:option>
                </c:forEach>
            </props:selectProperty>
            <span class="error" id="error_${keys.overwriteModeKey}"></span>
            <span class="smallNote">Normally, if the same build information already exists on the server, the server will reject the build information push. This is a good practice as it ensures build information isn't accidentally overwritten or ignored. Use this setting to override this behavior.</span>
        </td>
    </tr>
</l:settingsGroup>
