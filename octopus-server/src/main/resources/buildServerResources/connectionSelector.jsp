<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%-- Requires request attributes: octopusConnections (List<Map>), editConnectionUrl (String).
     Requires page bean: keys (OctopusConstants), and a Space field named ${keys.spaceName}. --%>

<tr>
  <th>Connection:</th>
  <td>
    <props:selectProperty name="${keys.connectionIdKey}" id="octopusConnectionId" className="longField">
      <props:option value="">-- Enter connection details manually --</props:option>
      <c:forEach var="conn" items="${octopusConnections}">
        <props:option value="${conn.id}"><c:out value="${conn.displayName}"/></props:option>
      </c:forEach>
    </props:selectProperty>
    <span class="smallNote">
      Reuse a connection defined under
      <a href="${editConnectionUrl}" target="_blank">Project Settings &raquo; Connections</a>,
      or leave blank to enter details manually below.
    </span>
  </td>
</tr>

<script type="text/javascript">
  (function () {
    var octopusConnSpaces = {};
    <c:forEach var="conn" items="${octopusConnections}">
      octopusConnSpaces["${conn.id}"] = "${conn.space}";
    </c:forEach>

    function octopusToggleManualFields() {
      var select = document.getElementById("octopusConnectionId");
      if (!select) return;
      var usingConnection = select.value !== "";
      var rows = document.querySelectorAll("tr.octopusManualField");
      for (var i = 0; i < rows.length; i++) {
        rows[i].style.display = usingConnection ? "none" : "table-row";
      }
      if (usingConnection) {
        var spaceField = document.getElementById("${keys.spaceName}");
        if (spaceField && spaceField.value === "" && octopusConnSpaces[select.value]) {
          spaceField.value = octopusConnSpaces[select.value];
        }
      }
    }

    $j(document).ready(function () {
      var select = document.getElementById("octopusConnectionId");
      if (select) {
        select.addEventListener("change", octopusToggleManualFields);
        octopusToggleManualFields();
      }
    });

    // Exposed so step-specific scripts (e.g. Create release git-ref) can react.
    window.octopusSelectedConnectionId = function () {
      var s = document.getElementById("octopusConnectionId");
      return s ? s.value : "";
    };
  })();
</script>
