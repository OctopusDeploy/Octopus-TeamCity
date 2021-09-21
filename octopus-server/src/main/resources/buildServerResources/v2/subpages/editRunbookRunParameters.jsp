<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="params" class="octopus.teamcity.server.generic.BuildStepCollection"/>
<jsp:useBean id="keys" class="octopus.teamcity.common.runbookrun.RunbookRunPropertyNames"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>


<l:settingsGroup title="Runbook Details">
    <tr>
        <th>Runbook name:<l:star/></th>
        <td>
            <props:textProperty name="${keys.runbookNamePropertyName}" />
            <span class="error" id="error_${keys.runbookNamePropertyName}"></span>
            <span class="smallNote">The runbook's name</span>
        </td>
    </tr>
    <tr>
        <th>Project name:<l:star/></th>
        <td>
            <props:textProperty name="${keys.projectNamePropertyName}" />
            <span class="error" id="error_${keys.projectNamePropertyName}"></span>
            <span class="smallNote">The project's name</span>
        </td>
    </tr>
    <tr>
        <th>Environments:<l:star/></th>
        <td>
            <props:multilineProperty name="${keys.environmentNamesPropertyName}" rows="5" cols="55" expanded="true" linkTitle="Enter list of environment names"/>
            <span class="error" id="error_${keys.environmentNamesPropertyName}"></span>
            <span class="smallNote">Newline separated environment names</span>
        </td>
    </tr>
    <tr>
        <th>Snapshot name:</th>
        <td>
            <props:textProperty name="${keys.snapshotNamePropertyName}" />
            <span class="error" id="error_${keys.snapshotNamePropertyName}"></span>
            <span class="smallNote">The snapshot's name</span>
        </td>
    </tr>
</l:settingsGroup>
