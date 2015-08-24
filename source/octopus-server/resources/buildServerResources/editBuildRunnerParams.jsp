<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="keys" class="octopus.teamcity.common.OctopusConstants" />
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<l:settingsGroup title="Octopus Packaging">
<tr>
  <th><label for="${keys.runOctoPack}">Run OctoPack</label>:</th>
  <td>
    <props:checkboxProperty name="${keys.runOctoPack}" />
    <span class="error" id="error_${keys.runOctoPack}"></span>
    <span class="smallNote">If checked, any projects with OctoPack installed will be packaged.</span>
  </td>
</tr>
<tr>
  <th><label for="${keys.octoPackPackageVersion}">OctoPack package version</label>:</th>
  <td>
    <props:textProperty name="${keys.octoPackPackageVersion}" className="longField"/>
    <span class="error" id="error_${keys.octoPackPackageVersion}"></span>
    <span class="smallNote">Package version number for NuGet packages created by OctoPack.</span>
  </td>
</tr>
<tr>
  <th><label for="${keys.octoPackAppendToPackageId}">Append to package ID</label>:</th>
  <td>
    <props:textProperty name="${keys.octoPackAppendToPackageId}" className="longField"/>
    <span class="error" id="error_${keys.octoPackAppendToPackageId}"></span>
    <span class="smallNote">String to append to package ID.</span>
  </td>
</tr>
<tr>
  <th><label for="${keys.octoPackPublishPackageToHttp}">Publish packages to HTTP</label>:</th>
  <td>
    <props:textProperty name="${keys.octoPackPublishPackageToHttp}" className="longField"/>
    <span class="error" id="error_${keys.octoPackPublishPackageToHttp}"></span>
    <span class="smallNote">Publish NuGet packages to the specified URL.</span>
  </td>
</tr>
<tr>
  <th><label for="${keys.octoPackPublishApiKey}">Publish API key</label>:</th>
  <td>
    <props:textProperty name="${keys.octoPackPublishApiKey}" className="longField"/>
    <span class="error" id="error_${keys.octoPackPublishApiKey}"></span>
    <span class="smallNote">Octopus Deploy API key.</span>
  </td>
</tr>
</l:settingsGroup>
