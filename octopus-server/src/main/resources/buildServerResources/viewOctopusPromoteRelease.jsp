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
    Promote from:
    <strong><props:displayValue name="${keys.promoteFromKey}" emptyValue="not specified"/></strong>
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
