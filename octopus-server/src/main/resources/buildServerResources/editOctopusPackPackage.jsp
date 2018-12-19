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

<l:settingsGroup title="Pack">

  <tr>
    <th>Package ID:<l:star/></th>
    <td>
      <props:textProperty name="${keys.packageIdKey}" className="longField" />
      <span class="error" id="error_${keys.packageIdKey}"></span>
      <span class="smallNote">
        The package's identifier.
      </span>
    </td>
  </tr>
  <tr>
    <th>Package format:<l:star/></th>
    <td>
      <props:selectProperty name="${keys.packageFormatKey}" multiple="false">
        <props:option value="NuPkg" selected="${keys.packageFormatKey == 'NuPkg'}">NuPkg</props:option>
        <props:option value="Zip" selected="${keys.packageFormatKey == 'Zip'}">Zip</props:option>
      </props:selectProperty>
      <span class="error" id="error_${keys.packageFormatKey}"></span>
      <span class="smallNote">
        The package's format, e.g. "NuGet" or "zip". Defaults to NuGet if left blank.
      </span>
    </td>
  </tr>
  <tr>
    <th>Package version:<l:star/></th>
    <td>
      <props:textProperty name="${keys.packageVersionKey}" className="longField" />
      <span class="error" id="error_${keys.packageVersionKey}"></span>
      <span class="smallNote">
        The package's version, e.g. 1.0.0, 1.0.1-alpha, 1.0.2+githash. %build.number%
      </span>
    </td>
  </tr>
    
  <tr>
    <th>Source path:<l:star/></th>
    <td>
      <props:textProperty name="${keys.packageSourcePathKey}" className="longField" />
      <span class="error" id="error_${keys.packageSourcePathKey}"></span>
      <span class="smallNote">
        Directory to create package from.
      </span>
    </td>
  </tr>
    
  <tr>
    <th>Output path:<l:star/></th>
    <td>
      <props:textProperty name="${keys.packageOutputPathKey}" className="longField" />
      <span class="error" id="error_${keys.packageOutputPathKey}"></span>
      <span class="smallNote">
        Directory to write the package to.
      </span>
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

  <tr class="advancedSetting">
    <th>Additional command line arguments:</th>
    <td>
      <props:textProperty name="${keys.commandLineArgumentsKey}" className="longField"/>
      <span class="error" id="error_${keys.commandLineArgumentsKey}"></span>
      <span class="smallNote">Additional arguments to be passed to <a href="https://github.com/OctopusDeploy/Octo.exe">Octo.exe</a></span>
    </td>
  </tr>
</l:settingsGroup>
