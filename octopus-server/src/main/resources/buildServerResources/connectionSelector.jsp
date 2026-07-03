<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="octopus.teamcity.server.connection.OctopusConnectionUiData" %>
<jsp:useBean id="keys" class="octopus.teamcity.common.OctopusConstants"/>

<%
  pageContext.setAttribute("octopusConnections", OctopusConnectionUiData.availableConnections(request));
  pageContext.setAttribute("editConnectionUrl", OctopusConnectionUiData.editConnectionUrl(request));
  pageContext.setAttribute("buildFeaturesUrl", OctopusConnectionUiData.buildFeaturesUrl(request));
%>

<tr>
  <th>Connection:</th>
  <td>
    <props:selectProperty name="${keys.connectionIdKey}" id="octopusConnectionId" className="longField">
      <props:option value="">(Specify connection details inline)</props:option>
      <c:forEach var="conn" items="${octopusConnections}">
        <props:option value="${conn.id}"><c:out value="${conn.displayName}"/></props:option>
      </c:forEach>
    </props:selectProperty>
    <%-- Connection metadata exposed as HTML-escaped data attributes. JS reads these from the
         DOM rather than from server-interpolated JS string literals, so admin-entered space
         names / versions cannot break or inject script. --%>
    <span id="octopusConnectionMeta" style="display:none;">
      <c:forEach var="conn" items="${octopusConnections}">
        <span class="octopusConnMeta"
              data-conn-id="<c:out value='${conn.id}'/>"
              data-conn-space="<c:out value='${conn.space}'/>"
              data-conn-version="<c:out value='${conn.version}'/>"
              data-conn-oidc-warning="<c:out value='${conn.oidcWarning}'/>"
              data-conn-oidc-expected-var="<c:out value='${conn.oidcExpectedTokenVariable}'/>"></span>
      </c:forEach>
    </span>
    <span class="smallNote">
      Reuse a connection defined under
      <a href="${editConnectionUrl}" target="_blank">Project Settings &raquo; Connections</a>.
    </span>
    <div class="octopusOidcWarning octopusOidcFeatureMissing error" style="display:none;">
      This connection authenticates using OIDC, but this build configuration has no OIDC Identity
      Token build feature referencing its connector. Add one on the
      <c:choose>
        <c:when test="${not empty buildFeaturesUrl}"><a href="${buildFeaturesUrl}">Build Features</a></c:when>
        <c:otherwise>Build Features</c:otherwise>
      </c:choose>
      page, or the build will fail to authenticate.
    </div>
    <div class="octopusOidcWarning octopusOidcTokenMismatch error" style="display:none;">
      The OIDC Identity Token build feature for this connector publishes its token under a different
      variable name than this connection expects (<code class="octopusExpectedVar"></code>), so the
      build will not find the token. Set the feature's token variable name to
      <code class="octopusExpectedVar"></code>.
    </div>
  </td>
</tr>

<script type="text/javascript">
  (function () {
    function getOctopusConnectionMetadataFor(connId) {
      const nodes = document.querySelectorAll("#octopusConnectionMeta .octopusConnMeta");
      for (let i = 0; i < nodes.length; i++) {
        if (nodes[i].getAttribute("data-conn-id") === connId) {
          return nodes[i];
        }
      }
      return null;
    }

    function toggleOctopusInlineConnectionFields() {
      const select = document.getElementById("octopusConnectionId");
      if (!select) return;
      const usingConnection = select.value !== "";
      const rows = document.querySelectorAll("tr.octopusInlineConnectionField");
      for (let i = 0; i < rows.length; i++) {
        rows[i].style.display = usingConnection ? "none" : "table-row";
      }

      // The step's Space name field is hidden when the selected connection defines its own
      // space (the connection's space is then used), otherwise it stays visible so it can be set per step.
      const spaceField = document.getElementById("${keys.spaceName}");
      const spaceRow = spaceField ? spaceField.closest("tr") : null;
      if (spaceRow) {
        let connSpace = "";
        if (usingConnection) {
          const meta = getOctopusConnectionMetadataFor(select.value);
          connSpace = meta ? meta.getAttribute("data-conn-space") : "";
        }
        spaceRow.style.display = connSpace ? "none" : "table-row";
      }
    }

    function updateOctopusOidcWarning() {
      const select = document.getElementById("octopusConnectionId");
      if (!select) return;
      const missingEl = document.querySelector(".octopusOidcFeatureMissing");
      const mismatchEl = document.querySelector(".octopusOidcTokenMismatch");
      let warning = "none";
      let expectedVar = "";
      if (select.value !== "") {
        const meta = getOctopusConnectionMetadataFor(select.value);
        if (meta) {
          warning = meta.getAttribute("data-conn-oidc-warning") || "none";
          expectedVar = meta.getAttribute("data-conn-oidc-expected-var") || "";
        }
      }
      if (missingEl) {
        missingEl.style.display = warning === "feature-missing" ? "block" : "none";
      }
      if (mismatchEl) {
        mismatchEl.style.display = warning === "token-mismatch" ? "block" : "none";
        const varSpans = mismatchEl.querySelectorAll(".octopusExpectedVar");
        for (let i = 0; i < varSpans.length; i++) {
          varSpans[i].textContent = expectedVar;
        }
      }
    }

    $j(document).ready(function () {
      const select = document.getElementById("octopusConnectionId");
      if (select) {
        select.addEventListener("change", toggleOctopusInlineConnectionFields);
        select.addEventListener("change", updateOctopusOidcWarning);
        toggleOctopusInlineConnectionFields();
        updateOctopusOidcWarning();
      }
    });

    // Exposed so step-specific scripts (e.g. Create release git-ref) can react.
    window.octopusSelectedConnectionId = function () {
      const s = document.getElementById("octopusConnectionId");
      return s ? s.value : "";
    };
    window.octopusConnectionVersion = function (connId) {
      const meta = getOctopusConnectionMetadataFor(connId);
      return meta ? meta.getAttribute("data-conn-version") : "";
    };
  })();
</script>
