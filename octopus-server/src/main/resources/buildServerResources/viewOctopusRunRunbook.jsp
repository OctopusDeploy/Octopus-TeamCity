<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="keys" class="octopus.teamcity.common.OctopusConstants"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
    Octopus URL:
    <strong><props:displayValue name="${keys.serverKey}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Space name:
    <strong><props:displayValue name="${keys.spaceName}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Project:
    <strong><props:displayValue name="${keys.projectNameKey}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Runbook:
    <strong><props:displayValue name="${keys.runbookNameKey}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Environment(s):
    <strong><props:displayValue name="${keys.deployToKey}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Snapshot:
    <strong><props:displayValue name="${keys.runbookSnapshotKey}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Show runbook run progress:
    <strong><props:displayValue name="${keys.waitForDeployments}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Time to wait for runbook run:
    <strong><props:displayValue name="${keys.deploymentTimeout}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Cancel runbook run on timeout:
    <strong><props:displayValue name="${keys.cancelDeploymentOnTimeout}" emptyValue="not specified"/></strong>
</div>
