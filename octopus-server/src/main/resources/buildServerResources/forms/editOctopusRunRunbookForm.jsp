<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="keys" class="octopus.teamcity.common.OctopusConstants" />
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<l:settingsGroup title="Octopus Connection">
  <jsp:include page="../connectionSelector.jsp"/>
<tr class="octopusInlineConnectionField">
  <th>Octopus URL:<l:star/></th>
  <td>
    <props:textProperty name="${keys.serverKey}" className="longField"/>
    <span class="error" id="error_${keys.serverKey}"></span>
    <span class="smallNote">Specify Octopus web portal URL</span>
  </td>
</tr>
<tr class="octopusInlineConnectionField">
  <th>API key:<l:star/></th>
  <td>
    <props:passwordProperty name="${keys.apiKey}" className="longField"/>
    <span class="error" id="error_${keys.apiKey}"></span>
    <span class="smallNote">Specify Octopus API key. You can get this from your user page in the Octopus web portal.
      You can also reference a build parameter here, e.g. <code>%octopus.apikey%</code>.</span>
  </td>
</tr>
<tr>
    <th>Space name:</th>
    <td>
        <props:textProperty name="${keys.spaceName}" className="longField"/>
        <span class="error" id="error_${keys.spaceName}"></span>
        <span class="smallNote">Specify the Octopus Space name to run within. Leave blank to use the default space.</span>
    </td>
</tr>
</l:settingsGroup>

<l:settingsGroup title="Runbook Run">
    <tr>
      <th>Project:<l:star/></th>
      <td>
        <props:textProperty name="${keys.projectNameKey}" className="longField"/>
        <span class="error" id="error_${keys.projectNameKey}"></span>
        <span class="smallNote">Enter the name of the Octopus project that contains the runbook</span>
      </td>
    </tr>
    <tr>
      <th>Runbook:<l:star/></th>
      <td>
        <props:textProperty name="${keys.runbookNameKey}" className="longField"/>
        <span class="error" id="error_${keys.runbookNameKey}"></span>
        <span class="smallNote">The name of the runbook to run</span>
      </td>
    </tr>
    <tr>
      <th>Environment(s):<l:star/></th>
      <td>
        <props:textProperty name="${keys.deployToKey}" className="longField"/>
        <span class="error" id="error_${keys.deployToKey}"></span>
        <span class="smallNote">Comma separated list of environments to run the runbook in.</span>
      </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="${keys.tenantsKey}">Tenant(s):</label></th>
        <td>
            <props:textProperty name="${keys.tenantsKey}" className="longField"/>
            <span class="error" id="error_${keys.tenantsKey}"></span>
            <span class="smallNote">Comma separated list of tenants to run for.
            <br />Note that when supplying tenant filters then only one environment may be provided above.</span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="${keys.tenantTagsKey}">Tenant tag(s):</label></th>
        <td>
            <props:textProperty name="${keys.tenantTagsKey}" className="longField"/>
            <span class="error" id="error_${keys.tenantTagsKey}"></span>
            <span class="smallNote">Comma separated list of <a href='http://g.octopushq.com/TenantTags' target='_blank'>tenant tags</a> that match tenants to run for.
            <br />Note that when supplying tag filters then only one environment may be provided above.</span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="${keys.runbookSnapshotKey}">Snapshot:</label></th>
        <td>
            <props:textProperty name="${keys.runbookSnapshotKey}" className="longField"/>
            <span class="error" id="error_${keys.runbookSnapshotKey}"></span>
            <span class="smallNote">The name of the runbook snapshot to run. Leave blank to use the published snapshot.</span>
        </td>
    </tr>
    <tr>
        <th>Show runbook run progress:</th>
        <td>
            <props:checkboxProperty name="${keys.waitForDeployments}" />
            <span class="error" id="error_${keys.waitForDeployments}"></span>
            <span class="smallNote">If checked, the build process will only succeed if the runbook run is successful.</span>
        </td>
    </tr>
    <tr>
        <th>Time to wait for runbook run:</th>
        <td>
            <props:textProperty name="${keys.deploymentTimeout}" />
            <span class="error" id="error_${keys.deploymentTimeout}"></span>
            <span class="smallNote">The amount of time, specified in timespan format, to wait for the runbook run to complete. Default is 00:10:00 if left blank. The runbook run itself does not timeout, this timeout is purely how long the client will keep polling to see if it has completed.</span>
        </td>
    </tr>
    <tr>
        <th>Cancel runbook run on timeout:</th>
        <td>
            <props:checkboxProperty name="${keys.cancelDeploymentOnTimeout}" />
            <span class="error" id="error_${keys.cancelDeploymentOnTimeout}"></span>
            <span class="smallNote">If checked, and <strong>Show runbook run progress</strong> is also checked, then the runbook run will be explicitly canceled if the time to wait has expired and the task has not completed.</span>
        </td>
    </tr>
</l:settingsGroup>


<l:settingsGroup title="Advanced">
<tr>
  <th>Additional command line arguments:</th>
  <td>
    <props:textProperty name="${keys.commandLineArgumentsKey}" className="longField"/>
    <span class="error" id="error_${keys.commandLineArgumentsKey}"></span>
    <span class="smallNote">Additional arguments to be passed to <a href="https://g.octopushq.com/OctoExeRunRunbook">Octopus CLI</a></span>
  </td>
</tr>
</l:settingsGroup>
