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
<jsp:useBean id="params" class="octopus.teamcity.server.generic.BuildStepCollection"/>
<jsp:useBean id="keys" class="octopus.teamcity.common.pushpackage.PushPackagePropertyNames"/>
<jsp:useBean id="propertiesBean" scope="request"
             type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<l:settingsGroup title="Package Push">
    <tr>
        <th>Package paths:<l:star/></th>
        <td>
            <props:multilineProperty name="${keys.packagePathsPropertyName}" rows="5" cols="50"
                                     linkTitle="Package path patterns" expanded="true"/>
            <span class="error" id="error_${keys.packagePathsPropertyName}"></span>
            <span class="smallNote">
        Newline-separated paths of either package files, or wildcarded globs which match
      packaged files.
        NOTE: This only pushes packages, it does not create/package them from a file set.
      </span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th>Overwrite Mode:</th>
        <td>
            <props:selectProperty name="${keys.overwriteModePropertyName}">
                <c:forEach items="${params.overwriteModes}" var="item">
                    <props:option value="${item.key}">${item.value}</props:option>
                </c:forEach>
            </props:selectProperty>
            <span class="error" id="error_${keys.overwriteModePropertyName}"></span>
            <span class="smallNote">Normally, if the same package already exists on the server, the server will reject the package push. This is a good practice as it ensures a package isn't accidentally overwritten or ignored. Use this setting to override this behavior.</span>
        </td>
    </tr>
</l:settingsGroup>

<tr class="advancedSetting">
    <th>Use Delta Compression:</th>
    <td>
        <props:checkboxProperty name="${keys.deltaComparisonPropertyName}" disabled="true"/>
        <span class="error" id="error_${keys.deltaComparisonPropertyName}"></span>
        <span class="smallNote">Allows disabling of delta compression when uploading packages to the Octopus Server.</span>
        <span class="smallNote">To be enabled in a future release.</span>
    </td>
</tr>
