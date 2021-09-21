<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="keys" class="octopus.teamcity.common.runbookrun.RunbookRunPropertyNames"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
    Runbook name:
    <strong><props:displayValue name="${keys.runbookNamePropertyName}" emptyValue="not specified"/></strong>
</div>
<div class="parameter">
    Project name:
    <strong><props:displayValue name="${keys.projectNamePropertyName}" emptyValue="not specified"/></strong>
</div>
<div class="parameter">
    Environment names:
    <strong><props:displayValue name="${keys.environmentNamesPropertyName}" emptyValue="not specified"/></strong>
</div>
<div class="parameter">
    Snapshot name:
    <strong><props:displayValue name="${keys.snapshotNamePropertyName}" emptyValue="not specified"/></strong>
</div>