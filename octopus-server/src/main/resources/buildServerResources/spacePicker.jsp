<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
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

  Stores the chosen space's *id*, which survives the space being renamed in Octopus. The name is
  stored alongside it so the UI and the connection description stay readable.

  The free-text name field is deliberately retained. An API key held in a build parameter, and an
  OIDC token, only exist while a build runs, so spaces cannot be listed at edit time for those
  connections - typing a name stays the fallback, and is what older configurations already use.

  The property names are the same on a connection and on a step, so OctopusConstants supplies both
  regardless of which page is including this.
--%>

<jsp:useBean id="spaceKeys" class="octopus.teamcity.common.OctopusConstants"/>
<%
  pageContext.setAttribute(
      "spacePickerProjectId", OctopusConnectionUiData.spacePickerProjectId(request));
%>

<tr class="octopusSpacePickerRow">
  <th>Space:</th>
  <td>
    <props:hiddenProperty name="${spaceKeys.spaceId}"/>
    <props:textProperty name="${spaceKeys.spaceName}" className="longField"/>
    <span class="error" id="error_${spaceKeys.spaceName}"></span>

    <%-- Config passed as HTML-escaped data attributes rather than interpolated into JS string
         literals, so nothing server-side can break out into script. --%>
    <span class="octopusSpacePickerMeta" style="display:none;"
          data-project-id="<c:out value='${spacePickerProjectId}'/>"
          data-lookup-url="<c:out value='${pageContext.request.contextPath}'/>/octopus/listSpaces.html"
          data-space-id-field="<c:out value='${spaceKeys.spaceId}'/>"></span>

    <div class="octopusSpacePickerControls" style="margin-top:4px;">
      <button type="button" class="btn btn_mini octopusLoadSpaces">Load spaces</button>
      <select class="longField octopusSpaceSelect" style="display:none; margin-left:4px;"></select>
      <span class="smallNote octopusSpaceStatus" style="margin-left:4px;"></span>
    </div>

    <span class="smallNote">
      Optional. <strong>Load spaces</strong> lists the spaces on the Octopus server and stores the
      one you pick by id, so renaming that space in Octopus will not break this configuration. You
      can type a space name instead, which is required when the API key comes from a build
      parameter or from OIDC, because those credentials only exist while a build runs.
    </span>
    <span class="smallNote octopusSpaceIdNote" style="display:none;">
      Stored space id: <code class="octopusSpaceIdValue"></code>
    </span>
  </td>
</tr>

<script type="text/javascript">
  (function () {
    const cells = document.querySelectorAll("tr.octopusSpacePickerRow td");
    for (let c = 0; c < cells.length; c++) {
      setupOctopusSpacePicker(cells[c]);
    }

    function setupOctopusSpacePicker(cell) {
      const meta = cell.querySelector(".octopusSpacePickerMeta");
      const button = cell.querySelector(".octopusLoadSpaces");
      const select = cell.querySelector(".octopusSpaceSelect");
      const status = cell.querySelector(".octopusSpaceStatus");
      const idNote = cell.querySelector(".octopusSpaceIdNote");
      const idValue = cell.querySelector(".octopusSpaceIdValue");
      if (!meta || !button || !select || button.dataset.octopusBound === "true") return;
      button.dataset.octopusBound = "true";

      const idFieldName = meta.getAttribute("data-space-id-field");
      const idField = cell.querySelector("input[name='" + idFieldName + "']");
      const nameField = cell.querySelector("input.longField[type='text']");
      if (!nameField) return;

      function showStoredId() {
        const current = idField ? idField.value : "";
        if (idNote) idNote.style.display = current ? "" : "none";
        if (idValue) idValue.textContent = current;
      }

      function setStatus(message, isError) {
        if (!status) return;
        status.textContent = message || "";
        status.className = isError ? "smallNote error octopusSpaceStatus" : "smallNote octopusSpaceStatus";
      }

      function lookupRequest() {
        const params = {projectId: meta.getAttribute("data-project-id")};
        const connectionSelect = document.getElementById("octopusConnectionId");
        if (connectionSelect && connectionSelect.value) {
          // A saved connection: the server reads the stored credentials itself, because TeamCity
          // renders a saved API key as a placeholder and this page never holds the real one.
          params.connectionId = connectionSelect.value;
          return params;
        }
        const sourceEl = document.getElementById("octopusApiKeySource");
        if (sourceEl) params.apiKeySource = sourceEl.value;
        const urlEl = document.querySelector("input[name='octopus_host']");
        const keyEl = document.querySelector("input[name='secure:octopus_apikey']");
        if (urlEl) params.serverUrl = urlEl.value;
        if (keyEl) params.apiKey = keyEl.value;
        return params;
      }

      function populate(spaces) {
        select.innerHTML = "";
        const placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = spaces.length ? "-- select a space --" : "-- no spaces found --";
        select.appendChild(placeholder);
        const currentId = idField ? idField.value : "";
        for (let i = 0; i < spaces.length; i++) {
          const space = spaces[i];
          const option = document.createElement("option");
          option.value = space.id;
          // textContent and a data attribute: a space name is admin-supplied data, never markup.
          option.textContent = space.name + " (" + space.id + ")";
          option.setAttribute("data-space-name", space.name);
          if (space.id === currentId) option.selected = true;
          select.appendChild(option);
        }
        select.style.display = "";
      }

      button.addEventListener("click", function (event) {
        event.preventDefault();
        setStatus("Loading spaces…", false);
        $j.ajax({
          url: meta.getAttribute("data-lookup-url"),
          type: "POST",
          data: lookupRequest(),
          dataType: "json"
        }).done(function (data) {
          if (data && data.error) {
            setStatus(data.error, true);
            return;
          }
          const spaces = (data && data.spaces) || [];
          populate(spaces);
          setStatus(spaces.length ? "" : "No spaces returned.", !spaces.length);
        }).fail(function (xhr) {
          let message = "Could not list spaces.";
          try {
            const parsed = JSON.parse(xhr.responseText);
            if (parsed && parsed.error) message = parsed.error;
          } catch (ignored) { /* fall back to the generic message */ }
          setStatus(message, true);
        });
      });

      select.addEventListener("change", function () {
        const option = select.options[select.selectedIndex];
        if (!option || !option.value) return;
        if (idField) idField.value = option.value;
        // Keep the readable name in step with the id that was just stored.
        nameField.value = option.getAttribute("data-space-name") || "";
        showStoredId();
        setStatus("", false);
      });

      // Typing a name by hand abandons the stored id, which would otherwise silently win.
      nameField.addEventListener("input", function () {
        if (idField && idField.value) {
          idField.value = "";
          showStoredId();
          setStatus("Using the typed space name.", false);
        }
      });

      showStoredId();
    }
  })();
</script>
