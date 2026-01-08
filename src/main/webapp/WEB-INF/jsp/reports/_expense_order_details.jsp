<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:choose>
  <c:when test="${empty expenses}">
    <div class="text-muted small">
      No additional expenses captured for this order.
    </div>
  </c:when>
  <c:otherwise>
    <div class="table-responsive">
      <table class="table table-sm table-bordered mb-0">
        <thead class="thead-light">
          <tr>
            <th>Label</th>
            <th class="text-right">Amount</th>
            <th class="text-right">Amount (USD)</th>
            <th>Status</th>
            <th>Comments</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="ex" items="${expenses}">
            <tr>
              <td><c:out value="${ex.label.name}"/></td>
              <td class="text-right">
                <fmt:formatNumber value="${ex.amount}" type="number"
                                  minFractionDigits="2" maxFractionDigits="2"/>
                <span class="text-muted">/ ${ex.currency}</span>
              </td>
              <td class="text-right">
                <fmt:formatNumber value="${ex.amountUsd}" type="number"
                                  minFractionDigits="2" maxFractionDigits="2"/>
              </td>
              <td>
                <span class="badge badge-secondary
                  ">
                  <c:out value="${ex.approvalStatus}"/>
                </span>
              </td>
              <td><c:out value="${ex.comments}"/></td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
  </c:otherwise>
</c:choose>
