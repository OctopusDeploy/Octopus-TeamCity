<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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

<%-- Space field shared by the connection form and every step form; both store the space under the
     same property name. The including page passes its help text as the "note" param. --%>
<jsp:useBean id="spaceKeys" class="octopus.teamcity.common.OctopusConstants"/>

<tr>
  <th>Space name:</th>
  <td>
    <props:textProperty name="${spaceKeys.spaceName}" className="longField"/>
    <span class="error" id="error_${spaceKeys.spaceName}"></span>
    <span class="smallNote"><c:out value="${param.note}"/></span>
  </td>
</tr>
