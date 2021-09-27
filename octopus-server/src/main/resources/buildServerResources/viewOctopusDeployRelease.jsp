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
    Release number:
    <strong><props:displayValue name="${keys.releaseNumberKey}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Deploy to:
    <strong><props:displayValue name="${keys.deployToKey}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Show deployment progress:
    <strong><props:displayValue name="${keys.waitForDeployments}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Time to wait for deployment:
    <strong><props:displayValue name="${keys.deploymentTimeout}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Cancel deployment on timeout:
    <strong><props:displayValue name="${keys.cancelDeploymentOnTimeout}" emptyValue="not specified"/></strong>
</div>
