<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<div class="section-title"><i class="fa fa-info-circle mr-1"></i> Finance Summary</div>
<div class="row">
  <div class="col-md-4 mb-3">
    <div class="card shadow-sm h-100">
      <div class="card-body">
        <div class="text-muted small">Total Value</div>
        <div class="h5 mb-0">
          <c:out value="${financeSummary.totalValueFormatted != null ? financeSummary.totalValueFormatted : '-'}"/>
        </div>
      </div>
    </div>
  </div>
  <div class="col-md-4 mb-3">
    <div class="card shadow-sm h-100">
      <div class="card-body">
        <div class="text-muted small">Collected</div>
        <div class="h5 mb-0">
          <c:out value="${financeSummary.collectedFormatted != null ? financeSummary.collectedFormatted : '-'}"/>
        </div>
      </div>
    </div>
  </div>
  <div class="col-md-4 mb-3">
    <div class="card shadow-sm h-100">
      <div class="card-body">
        <div class="text-muted small">Outstanding</div>
        <div class="h5 mb-0">
          <c:out value="${financeSummary.outstandingFormatted != null ? financeSummary.outstandingFormatted : '-'}"/>
        </div>
      </div>
    </div>
  </div>
</div>

<div class="section-title mt-3"><i class="fa fa-list mr-1"></i> Transactions</div>
<div class="table-responsive">
  <table class="table table-hover table-sm mb-0">
    <thead class="thead-light">
      <tr>
        <th>Date</th>
        <th>Reference</th>
        <th>Type</th>
        <th class="text-right">Amount</th>
      </tr>
    </thead>
    <tbody>
      <c:choose>
        <c:when test="${empty financeSummary || empty financeSummary.transactions}">
          <tr><td colspan="4" class="text-muted text-center py-4">No finance data yet.</td></tr>
        </c:when>
        <c:otherwise>
          <c:forEach var="t" items="${financeSummary.transactions}">
            <tr>
              <td><fmt:formatDate value="${t.date}" pattern="yyyy-MM-dd"/></td>
              <td><c:out value="${t.reference}"/></td>
              <td><c:out value="${t.type}"/></td>
              <td class="text-right"><c:out value="${t.amountFormatted}"/></td>
            </tr>
          </c:forEach>
        </c:otherwise>
      </c:choose>
    </tbody>
  </table>
</div>
