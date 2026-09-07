<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:forEach var="release" items="${octopusReleases}">
  <div class="octopusRelease">
    Octopus Deploy release:
    <a href="<c:out value='${release.url}'/>" target="_blank" rel="noreferrer noopener">
      <c:out value="${empty release.version ? release.url : release.version}"/>
    </a>
  </div>
</c:forEach>
