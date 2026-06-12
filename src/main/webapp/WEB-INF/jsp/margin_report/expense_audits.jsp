<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>


<h6 class="mb-3">
    Vertical: <c:out value="${ex.vertical.name}" />
</h6>
<table class="table table-sm table-striped">
  <thead><tr><th>When</th><th>Action</th><th>Actor</th><th>Comments</th><th>Note</th></tr></thead>
  <tbody>
    <c:forEach var="a" items="${audits}">
      <tr>
        <td><fmt:formatDate value="${a.actedOnDate}" pattern="yyyy-MM-dd HH:mm" timeZone="Africa/Nairobi"/></td>
        <td><c:out value="${a.action}"/></td>
        <td><c:out value="${a.actor != null ? (a.actor.firstName.concat(' ').concat(a.actor.lastName)) : '-'}"/></td>
        <td><c:out value="${a.comments}"/></td>
        <td><c:out value="${a.note}"/></td>
      </tr>
    </c:forEach>
    <c:if test="${empty audits}">
      <tr><td colspan="4" class="text-muted text-center py-4">No audit entries yet.</td></tr>
    </c:if>
  </tbody>
</table>
