<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="octopus.teamcity.server.connection.OctopusConnectionUiData" %>

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

<%--
  Space picker, shared by the connection form and every step form.

  A text field that suggests the connection's spaces as you type. Choosing a space, or typing its
  exact name, stores the space's *id* alongside the name; the id survives the space being renamed
  in Octopus. Anything else typed - including a parameter reference such as %octopus.space%, which
  TeamCity resolves when the build runs - is stored as the name alone.

  The property names are the same on a connection and on a step, so OctopusConstants supplies both
  regardless of which page is including this. The including page passes its own help text as the
  "note" param.
--%>

<jsp:useBean id="spaceKeys" class="octopus.teamcity.common.OctopusConstants"/>
<%
  pageContext.setAttribute(
      "spacePickerProjectId", OctopusConnectionUiData.spacePickerProjectId(request));
  pageContext.setAttribute(
      "spacePickerConnectionId", OctopusConnectionUiData.spacePickerConnectionId(request));
  pageContext.setAttribute(
      "spacePickerBuildTypeId", OctopusConnectionUiData.spacePickerBuildTypeId(request));
  pageContext.setAttribute(
      "spacePickerScriptUrl", OctopusConnectionUiData.spacePickerScriptUrl(request));
%>

<tr class="octopusSpacePickerRow">
  <th>Space:</th>
  <td>
    <props:hiddenProperty name="${spaceKeys.spaceId}"/>
    <span class="octopusSpaceNameInput">
      <props:textProperty name="${spaceKeys.spaceName}" className="longField"/>
    </span>
    <span class="error" id="error_${spaceKeys.spaceName}"></span>
    <span class="error octopusSpaceMissing" style="display:none;"></span>
    <span class="smallNote octopusSpaceStatus"><span class="octopusSpaceStatusText"></span>
      <%-- Why the spaces could not be listed, shown in a tooltip; the reason is in data-reason. --%>
      <span class="octopusSpaceDetails" tabindex="0" role="button" style="display:none;"><bs:helpIcon/></span>
    </span>

    <%-- Config passed as HTML-escaped data attributes rather than interpolated into JS string
         literals, so nothing server-side can break out into script. --%>
    <span class="octopusSpacePickerMeta" style="display:none;"
          data-project-id="<c:out value='${spacePickerProjectId}'/>"
          data-lookup-url="<c:out value='${pageContext.request.contextPath}'/>/octopus/listSpaces.html"
          data-space-id-field="<c:out value='${spaceKeys.spaceId}'/>"
          data-connection-id="<c:out value='${spacePickerConnectionId}'/>"
          data-build-type-id="<c:out value='${spacePickerBuildTypeId}'/>"
          data-script-url="<c:out value='${spacePickerScriptUrl}'/>"></span>

    <span class="smallNote"><c:out value="${param.note}"/></span>
    <span class="smallNote octopusSpaceByIdNote" style="display:none;">
      Choose a space from the suggestions to store it by id, so renaming it in Octopus will not
      break this configuration.
    </span>
  </td>
</tr>

<%-- The behaviour lives in js/spacePicker.js. It is added to the page from here rather than with
     a script src tag, because the connection form arrives through an AJAX dialog that runs inline
     scripts but does not load external ones. --%>
<script type="text/javascript">
  (function () {
    function setUp() {
      window.OctopusSpacePicker.setupAll();
    }
    if (window.OctopusSpacePicker) {
      setUp();
      return;
    }
    let script = document.getElementById("octopusSpacePickerScript");
    if (!script) {
      script = document.createElement("script");
      script.id = "octopusSpacePickerScript";
      script.src = document.querySelector(".octopusSpacePickerMeta").getAttribute("data-script-url");
      document.head.appendChild(script);
    }
    script.addEventListener("load", setUp);
  })();
</script>
