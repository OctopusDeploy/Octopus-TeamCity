<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="params" class="octopus.teamcity.server.generic.BuildStepCollection"/>
<jsp:useBean id="keys" class="octopus.teamcity.common.createrelease.CreateReleasePropertyNames"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>


<l:settingsGroup title="Release Details">
    <tr>
        <th>Project name:<l:star/></th>a
        <td>
            <props:textProperty name="${keys.projectNamePropertyName}" />
            <span class="error" id="error_${keys.projectNamePropertyName}"></span>
            <span class="smallNote">The project's name</span>
        </td>
    </tr>
    <tr>
        <th>Package version:<l:star/></th>a
        <td>
            <props:textProperty name="${keys.packageVersionPropertyName}" />
            <span class="error" id="error_${keys.packageVersionPropertyName}"></span>
            <span class="smallNote">The package version to release</span>
        </td>
    </tr>
    <tr>
        <th>Release version:</th>a
        <td>
            <props:textProperty name="${keys.releaseVersionPropertyName}" />
            <span class="error" id="error_${keys.releaseVersionPropertyName}"></span>
            <span class="smallNote">The release version</span>
        </td>
    </tr>
    <tr>
        <th>Channel name:</th>a
        <td>
            <props:textProperty name="${keys.channelNamePropertyName}" />
            <span class="error" id="error_${keys.channelNamePropertyName}"></span>
            <span class="smallNote">The channel name to release to</span>
        </td>
    </tr>
    <tr>
        <th>Packages:<l:star/></th>
        <td>
            <props:multilineProperty name="${keys.packagesPropertyName}" rows="5" cols="55" expanded="true" linkTitle="Enter list of package strings"/>
            <span class="error" id="error_${keys.packagesPropertyName}"></span>
            <span class="smallNote">Newline separated list of packages in the format - <em>PackageID:Version</em> or <em>StepName:PackageName:Version</em>.</span>
        </td>
    </tr>
</l:settingsGroup>
