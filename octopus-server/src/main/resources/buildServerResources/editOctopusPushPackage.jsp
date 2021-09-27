<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--
  ~ Copyright 2000-2012 Octopus Deploy Pty. Ltd.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<jsp:useBean id="keys" class="octopus.teamcity.common.OctopusConstants" />
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<l:settingsGroup title="Octopus Connection">
  <tr>
    <th>Octopus URL:<l:star/></th>
    <td>
      <props:textProperty name="${keys.serverKey}" className="longField"/>
      <span class="error" id="error_${keys.serverKey}"></span>
      <span class="smallNote">Specify Octopus web portal URL</span>
    </td>
  </tr>
  <tr>
    <th>API key:<l:star/></th>
    <td>
      <props:passwordProperty name="${keys.apiKey}" className="longField"/>
      <span class="error" id="error_${keys.apiKey}"></span>
      <span class="smallNote">Specify Octopus API key. You can get this from your user page in the Octopus web portal.</span>
    </td>
  </tr>
  <tr>
    <th>Space name:</th>
    <td>
      <props:textProperty name="${keys.spaceName}" className="longField"/>
      <span class="error" id="error_${keys.spaceName}"></span>
      <span class="smallNote">Specify the Octopus Space name to push to. Leave blank to use the default space.</span>
    </td>
  </tr>
</l:settingsGroup>

<l:settingsGroup title="Package Push">
  <tr>
    <th>Package paths:<l:star/></th>
    <td>
      <props:multilineProperty name="${keys.packagePathsKey}" rows="5" cols="50" linkTitle="Package path patterns" expanded="true" />
      <span class="error" id="error_${keys.packagePathsKey}"></span>
    <span class="smallNote">
        Newline-separated paths of either package files, or directories to create packages from, that will be pushed. These follow the same rules as TeamCity artifact paths. Ant-style wildcards like <kbd>dir/**/*.zip</kbd> and directory transforms that produce a package
        like <kbd>published-webapp/**/* =&gt; MyApp.%build.number%.zip</kbd> are supported.
      </span>
    </td>
  </tr>
  <tr class="advancedSetting">
    <th>Overwrite Mode:</th>
    <td>
      <props:selectProperty name="${keys.forcePushKey}">
        <props:option value="false">Fail If Exists</props:option>
        <props:option value="true">Overwrite Existing</props:option>
        <props:option value="IgnoreIfExists">Ignore If Exists</props:option>
      </props:selectProperty>
      <span class="error" id="error_${keys.forcePushKey}"></span>
      <span class="smallNote">Normally, if the same package already exists on the server, the server will reject the package push. This is a good practice as it ensures a package isn't accidentally overwritten or ignored. Use this setting to override this behavior.</span>
    </td>
  </tr>

  <tr class="advancedSetting">
    <th>Publish packages as build artifacts:</th>
    <td>
      <props:checkboxProperty name="${keys.publishArtifactsKey}" />
      <span class="error" id="error_${keys.publishArtifactsKey}"></span>
      <span class="smallNote">Set this option to automatically publish any packages as TeamCity build artifacts. This is useful if you are creating a package from a directory, and want the package to appear in TeamCity as a build artifact.</span>
    </td>
  </tr>
</l:settingsGroup>

<l:settingsGroup title="Advanced">
  <tr>
    <th>Additional command line arguments:</th>
    <td>
      <props:textProperty name="${keys.commandLineArgumentsKey}" className="longField"/>
      <span class="error" id="error_${keys.commandLineArgumentsKey}"></span>
      <span class="smallNote">Additional arguments to be passed to <a href="https://g.octopushq.com/OctoExePush">Octopus CLI</a></span>
    </td>
  </tr>
</l:settingsGroup>
