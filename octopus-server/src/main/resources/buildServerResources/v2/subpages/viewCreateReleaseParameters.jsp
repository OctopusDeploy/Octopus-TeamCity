<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="keys" class="octopus.teamcity.common.createrelease.CreateReleasePropertyNames"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
    Project name:
    <strong><props:displayValue name="${keys.projectNamePropertyName}" emptyValue="not specified"/></strong>
</div>
<div class="parameter">
    Package version:
    <strong><props:displayValue name="${keys.packageVersionPropertyName}" emptyValue="not specified"/></strong>
</div>
<div class="parameter">
    Release version:
    <strong><props:displayValue name="${keys.releaseVersionPropertyName}" emptyValue="not specified"/></strong>
</div>
<div class="parameter">
    Channel name:
    <strong><props:displayValue name="${keys.channelNamePropertyName}" emptyValue="not specified"/></strong>
</div>
<div class="parameter">
    Packages:
    <strong><props:displayValue name="${keys.packagesPropertyName}" emptyValue="not specified"/></strong>
</div>