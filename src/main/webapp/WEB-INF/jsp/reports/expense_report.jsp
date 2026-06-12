<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/css/select2.min.css" />
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@ttskch/select2-bootstrap4-theme@x.x.x/dist/select2-bootstrap4.min.css" />
<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/js/select2.min.js"></script>
<div class="content-header px-0">
	<div class="container-fluid px-0">
		<div class="row mb-2">
			<div class="col-sm-7">
				<h1 class="m-0 text-dark">Additional Expense Overview</h1>
			</div>
		</div>
	</div>
</div>


<div class="card-header">
	<form method="get"
		action="${pageContext.request.contextPath}/reports/expenses"
		class="form-inline mb-0 w-100">
		<input type="hidden" name="page" value="0" />
		<div
			class="d-flex align-items-center justify-content-between w-100 flex-wrap">
			<h3 class="card-title mb-2 mb-md-0 mr-3">
				<i class="fa fa-list mr-1"></i> Expense / Consumption / Remaining by
				Order
			</h3>

			<div class="d-flex align-items-center flex-wrap" style="gap: 12px;">
  
  <div class="form-group mb-0">
    <select name="searchOrder" id="orderSelect" class="form-control form-control-sm select2" style="width: 180px;">
      <option value="">-- All Orders --</option>
      <c:forEach var="orderId" items="${orderOptions}">
        <option value="${orderId}" ${param.searchOrder == orderId ? 'selected' : ''}>
          #${orderId}
        </option>
      </c:forEach>
    </select>
  </div>

  <div class="form-group mb-0">
    <select name="searchCustomer" id="customerSelect" class="form-control form-control-sm select2" style="width: 200px;">
      <option value="">-- All Customers --</option>
      <c:forEach var="custName" items="${customerOptions}">
        <option value="${custName}" ${param.searchCustomer == custName ? 'selected' : ''}>
          ${custName}
        </option>
      </c:forEach>
    </select>
  </div>

  <button type="submit" class="btn btn-sm btn-primary">
    Filter
  </button>
  
  <c:if test="${not empty param.searchOrder || not empty param.searchCustomer}">
    <a href="${pageContext.request.contextPath}/reports/expenses" class="btn btn-sm btn-outline-secondary">
      Clear
    </a>
  </c:if>

  <span class="text-muted mx-1">|</span>

  <c:if test="${not empty page}">
    <label class="small text-muted mr-2 mb-0">Rows:</label>
    <select name="size" class="form-control form-control-sm" onchange="this.form.submit()">
      <option value="15"  ${page.size == 15  ? 'selected' : ''}>15</option>
      <option value="25"  ${page.size == 25  ? 'selected' : ''}>25</option>
      <option value="50"  ${page.size == 50  ? 'selected' : ''}>50</option>
    </select>
  </c:if>

</div>
		</div>
	</form>
</div>

<div class="card-body p-2">
	<div class="table-responsive">
		<table class="table table-sm table-bordered table-hover mb-0">
			<thead class="thead-light">
				<tr>
					<th style="width: 30%;">Order</th>
					<th>Customer</th>
					<th class="text-right">Total Expense (USD)</th>
					<th class="text-right">Total Consumed (USD)</th>
					<th class="text-right">Remaining (USD)</th>
					<th style="width: 1%;">Action</th>
				</tr>
			</thead>
			<tbody>
				<c:choose>
					<c:when test="${empty rows}">
						<tr>
							<td colspan="6" class="text-center text-muted">No additional
								expenses found.</td>
						</tr>
					</c:when>
					<c:otherwise>
						<c:forEach var="r" items="${rows}">
							<!-- Summary row per order -->
							<tr class="summary-row" data-order-id="${r.orderId}">
								<td>
									<button type="button"
										class="btn btn-xs btn-outline-secondary js-exp-toggle"
										data-order-id="${r.orderId}">
										<i class="fa fa-chevron-right"></i>
									</button> &nbsp; <a
									href="${pageContext.request.contextPath}/orders/${r.orderId}">
										#<c:out value="${r.salesOrderId}" />
								</a>
								</td>
								<td><c:out value="${r.customerName}" /></td>
								<td class="text-right"><fmt:formatNumber
										value="${r.totalExpenseUsd}" type="number"
										minFractionDigits="2" maxFractionDigits="2" /></td>
								<td class="text-right"><fmt:formatNumber
										value="${r.totalConsumedUsd}" type="number"
										minFractionDigits="2" maxFractionDigits="2" /></td>
								<td class="text-right"><fmt:formatNumber
										value="${r.remainingUsd}" type="number" minFractionDigits="2"
										maxFractionDigits="2" /></td>
								<td class="text-right"><a
									class="btn btn-xs btn-outline-primary"
									href="${pageContext.request.contextPath}/orders/${r.orderId}?tab=margin">
										View </a></td>
							</tr>

							<!-- Hidden details row -->
							<tr class="details-row" id="details-${r.orderId}"
								style="display: none;">
								<td colspan="6">
									<div class="text-muted small">Loading...</div>
								</td>
							</tr>
						</c:forEach>
					</c:otherwise>
				</c:choose>
			</tbody>
		</table>
	</div>

	<!-- Footer: "showing X-Y of Z" + pagination -->
	<c:if test="${not empty page && page.totalPages > 0}">
		<div class="d-flex align-items-center justify-content-between mt-2">
			<div class="small text-muted">
				<c:set var="start" value="${page.number * page.size + 1}" />
				<c:set var="end"
					value="${page.number * page.size + fn:length(rows)}" />
				Showing ${start} - ${end} of ${page.totalElements}
			</div>

			<c:if test="${page.totalPages > 1}">
				<nav aria-label="Expense pagination">
					<ul class="pagination pagination-sm mb-0">

						<li class="page-item ${page.first ? 'disabled' : ''}"><a
							class="page-link"
							href="${pageContext.request.contextPath}/reports/expenses?page=${page.number-1}&size=${page.size}&searchOrder=${param.searchOrder}&searchCustomer=${param.searchCustomer}">
								Prev </a></li>

						<c:set var="window" value="3" />
						<c:set var="from" value="${page.number - window}" />
						<c:set var="to" value="${page.number + window}" />
						<c:if test="${from < 0}">
							<c:set var="from" value="0" />
						</c:if>
						<c:if test="${to > page.totalPages - 1}">
							<c:set var="to" value="${page.totalPages - 1}" />
						</c:if>

						<c:forEach begin="${from}" end="${to}" var="i">
							<li class="page-item ${i == page.number ? 'active' : ''}"><a
								class="page-link"
								href="${pageContext.request.contextPath}/reports/expenses?page=${i}&size=${page.size}&searchOrder=${param.searchOrder}&searchCustomer=${param.searchCustomer}">
									${i + 1} </a></li>
						</c:forEach>

						<li class="page-item ${page.last ? 'disabled' : ''}"><a
							class="page-link"
							href="${pageContext.request.contextPath}/reports/expenses?page=${page.number+1}&size=${page.size}&searchOrder=${param.searchOrder}&searchCustomer=${param.searchCustomer}">
								Next </a></li>

					</ul>
				</nav>
			</c:if>
		</div>
	</c:if>

</div>

<script>
(function () {
  document.addEventListener('DOMContentLoaded', function () {
    var table = document.querySelector('.table');
    if (!table) return;

    table.addEventListener('click', function (ev) {
      var btn = ev.target.closest('.js-exp-toggle');
      if (!btn) return;

      var orderId    = btn.getAttribute('data-order-id');
      var detailsRow = document.getElementById('details-' + orderId);
      if (!detailsRow) return;

      var icon = btn.querySelector('i');
      var isHidden = window.getComputedStyle(detailsRow).display === 'none';

      if (!isHidden) {
        detailsRow.style.display = 'none';
        if (icon) {
          icon.classList.remove('fa-chevron-down');
          icon.classList.add('fa-chevron-right');
        }
        return;
      }

      var wrap = detailsRow.querySelector('td > div');
      if (!wrap) return;

      if (detailsRow.dataset.loaded === 'true') {
        detailsRow.style.display = 'table-row';
        if (icon) {
          icon.classList.remove('fa-chevron-right');
          icon.classList.add('fa-chevron-down');
        }
        return;
      }

      wrap.innerHTML = '<span class="text-muted small">Loading...</span>';

      fetch('${pageContext.request.contextPath}/reports/expenses/' + orderId + '/details', {
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
      })
      .then(function (r) {
        if (!r.ok) throw new Error('Failed to load details');
        return r.text();
      })
      .then(function (html) {
        wrap.innerHTML = html;
        detailsRow.dataset.loaded = 'true';
        detailsRow.style.display = 'table-row';
        if (icon) {
          icon.classList.remove('fa-chevron-right');
          icon.classList.add('fa-chevron-down');
        }
      })
      .catch(function () {
        wrap.innerHTML = '<span class="text-danger small">Failed to load details.</span>';
      });
    });
  });
})();
(function () {
  // Polling helper function to wait until jQuery and Select2 are available
  function initializeSelect2WhenReady() {
    if (typeof window.jQuery !== 'undefined' && window.jQuery.fn && window.jQuery.fn.select2) {
      var $ = window.jQuery;
      
      // Initialize Order Dropdown
      $('#orderSelect').select2({
        theme: 'bootstrap4',
        placeholder: "-- All Orders --",
        allowClear: true,
        minimumResultsForSearch: 0 // Forces search input bar to show up
      });
      
      // Initialize Customer Dropdown
      $('#customerSelect').select2({
        theme: 'bootstrap4',
        placeholder: "-- All Customers --",
        allowClear: true,
        minimumResultsForSearch: 0 // Forces search input bar to show up
      });
      
      console.log("Select2 successfully initialized on report dropdowns.");
    } else {
      // If not ready yet, check again in 50 milliseconds
      setTimeout(initializeSelect2WhenReady, 50);
    }
  }

  // Kick off the check
  initializeSelect2WhenReady();
})();
</script>
