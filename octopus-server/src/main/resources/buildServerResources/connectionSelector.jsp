<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="octopus.teamcity.server.connection.OctopusConnectionUiData" %>
<jsp:useBean id="keys" class="octopus.teamcity.common.OctopusConstants"/>

<%
  pageContext.setAttribute("octopusConnections", OctopusConnectionUiData.availableConnections(request));
  pageContext.setAttribute("editConnectionUrl", OctopusConnectionUiData.editConnectionUrl(request));
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
              data-conn-version="<c:out value='${conn.version}'/>"></span>
      </c:forEach>
    </span>
    <span class="smallNote">
      Reuse a connection defined under
      <a href="${editConnectionUrl}" target="_blank">Project Settings &raquo; Connections</a>.
    </span>
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

    $j(document).ready(function () {
      const select = document.getElementById("octopusConnectionId");
      if (select) {
        select.addEventListener("change", toggleOctopusInlineConnectionFields);
        toggleOctopusInlineConnectionFields();
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
