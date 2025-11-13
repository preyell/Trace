<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<table class="table table-bordered mb-0">
  <thead class="thead-light">
    <tr>
      <th>Amount</th>
      <th>Amount (USD)</th>
      <th>Disbursed On</th>
      <th>Disbursed By</th>
      <th>Comments</th>
      <th style="width:1%;">Actions</th>
    </tr>
  </thead>
  <tbody>
    <c:choose>
      <c:when test="${empty disbursements}">
        <tr><td colspan="6" class="text-muted text-center">No disbursements.</td></tr>
      </c:when>
      <c:otherwise>
        <c:forEach var="d" items="${disbursements}">
          <tr>
            <td>
              <fmt:formatNumber value="${d.amount}" type="number" minFractionDigits="2" maxFractionDigits="2"/>
              <span class="text-muted">/ ${d.currency}</span>
            </td>
            <td><fmt:formatNumber value="${d.amountUsd}" type="number" minFractionDigits="2" maxFractionDigits="2"/></td>
            <td><fmt:formatDate value="${Date.from(d.disbursedOn)}" pattern="yyyy-MM-dd HH:mm" timeZone="Africa/Kampala"/></td>
            <td>
              <c:out value="${d.actor != null ? (d.actor.firstName.concat(' ').concat(d.actor.lastName)) : '-'}"/>
            </td>
            <td><c:out value="${d.note}"/></td>
            <td class="text-right">
              <form method="post" action="${pageContext.request.contextPath}/orders/${order.id}/expenses/${param.expId}/disbursements/${d.id}/delete" onsubmit="return confirm('Delete this disbursement?');">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <button class="btn btn-sm btn-outline-danger"><i class="fa fa-trash"></i></button>
              </form>
            </td>
          </tr>
        </c:forEach>
      </c:otherwise>
    </c:choose>
  </tbody>
</table>
