<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<table class="table table-sm table-striped">
  <thead><tr><th style="width:170px">When</th><th style="width:140px">Action</th><th style="width:220px">Actor</th><th>Note</th></tr></thead>
  <tbody>
    <c:forEach var="a" items="${audits}">
      <tr>
        <td><fmt:formatDate value="${a.actedOnDate}" pattern="yyyy-MM-dd HH:mm" timeZone="Africa/Kampala"/></td>
        <td><c:out value="${a.action}"/></td>
        <td><c:out value="${a.actor != null ? (a.actor.firstName.concat(' ').concat(a.actor.lastName)) : '-'}"/></td>
        <td><c:out value="${a.note}"/></td>
      </tr>
    </c:forEach>
    <c:if test="${empty audits}">
      <tr><td colspan="4" class="text-muted text-center py-4">No audit entries yet.</td></tr>
    </c:if>
  </tbody>
</table>
