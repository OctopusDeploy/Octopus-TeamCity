<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="stepTypeKeys" class="octopus.teamcity.common.commonstep.StepTypeConstants"/>
<jsp:useBean id="teamcityPluginResourcesPath" scope="request" type="java.lang.String"/>
<jsp:useBean id="connectionKeys"
             class="octopus.teamcity.common.connection.ConnectionPropertyNames"/>
<jsp:useBean id="keys" class="octopus.teamcity.common.commonstep.CommonStepPropertyNames"/>
<jsp:useBean id="params" class="octopus.teamcity.server.generic.BuildStepCollection"/>
<jsp:useBean id="propertiesBean" scope="request"
             type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<c:set var="selectedOctopusVersion" value="${propertiesBean.properties['octopus_version']}"/>
<c:set var="proxyServerUrl" value="${propertiesBean.properties[connectionKeys.proxyServerUrlPropertyName]}"/>
<c:set var="proxyServerUser" value="${propertiesBean.properties[connectionKeys.proxyUsernamePropertyName]}"/>

<div class="parameter">
    Octopus URL:
    <strong><props:displayValue name="${connectionKeys.serverUrlPropertyName}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Space name:
    <strong><props:displayValue name="${keys.spaceNamePropertyName}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Proxy server required:
    <strong><props:displayValue name="${connectionKeys.proxyRequiredPropertyName}" emptyValue="not specified"/></strong>
</div>

<c:if test="${proxyServerUrl ne null and proxyServerUrl ne ''}">
    <div class="parameter">
        Proxy server URL:
        <strong><props:displayValue name="${connectionKeys.proxyServerUrlPropertyName}" emptyValue="not specified"/></strong>
    </div>
</c:if>

<c:if test="${proxyServerUser ne null and proxyServerUser ne ''}">
    <div class="parameter">
        Proxy server username:
        <strong><props:displayValue name="${connectionKeys.proxyUsernamePropertyName}" emptyValue="not specified"/></strong>
    </div>
</c:if>

<div class="parameter">
    Verbose logging:
    <strong><props:displayValue name="${keys.verboseLoggingPropertyName}" emptyValue="false"/></strong>
</div>

<div class="parameter">
    Sub command:
    <strong><props:displayValue name="${stepTypeKeys.stepTypePropertyName}" emptyValue="not specified"/></strong>
</div>

<c:forEach items="${params.subSteps}" var="type">
    <c:if test = "${type.name.equals(propertiesBean.properties[stepTypeKeys.stepTypePropertyName])}">
        <jsp:include page="${teamcityPluginResourcesPath}/v2/subpages/${type.viewPage}"/>
    </c:if>
</c:forEach>
