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
          data-connection-id="<c:out value='${spacePickerConnectionId}'/>"></span>

    <span class="smallNote"><c:out value="${param.note}"/></span>
    <span class="smallNote octopusSpaceByIdNote" style="display:none;">
      Choose a space from the suggestions to store it by id, so renaming it in Octopus will not
      break this configuration.
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
      const nameInput = cell.querySelector(".octopusSpaceNameInput");
      const status = cell.querySelector(".octopusSpaceStatus");
      const missing = cell.querySelector(".octopusSpaceMissing");
      const byIdNote = cell.querySelector(".octopusSpaceByIdNote");
      const statusText = cell.querySelector(".octopusSpaceStatusText");
      const details = cell.querySelector(".octopusSpaceDetails");
      if (!meta || !nameInput || !statusText || !details || cell.dataset.octopusBound === "true") return;
      cell.dataset.octopusBound = "true";

      // The props taglib renders these fields with name="prop:<property>" and id="<property>".
      // Look the field up both ways so this does not hinge on that prefix.
      const idFieldName = meta.getAttribute("data-space-id-field");
      const idField =
        cell.querySelector("input[name='prop:" + idFieldName + "']")
        || cell.querySelector("input[name='" + idFieldName + "']")
        || document.getElementById(idFieldName);
      const nameField = nameInput.querySelector("input[type='text']");
      if (!nameField) return;

      // A step form has a connection selector; the connection form does not.
      const connectionSelect = document.getElementById("octopusConnectionId");
      let lookupSequence = 0;
      // The connection's spaces, or null while they are not known.
      let spaces = null;

      function setStatus(message, isError) {
        statusText.textContent = message || "";
        status.className = isError ? "smallNote error octopusSpaceStatus" : "smallNote octopusSpaceStatus";
        showDetails("");
      }

      function showDetails(reason) {
        details.setAttribute("data-reason", reason);
        details.setAttribute("aria-label", reason);
        details.style.display = reason ? "" : "none";
      }

      function showDetailsTooltip() {
        const reason = details.getAttribute("data-reason");
        if (!reason || !(window.BS && BS.Tooltip)) return;
        BS.Tooltip.hidePopup();
        // The tooltip takes HTML, and the reason can quote admin-entered values.
        BS.Tooltip.showMessage(details, { shift: { x: 10, y: 20 }, delay: 0 }, escapeHtml(reason));
      }

      function hideDetailsTooltip() {
        if (window.BS && BS.Tooltip) BS.Tooltip.hidePopup();
      }

      details.addEventListener("mouseover", showDetailsTooltip);
      details.addEventListener("click", showDetailsTooltip);
      details.addEventListener("focus", showDetailsTooltip);
      details.addEventListener("mouseout", hideDetailsTooltip);
      details.addEventListener("blur", hideDetailsTooltip);

      function showMissing(message) {
        missing.textContent = message || "";
        missing.style.display = message ? "" : "none";
      }

      // TeamCity resolves a parameter reference when the build runs, so it cannot be checked here.
      function isParameterReference(text) {
        return text.indexOf("%") !== -1;
      }

      function spaceWithId(id) {
        for (let i = 0; spaces && i < spaces.length; i++) {
          if (spaces[i].id === id) return spaces[i];
        }
        return null;
      }

      function spaceNamed(name) {
        const wanted = name.trim().toLowerCase();
        for (let i = 0; spaces && wanted && i < spaces.length; i++) {
          if (spaces[i].name.toLowerCase() === wanted) return spaces[i];
        }
        return null;
      }

      // Typing a space's exact name picks that space; anything else abandons the stored id, which
      // would otherwise silently win over the name.
      function storeIdForTypedName() {
        const space = spaceNamed(nameField.value);
        if (idField) idField.value = space ? space.id : "";
      }

      function checkTypedName() {
        const name = nameField.value.trim();
        if (spaces && name && !isParameterReference(name) && !spaceNamed(name)) {
          showMissing("No space named '" + name + "' was found in Octopus.");
        } else {
          showMissing("");
        }
      }

      function checkSavedSpace() {
        const storedId = idField ? idField.value : "";
        const storedName = nameField.value.trim();
        if (storedId) {
          const space = spaceWithId(storedId);
          if (space) {
            // Shows the space's current name, which differs from the saved one after a rename.
            nameField.value = space.name;
            showMissing("");
          } else {
            showMissing("The saved space " + describe(storedName, storedId)
              + " was not found. Please enter the space name.");
          }
          return;
        }
        // A configuration that only stored a name gains the id when it is next saved.
        storeIdForTypedName();
        checkTypedName();
      }

      function describe(name, id) {
        if (name && id) return "'" + name + "' (" + id + ")";
        return "'" + (name || id) + "'";
      }

      function suggestionsFor(term) {
        if (!spaces || isParameterReference(term)) return [];
        const wanted = term.trim().toLowerCase();
        const matches = [];
        for (let i = 0; i < spaces.length; i++) {
          const space = spaces[i];
          if (space.name.toLowerCase().indexOf(wanted) === -1) continue;
          matches.push({
            // TeamCity's autocomplete inserts the label as HTML, and a space name is admin-supplied.
            label: escapeHtml(space.name + " (" + space.id + ")"),
            value: space.name,
            id: space.id,
            selectable: true
          });
        }
        return matches;
      }

      function escapeHtml(text) {
        const holder = document.createElement("span");
        holder.textContent = text;
        return holder.innerHTML;
      }

      function csrfToken() {
        const tokenMeta = document.querySelector("meta[name='tc-csrf-token']");
        return tokenMeta ? tokenMeta.getAttribute("content") || "" : "";
      }

      // On the connection form, the connection being edited is identified by the page itself once
      // it has been saved.
      function savedConnectionId() {
        if (connectionSelect) return connectionSelect.value || "";
        return meta.getAttribute("data-connection-id") || "";
      }

      function cannotList(reason, isError) {
        spaces = null;
        byIdNote.style.display = "none";
        showMissing("");
        if (isError === false) {
          setStatus(reason, false);
          return;
        }
        setStatus("Unable to retrieve spaces, please enter space name manually", true);
        showDetails(reason);
      }

      // The lookup names a saved connection and nothing else - it never sends a URL or key. The
      // server reads those from the stored connection, so it is never asked to fetch an address
      // supplied by this page.
      function load() {
        const sequence = ++lookupSequence;
        const connectionId = savedConnectionId();
        if (!connectionId) {
          cannotList(connectionSelect
            ? "Select a connection to choose from its spaces."
            : "Save this connection to choose from its spaces.", false);
          return;
        }
        spaces = null;
        byIdNote.style.display = "none";
        showMissing("");
        setStatus("Loading spaces…", false);
        $j.ajax({
          url: meta.getAttribute("data-lookup-url"),
          type: "POST",
          data: {
            projectId: meta.getAttribute("data-project-id"),
            connectionId: connectionId
          },
          dataType: "json",
          // TeamCity's own scripts add this header to jQuery requests only once the page has
          // finished initialising, and this lookup runs before that.
          headers: { "X-TC-CSRF-Token": csrfToken() }
        }).done(function (data) {
          // A slower answer for an earlier connection must not replace the current one.
          if (sequence !== lookupSequence) return;
          if (data && data.error) {
            cannotList(data.error);
            return;
          }
          const listed = (data && data.spaces) || [];
          if (!listed.length) {
            cannotList("Octopus returned no spaces for this connection.");
            return;
          }
          spaces = listed;
          setStatus("", false);
          byIdNote.style.display = "";
          checkSavedSpace();
        }).fail(function (xhr) {
          if (sequence !== lookupSequence) return;
          let message = "Could not list spaces.";
          try {
            const parsed = JSON.parse(xhr.responseText);
            if (parsed && parsed.error) message = parsed.error;
          } catch (ignored) { /* fall back to the generic message */ }
          cannotList(message);
        });
      }

      if ($j.fn.autocomplete) {
        const field = $j(nameField);
        // TeamCity's parameter completion, if this page has the parts of it used here. They are
        // TeamCity internals, so a field without them - or where they fail - offers spaces only.
        const parameters = window.BS && BS.AvailableParams;
        const hasParameterCompletion = !!parameters
          && ["buildSourceUrl", "sliceForCompletion", "sourceForElement", "moreForElement",
              "selectForElement"].every(function (name) {
            return typeof parameters[name] === "function";
          });

        // A step form's text fields have TeamCity's own autocomplete, which completes parameter
        // references after a %, and a field can only have one. So this one autocomplete offers
        // both: TeamCity's parameters while a reference is being typed, and spaces otherwise.
        // TeamCity sets its autocomplete up on first focus unless the field is marked as done.
        nameField._autocompleteInited = true;

        function parameterSourceUrl() {
          return parameters.buildSourceUrl(field.data("editableObjId"));
        }

        function completingParameter() {
          if (!hasParameterCompletion) return false;
          try {
            return !!parameterSourceUrl()
              && (parameters.sliceForCompletion(field) !== null || !!field.data("ctrlSpace"));
          } catch (ignored) {
            return false;
          }
        }

        // Entering the field offers every space, whatever it already holds. Registered before the
        // autocomplete, whose own focus handler runs the search.
        let offerEverySpace = false;
        nameField.addEventListener("focus", function () {
          offerEverySpace = true;
        });

        field.autocomplete({
          minLength: 0,
          delay: 0,
          showOnFocus: true,
          showEmpty: false,
          insertItemWithFocus: false,
          completeOnCtrlSpace: true,
          source: function (request, respond) {
            const everySpace = offerEverySpace;
            offerEverySpace = false;
            if (completingParameter()) {
              try {
                parameters.sourceForElement(field, parameterSourceUrl(), true)(request, respond);
                return;
              } catch (ignored) { /* offer spaces instead */ }
            }
            field.data("sliced_text", null);
            respond(suggestionsFor(everySpace ? "" : request.term));
          },
          more: function (from, respond) {
            try {
              if (field.data("sliced_text")) {
                parameters.moreForElement(field, parameterSourceUrl(), true)(from, respond);
                return;
              }
            } catch (ignored) { /* nothing more to offer */ }
            respond(null);
          },
          select: function (event, ui) {
            if (field.data("sliced_text")) {
              try {
                parameters.selectForElement(field).call(this, event, ui);
              } catch (ignored) { /* the typed text is left as it is */ }
              storeIdForTypedName();
              showMissing("");
              return false;
            }
            nameField.value = ui.item.value;
            if (idField) idField.value = ui.item.id;
            showMissing("");
            return false;
          }
        });
      }

      nameField.addEventListener("input", function () {
        storeIdForTypedName();
        showMissing("");
      });
      nameField.addEventListener("change", function () {
        storeIdForTypedName();
        checkTypedName();
      });

      if (connectionSelect) connectionSelect.addEventListener("change", load);
      load();
    }
  })();
</script>
