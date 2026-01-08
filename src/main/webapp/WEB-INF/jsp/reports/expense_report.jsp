<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<div class="content-header px-0">
  <div class="container-fluid px-0">
    <div class="row mb-2">
      <div class="col-sm-7">
        <h1 class="m-0 text-dark">
          Additional Expense Overview
        </h1>
      </div>
    </div>
  </div>
</div>

<div class="card">
  <div class="card-header">
    <h3 class="card-title mb-0">
      <i class="fa fa-list mr-1"></i>
      Expense / Consumption / Remaining by Order
    </h3>
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
                <td colspan="6" class="text-center text-muted">
                  No additional expenses found.
                </td>
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
                    </button>
                    &nbsp;
                    <a href="${pageContext.request.contextPath}/orders/${r.orderId}">
                      #<c:out value="${r.salesOrderId}"/>
                    </a>
                  </td>
                  <td><c:out value="${r.customerName}"/></td>
                  <td class="text-right">
                    <fmt:formatNumber value="${r.totalExpenseUsd}" type="number"
                                      minFractionDigits="2" maxFractionDigits="2"/>
                  </td>
                  <td class="text-right">
                    <fmt:formatNumber value="${r.totalConsumedUsd}" type="number"
                                      minFractionDigits="2" maxFractionDigits="2"/>
                  </td>
                  <td class="text-right">
                    <fmt:formatNumber value="${r.remainingUsd}" type="number"
                                      minFractionDigits="2" maxFractionDigits="2"/>
                  </td>
                  <td class="text-right">
                    <a class="btn btn-xs btn-outline-primary"
                       href="${pageContext.request.contextPath}/orders/${r.orderId}?tab=margin">
                      View
                    </a>
                  </td>
                </tr>

                <!-- Hidden details row -->
                <tr class="details-row" id="details-${r.orderId}" style="display:none;">
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
  </div>
</div>

<script>
(function () {
  document.addEventListener('DOMContentLoaded', function () {
    var table = document.querySelector('.table');
    if (!table) return;

    table.addEventListener('click', function (ev) {
      var btn = ev.target.closest('.js-exp-toggle');
      if (!btn) return; // not our button

      var orderId    = btn.getAttribute('data-order-id');
      var detailsRow = document.getElementById('details-' + orderId);
      if (!detailsRow) return;

      var icon = btn.querySelector('i');

      // Use computed style to determine visibility
      var isHidden = window.getComputedStyle(detailsRow).display === 'none';

      if (!isHidden) {
        // Currently visible -> collapse
        detailsRow.style.display = 'none';
        if (icon) {
          icon.classList.remove('fa-chevron-down');
          icon.classList.add('fa-chevron-right');
        }
        return;
      }

      // From here: it's hidden -> expand
      var wrap = detailsRow.querySelector('td > div');
      if (!wrap) return;

      // If already loaded once, just show
      if (detailsRow.dataset.loaded === 'true') {
        detailsRow.style.display = 'table-row';
        if (icon) {
          icon.classList.remove('fa-chevron-right');
          icon.classList.add('fa-chevron-down');
        }
        return;
      }

      // First time load via AJAX
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
</script>
