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

<jsp:useBean id="keys" class="octopus.teamcity.common.buildinfo.BuildInfoKeys"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
  Package IDs:
  <strong><props:displayValue name="${keys.packageIdKey}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
  Package version:
  <strong><props:displayValue name="${keys.packageVersionKey}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
  Overwrite Mode:
  <strong><props:displayValue name="${keys.overwriteModeKey}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
  Verbose logging:
  <strong><props:displayValue name="${keys.verboseLoggingKey}" emptyValue="not specified"/></strong>
</div>
