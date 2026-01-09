<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<div class="d-flex align-items-center mb-3">
	<div class="section-title mb-0">
		<i class="fa fa-file-invoice-dollar mr-1"></i> Invoices
	</div>
	<button type="button" class="btn btn-primary btn-sm ml-auto"
		data-toggle="modal" data-target="#invAddModal">
		<i class="fa fa-plus mr-1"></i> Add Invoice
	</button>
</div>

<div class="card card-outline card-secondary">
	<div class="card-body p-2">
		<div class="table-responsive">
			<table class="table table-sm table-bordered table-hover mb-0">
				<thead class="thead-light">
					<tr>
						<th>Invoice #</th>
						<th class="text-right">Amount</th>
						<th>Currency</th>
						<th>Invoice Delivery Date</th>
						<th>Expected Payment Date</th>
						<th>Payment Received Date</th>
						<th class="text-right">Invoice %</th>
						<th class="text-right">Conv. Rate</th>
						<th>Final?</th>
						<th>File</th>
						<th style="width: 1%;">Actions</th>
					</tr>
				</thead>

				<tbody>
					<c:choose>
						<c:when test="${empty invoices}">
							<tr>
								<td colspan="8" class="text-center text-muted">No invoices
									captured for this order.</td>
							</tr>
						</c:when>
						<c:otherwise>
							<c:forEach var="inv" items="${invoices}">
								<tr>
									<td><c:out value="${inv.invoiceNumber}" /></td>
									<td class="text-right"><fmt:formatNumber
											value="${inv.amount}" type="number" minFractionDigits="2"
											maxFractionDigits="2" /></td>
									<td><c:out value="${inv.currency}" /></td>
									<td><c:out value="${inv.invoiceDeliveryDate}" /></td>
									<td><c:out value="${inv.expectedPaymentDate}" /></td>
									<td><c:out value="${inv.paymentReceivedDate}" /></td>
									<!-- NEW -->
									<td class="text-right"><fmt:formatNumber
											value="${inv.invoicePercent}" type="number"
											minFractionDigits="2" maxFractionDigits="2" />%</td>
									<td class="text-right"><c:if
											test="${not empty inv.conversionRate}">
											<fmt:formatNumber value="${inv.conversionRate}" type="number"
												minFractionDigits="2" maxFractionDigits="6" />
										</c:if></td>
									<td><c:choose>
											<c:when test="${inv.finalInvoice}">
												<span class="badge badge-primary">Yes</span>
											</c:when>
											<c:otherwise>
												<span class="badge badge-secondary">No</span>
											</c:otherwise>
										</c:choose></td>
									<td><c:if test="${not empty inv.fileName}">
											<a
												href="${pageContext.request.contextPath}/files/invoices/${inv.id}"
												target="_blank"> <i class="fa fa-paperclip mr-1"></i> <c:out
													value="${inv.fileName}" />
											</a>
										</c:if></td>
									<td class="text-right">
										<!-- Edit -->
										<button type="button"
											class="btn btn-xs btn-outline-secondary js-inv-edit"
											data-toggle="modal" data-target="#invEditModal"
											data-invid="${inv.id}" data-number="${inv.invoiceNumber}"
											data-amount="${inv.amount}" data-currency="${inv.currency}"
											data-delivery="${inv.invoiceDeliveryDate}"
											data-expected="${inv.expectedPaymentDate}"
											data-received="${inv.paymentReceivedDate}"
											data-percent="${inv.invoicePercent}"
											data-rate="${inv.conversionRate}"
											data-final="${inv.finalInvoice}">
											<i class="fa fa-edit"></i>
										</button>
										<button type="button"
											class="btn btn-xs btn-outline-danger js-inv-del"
											data-toggle="modal" data-target="#invDeleteModal"
											data-invid="${inv.id}">
											<i class="fa fa-trash"></i>
										</button>
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

<!-- Add Invoice Modal -->
<div class="modal fade" id="invAddModal" tabindex="-1" role="dialog"
	aria-hidden="true">
	<div class="modal-dialog modal-lg modal-dialog-scrollable">
		<div class="modal-content">
			<form method="post"
				action="${pageContext.request.contextPath}/orders/${order.id}/invoices"
				enctype="multipart/form-data">
				<div class="modal-header">
					<h5 class="modal-title">Add Invoice</h5>
					<button type="button" class="close" data-dismiss="modal">
						<span>&times;</span>
					</button>
				</div>

				<div class="modal-body">
					<input type="hidden" name="${_csrf.parameterName}"
						value="${_csrf.token}" />

					<div class="form-row">
						<div class="form-group col-md-4">
							<label>Invoice Number</label> <input type="text"
								name="invoiceNumber" class="form-control" required>
						</div>

						<div class="form-group col-md-4">
							<label>Invoice Amount</label> <input type="number" name="amount"
								step="0.01" min="0.01" class="form-control" required>
						</div>

						<div class="form-group col-md-4">
							<label>Currency</label> <select name="currency"
								class="form-control" required>
								<c:forEach var="cur" items="${currencies}">
									<option value="${cur}">${cur}</option>
								</c:forEach>
							</select>
						</div>
					</div>

					<div class="form-row">
						<div class="form-group col-md-4">
							<label>Invoice Delivery Date</label> <input type="date"
								name="invoiceDeliveryDate" class="form-control"  min="2000-01-01" max="2100-12-31" required>
						</div>
						<div class="form-group col-md-4">
							<label>Expected Payment Date</label> <input type="date"
								name="expectedPaymentDate" class="form-control"  min="2000-01-01" max="2100-12-31" required>
						</div>
						<div class="form-group col-md-4">
							<label>Invoice %</label> <input type="number"
								name="invoicePercent" step="0.01" min="0" max="100"
								class="form-control" required>
						</div>
					</div>

					<div class="form-group">
						<label>Attachment (optional)</label> <input type="file"
							name="file" class="form-control-file">
					</div>
				</div>

				<div class="modal-footer">
					<button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>
					<button type="submit" class="btn btn-primary">Save Invoice</button>
				</div>
			</form>
		</div>
	</div>
</div>

<!-- Edit Invoice Modal -->
<div class="modal fade" id="invEditModal" tabindex="-1" role="dialog"
	aria-hidden="true">
	<div class="modal-dialog modal-lg modal-dialog-scrollable">
		<div class="modal-content">
			<form method="post" id="invEditForm" enctype="multipart/form-data">
				<div class="modal-header">
					<h5 class="modal-title">Edit Invoice</h5>
					<button type="button" class="close" data-dismiss="modal">
						<span>&times;</span>
					</button>
				</div>

				<div class="modal-body">
					<input type="hidden" name="${_csrf.parameterName}"
						value="${_csrf.token}" />

					<div class="form-row">
						<div class="form-group col-md-4">
							<label>Invoice Number</label> <input type="text"
								name="invoiceNumber" id="invEditNumber" class="form-control"
								required>
						</div>

						<div class="form-group col-md-4">
							<label>Invoice Amount</label> <input type="number" name="amount"
								step="0.01" min="0.01" id="invEditAmount" class="form-control"
								required>
						</div>

						<div class="form-group col-md-4">
							<label>Currency</label> <select name="currency"
								id="invEditCurrency" class="form-control" required>
								<c:forEach var="cur" items="${currencies}">
									<option value="${cur}">${cur}</option>
								</c:forEach>
							</select>
						</div>
					</div>

					<div class="form-row">
						<div class="form-group col-md-4">
							<label>Invoice Delivery Date</label> <input type="date"
								name="invoiceDeliveryDate" id="invEditDelivery"
								class="form-control"  min="2000-01-01" max="2100-12-31" required>
						</div>
						<div class="form-group col-md-4">
							<label>Expected Payment Date</label> <input type="date"
								name="expectedPaymentDate" id="invEditExpected"
								class="form-control"  min="2000-01-01" max="2100-12-31" required>
						</div>
						<div class="form-group col-md-4">
							<label>Payment Received Date</label> <input type="date"
								name="paymentReceivedDate" id="invEditReceived"
								class="form-control"  min="2000-01-01" max="2100-12-31">
						</div>
					</div>

					<div class="form-row">
						<div class="form-group col-md-4">
							<label>Invoice %</label> <input type="number"
								name="invoicePercent" step="0.01" min="0" max="100"
								id="invEditPercent" class="form-control" required>
						</div>
						<div class="form-group col-md-4">
							<label>Conversion Rate</label> <input type="number"
								name="conversionRate" step="0.000001" min="0" id="invEditRate"
								class="form-control">
						</div>
						<div class="form-group col-md-4 d-flex align-items-center">
							<div class="form-check mt-4">
								<input class="form-check-input" type="checkbox"
									id="invEditFinal" name="finalInvoice" value="true"> <label
									class="form-check-label" for="invEditFinal"> Final
									Invoice? </label>
							</div>
						</div>
					</div>


					<div class="form-group">
						<label>Replace Attachment (optional)</label> <input type="file"
							name="file" class="form-control-file"> <small
							class="form-text text-muted"> Leave empty to keep
							existing file. </small>
					</div>
				</div>

				<div class="modal-footer">
					<button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>
					<button type="submit" class="btn btn-primary">Save Changes</button>
				</div>
			</form>
		</div>
	</div>
</div>

<!-- Delete Invoice Modal -->
<div class="modal fade" id="invDeleteModal" tabindex="-1" role="dialog"
	aria-hidden="true">
	<div class="modal-dialog">
		<div class="modal-content">
			<form method="post" id="invDeleteForm">
				<div class="modal-header">
					<h5 class="modal-title text-danger">Delete Invoice</h5>
					<button type="button" class="close" data-dismiss="modal">
						<span>&times;</span>
					</button>
				</div>

				<div class="modal-body">
					<input type="hidden" name="${_csrf.parameterName}"
						value="${_csrf.token}" />
					<p>Are you sure you want to delete this invoice?</p>

					<div class="custom-control custom-checkbox">
						<input type="checkbox" class="custom-control-input"
							id="invDeleteFile" name="deleteFile" value="true" checked>
						<label class="custom-control-label" for="invDeleteFile">
							Also delete the uploaded file </label>
					</div>
				</div>

				<div class="modal-footer">
					<button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>
					<button type="submit" class="btn btn-danger">Yes, delete</button>
				</div>
			</form>
		</div>
	</div>
</div>

<script>
(function () {
  document.addEventListener('DOMContentLoaded', function () {

    // ---------- helpers ----------
    function isValidISODate(s) {
      if (!s) return true; // optional: blank allowed
      if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return false;

      const parts = s.split('-').map(Number);
      const y = parts[0], m = parts[1], d = parts[2];

      if (y < 2000 || y > 2100) return false; // blocks 0776
      if (m < 1 || m > 12) return false;
      if (d < 1 || d > 31) return false;

      const dt = new Date(Date.UTC(y, m - 1, d));
      return dt.getUTCFullYear() === y &&
             (dt.getUTCMonth() + 1) === m &&
             dt.getUTCDate() === d;
    }

    function compareDates(a, b) {
      // for ISO yyyy-mm-dd, string compare works
      return a >= b;
    }

    function markInvalid(el, msg) {
      if (!el) return;
      el.classList.add('is-invalid');
      let fb = el.parentNode.querySelector('.invalid-feedback');
      if (!fb) {
        fb = document.createElement('div');
        fb.className = 'invalid-feedback d-block';
        el.parentNode.appendChild(fb);
      }
      fb.textContent = msg;
    }

    function clearInvalid(el) {
      if (!el) return;
      el.classList.remove('is-invalid');
      const fb = el.parentNode.querySelector('.invalid-feedback');
      if (fb) fb.textContent = '';
    }

    // ---------- Wire Edit button ----------
    document.body.addEventListener('click', function (ev) {
      var btn = ev.target.closest('.js-inv-edit');
      if (!btn) return;

      var id       = btn.getAttribute('data-invid');
      var number   = btn.getAttribute('data-number') || '';
      var amount   = btn.getAttribute('data-amount') || '';
      var currency = btn.getAttribute('data-currency') || '';
      var delivery = btn.getAttribute('data-delivery') || '';
      var expected = btn.getAttribute('data-expected') || '';
      var received = btn.getAttribute('data-received') || '';
      var percent  = btn.getAttribute('data-percent') || '';
      var rate     = btn.getAttribute('data-rate') || '';
      var isFinal  = btn.getAttribute('data-final') === 'true';

      var form = document.getElementById('invEditForm');
      if (!form) return;

      // Set form action
      form.setAttribute(
        'action',
        '${pageContext.request.contextPath}/orders/${order.id}/invoices/' + id + '/update'
      );

      // Fill fields
      document.getElementById('invEditNumber').value   = number;
      document.getElementById('invEditAmount').value   = amount;
      document.getElementById('invEditCurrency').value = currency;
      document.getElementById('invEditDelivery').value = delivery;
      document.getElementById('invEditExpected').value = expected;
      document.getElementById('invEditReceived').value = received;
      document.getElementById('invEditPercent').value  = percent;
      document.getElementById('invEditRate').value     = rate;
      document.getElementById('invEditFinal').checked  = isFinal;

      // Clear any previous validation errors when opening modal
      clearInvalid(document.getElementById('invEditDelivery'));
      clearInvalid(document.getElementById('invEditExpected'));
      clearInvalid(document.getElementById('invEditReceived'));
    });

    // ---------- Wire Delete button ----------
    document.body.addEventListener('click', function (ev) {
      var btn = ev.target.closest('.js-inv-del');
      if (!btn) return;

      var id = btn.getAttribute('data-invid');
      var form = document.getElementById('invDeleteForm');
      if (!form) return;

      form.setAttribute(
        'action',
        '${pageContext.request.contextPath}/orders/${order.id}/invoices/' + id + '/delete'
      );
    });

    // ---------- Invoice Edit Modal submit validation (IMPORTANT) ----------
    var invEditForm = document.getElementById('invEditForm');
    if (invEditForm) {
      invEditForm.addEventListener('submit', function (e) {

        const invEl = document.getElementById('invEditDelivery');
        const expEl = document.getElementById('invEditExpected');
        const recEl = document.getElementById('invEditReceived');

        // clear old errors
        [invEl, expEl, recEl].forEach(clearInvalid);

        let ok = true;

        // format/range validation
        if (invEl && !isValidISODate(invEl.value)) {
          ok = false;
          markInvalid(invEl, 'Invoice delivery date must be a valid date (YYYY-MM-DD) between 2000 and 2100.');
        }
        if (expEl && !isValidISODate(expEl.value)) {
          ok = false;
          markInvalid(expEl, 'Expected payment date must be a valid date (YYYY-MM-DD) between 2000 and 2100.');
        }
        if (recEl && !isValidISODate(recEl.value)) {
          ok = false;
          markInvalid(recEl, 'Payment received date must be a valid date (YYYY-MM-DD) between 2000 and 2100.');
        }

        // business rule: received >= invoice delivery AND >= expected
        const inv = invEl ? invEl.value : '';
        const exp = expEl ? expEl.value : '';
        const rec = recEl ? recEl.value : '';

        if (rec) {
          if (inv && !compareDates(rec, inv)) {
            ok = false;
            markInvalid(recEl, 'Payment received date cannot be earlier than invoice delivery date.');
          }
          if (exp && !compareDates(rec, exp)) {
            ok = false;
            markInvalid(recEl, 'Payment received date cannot be earlier than expected payment date.');
          }
        }

        if (!ok) {
          e.preventDefault();
          e.stopPropagation();
        }
      });

      // optional: clear invalid state as user types
      ['invEditDelivery', 'invEditExpected', 'invEditReceived'].forEach(function (id) {
        var el = document.getElementById(id);
        if (!el) return;
        el.addEventListener('input', function () { clearInvalid(el); });
        el.addEventListener('change', function () { clearInvalid(el); });
      });
    }
  });
})();
</script>
