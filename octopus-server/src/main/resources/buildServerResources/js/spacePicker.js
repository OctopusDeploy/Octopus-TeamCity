/*
 * Copyright (c) Octopus Deploy and contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  these files except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/*
 * Behaviour for the space picker that spacePicker.jsp renders. The JSP loads this file once per
 * page and calls OctopusSpacePicker.setupAll(), which is safe to call again for markup added later.
 */
window.OctopusSpacePicker = window.OctopusSpacePicker || (function () {

  function escapeHtml(text) {
    const holder = document.createElement("span");
    holder.textContent = text;
    return holder.innerHTML;
  }

  // TeamCity resolves a parameter reference when the build runs, so it cannot be checked here.
  function isParameterReference(text) {
    return text.indexOf("%") !== -1;
  }

  function describe(name, id) {
    if (name && id) return "'" + name + "' (" + id + ")";
    return "'" + (name || id) + "'";
  }

  function csrfToken() {
    const tokenMeta = document.querySelector("meta[name='tc-csrf-token']");
    return tokenMeta ? tokenMeta.getAttribute("content") || "" : "";
  }

  /** The connection's spaces, or no spaces while they are not known. */
  function spaceList(spaces) {
    return {
      known: !!spaces,

      withId: function (id) {
        for (let index = 0; spaces && index < spaces.length; index++) {
          if (spaces[index].id === id) return spaces[index];
        }
        return null;
      },

      named: function (name) {
        const wanted = name.trim().toLowerCase();
        for (let index = 0; spaces && wanted && index < spaces.length; index++) {
          if (spaces[index].name.toLowerCase() === wanted) return spaces[index];
        }
        return null;
      },

      suggestionsFor: function (term) {
        if (!spaces || isParameterReference(term)) return [];
        const wanted = term.trim().toLowerCase();
        const matches = [];
        for (let index = 0; index < spaces.length; index++) {
          const space = spaces[index];
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
    };
  }

  /** The status line, its details tooltip, the missing-space warning and the by-id note. */
  function pickerView(cell) {
    const status = cell.querySelector(".octopusSpaceStatus");
    const statusText = cell.querySelector(".octopusSpaceStatusText");
    const details = cell.querySelector(".octopusSpaceDetails");
    const missing = cell.querySelector(".octopusSpaceMissing");
    const byIdNote = cell.querySelector(".octopusSpaceByIdNote");

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

    const view = {
      setStatus: function (message, isError) {
        statusText.textContent = message || "";
        status.className = isError ? "smallNote error octopusSpaceStatus" : "smallNote octopusSpaceStatus";
        showDetails("");
      },

      showMissing: function (message) {
        missing.textContent = message || "";
        missing.style.display = message ? "" : "none";
      },

      showByIdNote: function (shown) {
        byIdNote.style.display = shown ? "" : "none";
      },

      loading: function () {
        view.showByIdNote(false);
        view.showMissing("");
        view.setStatus("Loading spaces…", false);
      },

      listed: function () {
        view.setStatus("", false);
        view.showByIdNote(true);
      },

      cannotList: function (reason, isError) {
        view.showByIdNote(false);
        view.showMissing("");
        if (isError === false) {
          view.setStatus(reason, false);
          return;
        }
        view.setStatus("Unable to retrieve spaces, please enter space name manually", true);
        showDetails(reason);
      }
    };
    return view;
  }

  // The lookup names a saved connection and nothing else - it never sends a URL or key. The
  // server reads those from the stored connection, so it is never asked to fetch an address
  // supplied by this page.
  function requestSpaces(meta, connectionId, onSpaces, onFailure) {
    $j.ajax({
      url: meta.getAttribute("data-lookup-url"),
      type: "POST",
      data: {
        projectId: meta.getAttribute("data-project-id"),
        connectionId: connectionId,
        buildTypeId: meta.getAttribute("data-build-type-id") || ""
      },
      dataType: "json",
      // TeamCity's own scripts add this header to jQuery requests only once the page has
      // finished initialising, and this lookup runs before that.
      headers: { "X-TC-CSRF-Token": csrfToken() }
    }).done(function (data) {
      if (data && data.error) {
        onFailure(data.error);
        return;
      }
      const listed = (data && data.spaces) || [];
      if (!listed.length) {
        onFailure("Octopus returned no spaces for this connection.");
        return;
      }
      onSpaces(listed);
    }).fail(function (xhr) {
      let message = "Could not list spaces.";
      try {
        const parsed = JSON.parse(xhr.responseText);
        if (parsed && parsed.error) message = parsed.error;
      } catch (ignored) { /* fall back to the generic message */ }
      onFailure(message);
    });
  }

  /**
   * One autocomplete on the name field offering both TeamCity's parameters and the connection's
   * spaces. A step form's text fields have TeamCity's own autocomplete, which completes parameter
   * references after a %, and a field can only have one. So this one offers TeamCity's parameters
   * while a reference is being typed, and spaces otherwise.
   */
  function attachAutocomplete(nameField, callbacks) {
    if (!$j.fn.autocomplete) return;
    const field = $j(nameField);
    // TeamCity's parameter completion, if this page has the parts of it used here. They are
    // TeamCity internals, so a field without them - or where they fail - offers spaces only.
    const parameters = window.BS && BS.AvailableParams;
    const hasParameterCompletion = !!parameters
      && ["buildSourceUrl", "sliceForCompletion", "sourceForElement", "moreForElement",
          "selectForElement"].every(function (name) {
        return typeof parameters[name] === "function";
      });

    // TeamCity sets its autocomplete up on first focus unless the field is marked as done.
    nameField._autocompleteInited = true;

    function parameterSourceUrl() {
      return parameters.buildSourceUrl(field.data("editableObjId"));
    }

    function completingParameter() {
      if (!hasParameterCompletion) return false;
      try {
        return !!parameterSourceUrl()
          && (!!parameters.sliceForCompletion(field) || !!field.data("ctrlSpace"));
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
        respond(callbacks.suggestionsFor(everySpace ? "" : request.term));
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
          callbacks.parameterChosen();
          return false;
        }
        callbacks.spaceChosen(ui.item.value, ui.item.id);
        return false;
      }
    });
  }

  function setup(cell) {
    const meta = cell.querySelector(".octopusSpacePickerMeta");
    const nameInput = cell.querySelector(".octopusSpaceNameInput");
    if (!meta || !nameInput || !cell.querySelector(".octopusSpaceStatusText")
        || !cell.querySelector(".octopusSpaceDetails") || cell.dataset.octopusBound === "true") {
      return;
    }
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
    const view = pickerView(cell);
    let spaces = spaceList(null);
    let lookupSequence = 0;

    // Typing a space's exact name picks that space; anything else abandons the stored id, which
    // would otherwise silently win over the name.
    function storeIdForTypedName() {
      const space = spaces.named(nameField.value);
      if (idField) idField.value = space ? space.id : "";
    }

    function checkTypedName() {
      const name = nameField.value.trim();
      if (spaces.known && name && !isParameterReference(name) && !spaces.named(name)) {
        view.showMissing("No space named '" + name + "' was found in Octopus.");
      } else {
        view.showMissing("");
      }
    }

    function checkSavedSpace() {
      const storedId = idField ? idField.value : "";
      const storedName = nameField.value.trim();
      if (storedId) {
        const space = spaces.withId(storedId);
        if (space) {
          // Shows the space's current name, which differs from the saved one after a rename.
          nameField.value = space.name;
          view.showMissing("");
        } else {
          view.showMissing("The saved space " + describe(storedName, storedId)
            + " was not found. Please enter the space name.");
        }
        return;
      }
      // A configuration that only stored a name gains the id when it is next saved.
      storeIdForTypedName();
      checkTypedName();
    }

    // On the connection form, the connection being edited is identified by the page itself once
    // it has been saved.
    function savedConnectionId() {
      if (connectionSelect) return connectionSelect.value || "";
      return meta.getAttribute("data-connection-id") || "";
    }

    function cannotList(reason, isError) {
      spaces = spaceList(null);
      view.cannotList(reason, isError);
    }

    function load() {
      const sequence = ++lookupSequence;
      const connectionId = savedConnectionId();
      if (!connectionId) {
        cannotList(connectionSelect
          ? "Select a connection to choose from its spaces."
          : "Save this connection to choose from its spaces.", false);
        return;
      }
      spaces = spaceList(null);
      view.loading();
      requestSpaces(
        meta,
        connectionId,
        function (listed) {
          // A slower answer for an earlier connection must not replace the current one.
          if (sequence !== lookupSequence) return;
          spaces = spaceList(listed);
          view.listed();
          checkSavedSpace();
        },
        function (reason) {
          if (sequence !== lookupSequence) return;
          cannotList(reason);
        });
    }

    attachAutocomplete(nameField, {
      suggestionsFor: function (term) {
        return spaces.suggestionsFor(term);
      },
      parameterChosen: function () {
        storeIdForTypedName();
        view.showMissing("");
      },
      spaceChosen: function (name, id) {
        nameField.value = name;
        if (idField) idField.value = id;
        view.showMissing("");
      }
    });

    nameField.addEventListener("input", function () {
      storeIdForTypedName();
      view.showMissing("");
    });
    nameField.addEventListener("change", function () {
      storeIdForTypedName();
      checkTypedName();
    });

    // A space id belongs to one Octopus server, so it does not carry over to another connection.
    // The name is kept, and picks the same-named space if the new connection has one.
    function connectionChanged() {
      if (idField) idField.value = "";
      load();
    }

    // The connection form's lookup uses the saved settings, so once they are edited its spaces
    // may not be the ones the form would save. A space id belongs to one Octopus server, so a
    // new URL also drops it.
    function connectionSettingChanged(serverChanged) {
      if (serverChanged && idField) idField.value = "";
      if (!savedConnectionId()) return;
      lookupSequence++;
      cannotList("Save the changed settings to choose from this connection's spaces.", false);
    }

    function watchConnectionSettings() {
      const form = cell.closest("form") || document;
      const rows = form.querySelectorAll("tr.octopusConnectionSetting");
      for (let rowIndex = 0; rowIndex < rows.length; rowIndex++) {
        const serverChanged = rows[rowIndex].classList.contains("octopusConnectionServer");
        const fields = rows[rowIndex].querySelectorAll("input, select");
        for (let fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
          const onChange = function () {
            connectionSettingChanged(serverChanged);
          };
          fields[fieldIndex].addEventListener("input", onChange);
          fields[fieldIndex].addEventListener("change", onChange);
        }
      }
    }

    if (connectionSelect) {
      connectionSelect.addEventListener("change", connectionChanged);
    } else {
      watchConnectionSettings();
    }
    load();
  }

  return {
    setupAll: function () {
      const cells = document.querySelectorAll("tr.octopusSpacePickerRow td");
      for (let index = 0; index < cells.length; index++) {
        setup(cells[index]);
      }
    }
  };
})();
