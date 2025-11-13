<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<!-- Optional: plugin; safe to keep even if globally included -->
<script
	src="https://cdn.jsdelivr.net/npm/bs-custom-file-input/dist/bs-custom-file-input.min.js"></script>

<div class="d-flex align-items-center mb-3">
	<div class="section-title mb-0">
		<i class="fa fa-upload mr-1"></i> Margin Reports
	</div>
	<button type="button" class="btn btn-primary btn-sm ml-auto"
		data-toggle="modal" data-target="#mrModal">
		<i class="fa fa-plus mr-1"></i> Add Margin Report
	</button>
</div>

<!-- Create Modal -->
<div class="modal fade" id="mrModal" tabindex="-1" role="dialog"
	aria-labelledby="mrModalLabel" aria-hidden="true">
	<div class="modal-dialog modal-lg" role="document">
		<form method="post"
			action="${pageContext.request.contextPath}/orders/${order.id}/margin-reports"
			enctype="multipart/form-data" class="modal-content">
			<input type="hidden" name="${_csrf.parameterName}"
				value="${_csrf.token}" />
			<div class="modal-header">
				<h5 class="modal-title" id="mrModalLabel">Upload Margin Report</h5>
				<button type="button" class="close" data-dismiss="modal"
					aria-label="Close">
					<span>&times;</span>
				</button>
			</div>

			<div class="modal-body">
				<!-- File + label -->
				<div class="form-row align-items-center">
					<div class="col-sm-7 mb-3">
						<div class="custom-file">
							<input class="custom-file-input" id="mrFile" type="file"
								name="file" required> <label class="custom-file-label"
								for="mrFile">Choose file...</label>
						</div>
					</div>
					<div class="col-sm-5 mb-3">
						<input class="form-control" type="text" name="label"
							placeholder="Label (optional)">
					</div>
				</div>

				<!-- Buying / Selling / FX -->
				<div class="form-row">
					<div class="col-md-4 mb-3">
						<label class="mb-1">Buying price</label> <input
							class="form-control js-money-2dp" type="text" name="buyingPrice"
							placeholder="e.g. 1200" required>
					</div>
					<div class="col-md-2 mb-3">
						<label class="mb-1">Currency</label> <select
							class="form-control js-currency" name="buyingCurrency" required>
							<option value="">Select</option>
							<c:forEach var="c" items="${currencies}">
								<option value="${c}"><c:out value="${c}" /></option>
							</c:forEach>
						</select> <small class="text-muted d-block mt-1">Allowed pairs:
							same currency OR one side USD.</small>
					</div>

					<div class="col-md-4 mb-3">
						<label class="mb-1">Selling price</label> <input
							class="form-control js-money-2dp" type="text" name="sellingPrice"
							placeholder="e.g. 1500" required>
					</div>
					<div class="col-md-2 mb-3">
						<label class="mb-1">Currency</label> <select
							class="form-control js-currency" name="sellingCurrency" required>
							<option value="">Select</option>
							<c:forEach var="c" items="${currencies}">
								<option value="${c}"><c:out value="${c}" /></option>
							</c:forEach>
						</select>
					</div>
				</div>

				<div class="form-row">
					<div class="col-md-4 mb-3">
						<label class="mb-1">Conversion rate</label> <input
							class="form-control js-fx-6dp" type="text" name="conversionRate"
							placeholder="e.g. 1.000000" required> <small
							class="text-muted">Auto-set to 1.000000 when both
							currencies are USD.</small>
					</div>

					<div class="col-md-8 mb-3">
						<label class="mb-1">Vertical</label> <select class="form-control"
							name="verticalId" required>
							<option value="">Select vertical</option>
							<c:forEach var="v" items="${verticals}">
								<option value="${v.id}"><c:out value="${v.name}" /></option>
							</c:forEach>
						</select>
					</div>
				</div>

				<div class="form-group">
					<label class="mb-1">Comments</label>
					<textarea class="form-control" name="comments" rows="3"
						placeholder="Add any notes (optional)"></textarea>
				</div>
			</div>

			<div class="modal-footer">
				<button type="button" class="btn btn-secondary" data-dismiss="modal">Cancel</button>
				<button class="btn btn-primary" type="submit">
					<i class="fa fa-cloud-upload-alt mr-1"></i> Upload
				</button>
			</div>
		</form>
	</div>
</div>


<div class="table-responsive"
	style="overflow-x: auto; white-space: nowrap;">
	<table class="table table-bordered mb-0">
		<thead class="thead-light">
			<tr>
				<th>Selling Price</th>
				<th>Buying Price</th>
				<th>Conversion Rate</th>
				<th>Margin in USD</th>
				<th>Vertical</th>
				<th>Status</th>
				<th>Uploaded By</th>
				<th>Uploaded On</th>
				<th>File</th>
				<th class="text-right">Actions</th>
			</tr>
		</thead>
		<tbody>
			<c:choose>
				<c:when test="${empty marginReports}">
					<tr>
						<td colspan="10" class="text-muted text-center py-4">No
							margin reports yet.</td>
					</tr>
				</c:when>
				<c:otherwise>
					<c:forEach var="mr" items="${marginReports}">
						<tr>
							<td><fmt:formatNumber value="${mr.sellingPrice}"
									type="number" minFractionDigits="2" maxFractionDigits="2" /> <span
								class="text-muted">/ ${mr.sellingCurrency}</span></td>
							<td><fmt:formatNumber value="${mr.buyingPrice}"
									type="number" minFractionDigits="2" maxFractionDigits="2" /> <span
								class="text-muted">/ ${mr.buyingCurrency}</span></td>
							<td><fmt:formatNumber value="${mr.conversionRate}"
									type="number" minFractionDigits="2" maxFractionDigits="2" /></td>
							<td><fmt:formatNumber value="${mr.marginAmount}"
									type="number" minFractionDigits="2" maxFractionDigits="2" /></td>
							<td><c:out value="${mr.vertical.name}" /></td>
							<td><c:choose>
									<c:when test="${mr.approvalStatus == 'FINANCE_PENDING'}">
										<span class="badge badge-warning text-dark">Awaiting
											Finance</span>
									</c:when>
									<c:when test="${mr.approvalStatus == 'CEO_PENDING'}">
										<span class="badge badge-info">Awaiting CEO</span>
									</c:when>
									<c:when test="${mr.approvalStatus == 'APPROVED'}">
										<span class="badge badge-primary">Approved</span>
									</c:when>
									<c:when test="${mr.approvalStatus == 'REJECTED'}">
										<span class="badge badge-danger" title="${mr.rejectionReason}">Rejected</span>
									</c:when>
									<c:otherwise>
										<span class="badge badge-secondary">${mr.approvalStatus}</span>
									</c:otherwise>
								</c:choose></td>

							<td><c:out value="${mr.uploadedBy.firstName}" /></td>
							<td><fmt:formatDate value="${mr.uploadedOnDate}"
									pattern="yyyy-MM-dd HH:mm" timeZone="Africa/Kampala" /></td>
							<td><a
								href="${pageContext.request.contextPath}/orders/${order.id}/margin-reports/${mr.id}/download">
									<i class="fa fa-file mr-1"></i> <c:out value="${mr.fileName}" />
							</a></td>
							<td class="text-right">
								<!-- FINANCE APPROVE / REJECT --> <sec:authorize
									access="hasAnyAuthority('ROLE_FINANCE_APPROVER' ,'ROLE_ADMIN')">
									<c:if test="${mr.approvalStatus == 'FINANCE_PENDING'}">
										<form method="post" class="d-inline"
											action="${pageContext.request.contextPath}/orders/${order.id}/margin-reports/${mr.id}/approve-finance">
											<input type="hidden" name="${_csrf.parameterName}"
												value="${_csrf.token}" />
											<button class="btn btn-sm btn-outline-primary">
												<i class="fa fa-check mr-1"></i>Approve (Finance)
											</button>
										</form>

										<button type="button" class="btn btn-sm btn-outline-danger"
											data-toggle="modal" data-target="#rejectModal"
											data-mrid="${mr.id}">
											<i class="fa fa-times mr-1"></i>Reject
										</button>
									</c:if>
								</sec:authorize> <!-- CEO APPROVE / REJECT --> <sec:authorize
									access="hasAnyAuthority('ROLE_CEO','ROLE_ADMIN')">
									<c:if test="${mr.approvalStatus == 'CEO_PENDING'}">
										<form method="post" class="d-inline"
											action="${pageContext.request.contextPath}/orders/${order.id}/margin-reports/${mr.id}/approve-ceo">
											<input type="hidden" name="${_csrf.parameterName}"
												value="${_csrf.token}" />
											<button class="btn btn-sm btn-outline-primary">
												<i class="fa fa-check mr-1"></i>Approve (CEO)
											</button>
										</form>

										<button type="button" class="btn btn-sm btn-outline-danger"
											data-toggle="modal" data-target="#rejectModal"
											data-mrid="${mr.id}">
											<i class="fa fa-times mr-1"></i>Reject
										</button>
									</c:if>
								</sec:authorize>
								<button class="btn btn-sm btn-outline-primary"
									data-toggle="modal" data-target="#mrEditModal"
									data-mrid="${mr.id}" data-buying="${mr.buyingPrice}"
									data-bcur="${mr.buyingCurrency}"
									data-selling="${mr.sellingPrice}"
									data-scur="${mr.sellingCurrency}"
									data-fx="${mr.conversionRate}"
									data-vertical="${mr.vertical.id}"
									data-comments="${fn:escapeXml(mr.comments)}">
									<i class="fa fa-edit mr-1"></i>
								</button>


								<form method="post"
									action="${pageContext.request.contextPath}/orders/${order.id}/margin-reports/${mr.id}/delete"
									onsubmit="return confirm('Delete this margin report?');"
									class="d-inline">
									<input type="hidden" name="${_csrf.parameterName}"
										value="${_csrf.token}" />
									<button class="btn btn-sm btn-outline-danger">
										<i class="fa fa-trash mr-1"></i>
									</button>
								</form>

								<button type="button" class="btn btn-sm btn-outline-secondary"
									data-toggle="modal" data-target="#auditModal"
									data-audit-url="${pageContext.request.contextPath}/margin-reports/${mr.id}/audits">
									Audit</button>
							</td>
						</tr>
					</c:forEach>
				</c:otherwise>
			</c:choose>
		</tbody>
	</table>
</div>

<div id="additional-expenses" class="card mt-3">
	<div class="card-header d-flex align-items-center">
		<h3 class="card-title mb-0">
			<i class="fa fa-sack-dollar mr-2"></i> Additional Expenses
		</h3>
		<button class="btn btn-primary btn-sm ml-auto" data-toggle="modal"
			data-target="#expCreateModal">
			<i class="fa fa-plus mr-1"></i> Add Additional Expense
		</button>
	</div>
	<div id="expBody" class="collapse show">
		<div class="card-body p-0">
			<div class="table-responsive" style="white-space: nowrap;">
				<table class="table table-bordered mb-0">
					<thead class="thead-light">
						<tr>
							<th>Label</th>
							<th>Amount</th>
							<th>Conversion Rate</th>
							<th>Amount in USD</th>
							<th>Vertical</th>
							<th>Status</th>
							<th>Uploaded By</th>
							<th>Uploaded On</th>
							<th>File</th>
							<th class="text-right">Actions</th>
						</tr>
					</thead>
					<tbody>
						<c:choose>
							<c:when test="${empty additionalExpenses}">
								<tr>
									<td colspan="10" class="text-muted text-center py-4">No
										additional expenses yet.</td>
								</tr>
							</c:when>
							<c:otherwise>
								<c:forEach var="ex" items="${additionalExpenses}">
									<tr>
										<td><c:out value="${ex.label.name}" /></td>
										<td><fmt:formatNumber value="${ex.amount}" type="number"
												minFractionDigits="2" maxFractionDigits="2" /> <span
											class="text-muted">/ ${ex.currency}</span></td>
										<td><fmt:formatNumber value="${ex.conversionRate}"
												type="number" minFractionDigits="2" maxFractionDigits="6" /></td>
										<td><fmt:formatNumber value="${ex.amountUsd}"
												type="number" minFractionDigits="2" maxFractionDigits="2" /></td>
										<td><c:out value="${ex.vertical.name}" /></td>
										<td><span class="badge badge-secondary"><c:out
													value="${ex.approvalStatus}" /></span></td>
										<td><c:out value="${ex.uploadedBy.firstName}" /></td>
										<td><fmt:formatDate value="${ex.uploadedOnDate}"
												pattern="yyyy-MM-dd HH:mm" timeZone="Africa/Kampala" /></td>
										<td><c:if test="${not empty ex.fileName}">
												<a
													href="${pageContext.request.contextPath}/files/additional-expenses/${ex.id}/download">
													<i class="fa fa-file mr-1"></i> <c:out
														value="${ex.fileName}" />
												</a>
											</c:if></td>
										<td class="text-right">
											<!-- CEO approve --> <sec:authorize
												access="hasAnyAuthority('ROLE_ADMIN','ROLE_CEO')">
												<form method="post"
													action="${pageContext.request.contextPath}/orders/${order.id}/expenses/${ex.id}/approve/ceo"
													class="d-inline">
													<input type="hidden" name="${_csrf.parameterName}"
														value="${_csrf.token}" />
													<button class="btn btn-sm btn-outline-primary"
														${ex.approvalStatus != 'WAITING' ? 'disabled':''}>
														CEO Approve</button>
												</form>
											</sec:authorize> <!-- CFO approve --> <sec:authorize
												access="hasAnyAuthority('ROLE_ADMIN','ROLE_CFO')">
												<form method="post"
													action="${pageContext.request.contextPath}/orders/${order.id}/expenses/${ex.id}/approve/cfo"
													class="d-inline">
													<input type="hidden" name="${_csrf.parameterName}"
														value="${_csrf.token}" />
													<button class="btn btn-sm btn-outline-primary"
														${ex.approvalStatus != 'CEO_APPROVED' ? 'disabled':''}>
														CFO Approve</button>
												</form>
											</sec:authorize> <!-- Reject --> <sec:authorize
												access="hasAnyAuthority('ROLE_ADMIN','ROLE_CEO','ROLE_CFO')">
												<form method="post"
													action="${pageContext.request.contextPath}/orders/${order.id}/expenses/${ex.id}/reject"
													class="d-inline"
													onsubmit="return confirm('Reject this expense?');">
													<input type="hidden" name="${_csrf.parameterName}"
														value="${_csrf.token}" /> <input type="hidden"
														name="reason" value="Rejected from UI" />
													<button class="btn btn-sm btn-outline-danger"
														${ex.approvalStatus == 'REJECTED' ? 'disabled':''}>
														Reject</button>
												</form>
												
												<sec:authorize
													access="hasAnyAuthority('ROLE_ADMIN','ROLE_CFO')">
													<button type="button"
														class="btn btn-sm btn-outline-primary" data-toggle="modal"
														data-target="#expDisburseModal" data-expid="${ex.id}"
														data-exp-amount="${ex.amount}"
														data-exp-currency="${ex.currency}"
														data-exp-status="${ex.approvalStatus}">Disburse</button>
												</sec:authorize>

											</sec:authorize> <sec:authorize
												access="hasAnyAuthority('ROLE_ADMIN','ROLE_CFO','ROLE_CEO')">
												<button type="button" class="btn btn-sm btn-outline-primary"
													data-toggle="modal" data-target="#expEditModal"
													data-expid="${ex.id}" data-labelid="${ex.label.id}"
													data-amount="${ex.amount}" data-currency="${ex.currency}"
													data-rate="${ex.conversionRate}"
													data-verticalid="${ex.vertical.id}"
													data-comments="${fn:escapeXml(ex.comments)}"
													${ex.approvalStatus == 'CFO_APPROVED' ? 'disabled' : ''}>
													<i class="fa fa-edit mr-1"></i>
												</button>
											</sec:authorize> <sec:authorize access="hasAuthority('ROLE_ADMIN')">
												<button type="button" class="btn btn-sm btn-outline-danger"
													data-toggle="modal" data-target="#expDeleteModal"
													data-expid="${ex.id}"
													${ex.approvalStatus == 'CFO_APPROVED' ? 'disabled' : ''}>
													<i class="fa fa-trash mr-1"></i>
												</button>
											</sec:authorize> <!-- Audit -->
											<button type="button"
												class="btn btn-sm btn-outline-secondary" data-toggle="modal"
												data-target="#expAuditModal"
												data-audit-url="${pageContext.request.contextPath}/orders/${order.id}/expenses/${ex.id}/audits">
												Audit</button>

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
</div>

<!-- Edit Modal -->
<div class="modal fade" id="mrEditModal" tabindex="-1" role="dialog"
	aria-labelledby="mrEditLabel" aria-hidden="true">
	<div class="modal-dialog modal-lg" role="document">
		<form method="post" id="mrEditForm">
			<div class="modal-content">
				<div class="modal-header">
					<h5 class="modal-title" id="mrEditLabel">Edit Margin Report</h5>
					<button type="button" class="close" data-dismiss="modal">&times;</button>
				</div>

				<div class="modal-body">
					<div class="form-row">
						<div class="col-sm-3 mb-2">
							<label>Buying Price</label> <input class="form-control"
								type="number" step="0.01" min="0" name="buyingPrice" required>
						</div>
						<div class="col-sm-3 mb-2">
							<label>Buying Currency</label> <select class="form-control"
								name="buyingCurrency" required>
								<c:forEach var="cc" items="${currencies}">
									<option value="${cc}">${cc}</option>
								</c:forEach>
							</select>
						</div>
						<div class="col-sm-3 mb-2">
							<label>Selling Price</label> <input class="form-control"
								type="number" step="0.01" min="0" name="sellingPrice" required>
						</div>
						<div class="col-sm-3 mb-2">
							<label>Selling Currency</label> <select class="form-control"
								name="sellingCurrency" required>
								<c:forEach var="cc" items="${currencies}">
									<option value="${cc}">${cc}</option>
								</c:forEach>
							</select>
						</div>
					</div>

					<div class="form-row">
						<div class="col-sm-3 mb-2">
							<label>Conversion Rate</label> <input class="form-control"
								type="number" step="0.01" min="0" name="conversionRate" required>
							<small class="text-muted d-block mt-1">Allowed pairs:
								same currency OR one side USD.</small>
						</div>
						<div class="col-sm-5 mb-2">
							<label>Vertical</label> <select class="form-control"
								name="verticalId" required>
								<c:forEach var="v" items="${verticals}">
									<option value="${v.id}"><c:out value="${v.name}" /></option>
								</c:forEach>
							</select>
						</div>
						<div class="col-sm-12 mb-2">
							<label>Comments</label>
							<textarea class="form-control" name="comments" rows="2"></textarea>
						</div>
					</div>
				</div>

				<input type="hidden" name="${_csrf.parameterName}"
					value="${_csrf.token}" />
				<div class="modal-footer">
					<button class="btn btn-secondary" type="button"
						data-dismiss="modal">Cancel</button>
					<button class="btn btn-primary" type="submit">
						<i class="fa fa-save mr-1"></i>Save
					</button>
				</div>
			</div>
		</form>
	</div>
</div>

<!-- Audit Modal -->
<div class="modal fade" id="auditModal" tabindex="-1" aria-hidden="true">
	<div class="modal-dialog modal-lg modal-dialog-scrollable">
		<div class="modal-content">
			<div class="modal-header">
				<h5 class="modal-title">Audit trail</h5>
				<button type="button" class="close" data-dismiss="modal"
					aria-label="Close">
					<span aria-hidden="true">&times;</span>
				</button>
			</div>
			<div class="modal-body" id="auditModalBody">
				<div class="text-muted">Loading</div>
			</div>
		</div>
	</div>
</div>

<div class="modal fade" id="rejectModal" tabindex="-1" role="dialog"
	aria-hidden="true">
	<div class="modal-dialog" role="document">
		<form method="post" id="rejectForm">
			<div class="modal-content">
				<div class="modal-header">
					<h5 class="modal-title">Reject Margin Report</h5>
					<button type="button" class="close" data-dismiss="modal">&times;</button>
				</div>
				<div class="modal-body">
					<input type="hidden" name="${_csrf.parameterName}"
						value="${_csrf.token}" />
					<div class="form-group">
						<label>Reason</label>
						<textarea class="form-control" name="reason" rows="3" required></textarea>
					</div>
				</div>
				<div class="modal-footer">
					<button class="btn btn-secondary" type="button"
						data-dismiss="modal">Cancel</button>
					<button class="btn btn-danger" type="submit">
						<i class="fa fa-times mr-1"></i>Reject
					</button>
				</div>
			</div>
		</form>
	</div>
</div>
<div class="modal fade" id="expCreateModal" tabindex="-1" role="dialog"
	aria-hidden="true">
	<div class="modal-dialog modal-lg" role="document">
		<form method="post" enctype="multipart/form-data"
			action="${pageContext.request.contextPath}/orders/${order.id}/expenses"
			class="modal-content">
			<input type="hidden" name="${_csrf.parameterName}"
				value="${_csrf.token}" />
			<div class="modal-header">
				<h5 class="modal-title">Add Additional Expense</h5>
				<button type="button" class="close" data-dismiss="modal">&times;</button>
			</div>
			<div class="modal-body">

				<div class="form-row">
					<div class="col-md-6 mb-3">
						<label>Label (from Master DB)</label> <select name="labelId"
							class="form-control" required>
							<option value="">Select</option>
							<c:forEach var="lbl" items="${expenseLabels}">
								<option value="${lbl.id}"><c:out value="${lbl.name}" /></option>
							</c:forEach>
						</select>
					</div>

					<div class="col-md-3 mb-3">
						<label>Additional Cost</label> <input class="form-control"
							type="number" step="0.01" min="0.01" name="amount" required>
					</div>

					<div class="col-md-3 mb-3">
						<label>Currency</label> <select name="currency"
							class="form-control" required>
							<c:forEach var="c" items="${currencies}">
								<option value="${c}">${c}</option>
							</c:forEach>
						</select>
					</div>

					<div class="col-md-4 mb-3">
						<label>Conversion Rate</label> <input class="form-control"
							type="number" step="0.000001" min="0" name="conversionRate"
							value="1.000000" required> <small class="text-muted">To
							USD</small>
					</div>

					<div class="col-md-8 mb-3">
						<label>Vertical</label> <select class="form-control"
							name="verticalId" required>
							<c:forEach var="v" items="${verticals}">
								<option value="${v.id}"><c:out value="${v.name}" /></option>
							</c:forEach>
						</select>
					</div>
				</div>

				<div class="form-group">
					<label>Comments <span class="text-danger">*</span></label>
					<textarea class="form-control" name="comments" rows="2" required></textarea>
				</div>

				<div class="form-group">
					<label>Attachment (optional)</label> <input type="file" name="file"
						class="form-control-file">
				</div>

			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-secondary" data-dismiss="modal">Cancel</button>
				<button class="btn btn-primary" type="submit">
					<i class="fa fa-save mr-1"></i> Save
				</button>
			</div>
		</form>
	</div>
</div>
<div class="modal fade" id="expAuditModal" tabindex="-1" role="dialog"
	aria-hidden="true">
	<div class="modal-dialog modal-lg modal-dialog-scrollable">
		<div class="modal-content">
			<div class="modal-header">
				<h5 class="modal-title">Expense Audit</h5>
				<button type="button" class="close" data-dismiss="modal">&times;</button>
			</div>
			<div class="modal-body" id="expAuditBody">Loading...</div>
		</div>
	</div>
</div>


<!-- Disburse Modal -->
<div class="modal fade" id="expDisburseModal" tabindex="-1" role="dialog" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-dialog-scrollable">
    <div class="modal-content">
      <!-- Action gets set dynamically; __EXPID__ is a placeholder -->
      <form method="post"
            action="<c:url value='/orders/${order.id}/expenses/__EXPID__/disburse'/>"
            id="disburseForm">
        <div class="modal-header">
          <h5 class="modal-title">Disburse Additional Expense</h5>
          <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span>&times;</span></button>
        </div>

        <div class="modal-body">

          <!-- Inline expense meta -->
          <div class="alert alert-info py-2 mb-3" id="expMetaBar">
            <strong>Expense:</strong>
            <span id="metaAmount"></span> <span id="metaCurrency"></span>
            &nbsp;|&nbsp; <strong>Status:</strong> <span id="metaStatus"></span>
            &nbsp;|&nbsp; <strong>Total Disbursed:</strong>
            <span id="metaDisbursed">0</span> <span id="metaDisbursedCur"></span>
          </div>

          <div class="form-row">
            <div class="form-group col-md-4">
              <label>Amount</label>
              <input type="number" step="0.01" min="0.01" class="form-control" name="amount" required>
            </div>
            <div class="form-group col-md-4">
              <label>Currency</label>
              <select name="currency" class="form-control" required>
                <c:forEach var="cur" items="${CurrencyCode.values()}">
                  <option value="${cur}" ${cur==order.currency?'selected':''}>${cur}</option>
                </c:forEach>
              </select>
              <small class="form-text text-muted">Must match the expense currency.</small>
            </div>
            <div class="form-group col-md-12">
              <label>Comments</label>
              <textarea class="form-control" name="comments" rows="2" required></textarea>
            </div>
          </div>

          <hr class="my-3"/>

          <h6 class="mb-2">Past Disbursements</h6>
          <div id="disbTableWrap" class="table-responsive" style="white-space:nowrap;">
            <div class="text-muted">Loading...</div>
          </div>

        </div>

        <div class="modal-footer">
          <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" id="csrfTokenField"/>
          <button type="submit" class="btn btn-primary">Disburse</button>
          <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
        </div>
      </form>
    </div>
  </div>
</div>



<!-- Edit Additional Expense Modal -->
<div class="modal fade" id="expEditModal" tabindex="-1"
	aria-hidden="true">
	<div class="modal-dialog modal-lg modal-dialog-scrollable">
		<form id="expEditForm" method="post">
			<input type="hidden" name="${_csrf.parameterName}"
				value="${_csrf.token}" />
			<div class="modal-content">
				<div class="modal-header">
					<h5 class="modal-title">Edit Additional Expense</h5>
					<button type="button" class="close" data-dismiss="modal">&times;</button>
				</div>

				<div class="modal-body">
					<div class="form-row">
						<div class="form-group col-md-6">
							<label>Description (Label)</label> <select class="form-control"
								name="labelId" id="expEditLabel">
								<c:forEach var="l" items="${expenseLabels}">
									<option value="${l.id}" ${!l.active ? 'disabled':''}>
										<c:out value="${l.name}" />
										<c:if test="${!l.active}"> (inactive)</c:if>
									</option>
								</c:forEach>
							</select>
						</div>

						<div class="form-group col-md-3">
							<label>Amount</label> <input type="number" step="0.01" min="0"
								class="form-control" name="amount" id="expEditAmount" required>
						</div>

						<div class="form-group col-md-3">
							<label>Currency</label> <select class="form-control"
								name="currency" id="expEditCurrency" required>
								<c:forEach var="c" items="${currencies}">
									<option value="${c}">${c}</option>
								</c:forEach>
							</select>
						</div>

						<div class="form-group col-md-4">
							<label>Conversion Rate</label> <input type="number"
								step="0.000001" min="0" class="form-control"
								name="conversionRate" id="expEditRate" required>
						</div>

						<div class="form-group col-md-4">
							<label>Vertical</label> <select class="form-control"
								name="verticalId" id="expEditVertical" required>
								<c:forEach var="v" items="${verticals}">
									<option value="${v.id}"><c:out value="${v.name}" /></option>
								</c:forEach>
							</select>
						</div>

						<div class="form-group col-md-12">
							<label>Comments <span class="text-danger">*</span></label>
							<textarea class="form-control" name="comments"
								id="expEditComments" rows="2" required></textarea>
						</div>
					</div>
					<small class="text-muted">Note: File cannot be changed
						here. Delete & re-add if you must replace the file.</small>
				</div>

				<div class="modal-footer">
					<button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>
					<button type="submit" class="btn btn-primary">Save changes</button>
				</div>
			</div>
		</form>
	</div>
</div>
<!-- Delete Expense Modal -->
<div class="modal fade" id="expDeleteModal" tabindex="-1"
	aria-hidden="true">
	<div class="modal-dialog">
		<form id="expDeleteForm" method="post">
			<input type="hidden" name="${_csrf.parameterName}"
				value="${_csrf.token}" />
			<div class="modal-content">
				<div class="modal-header">
					<h5 class="modal-title text-danger">Delete Additional Expense</h5>
					<button type="button" class="close" data-dismiss="modal">&times;</button>
				</div>
				<div class="modal-body">
					<p class="mb-2">Are you sure you want to delete this additional
						expense?</p>
					<div class="custom-control custom-checkbox">
						<input type="checkbox" class="custom-control-input"
							id="expDeleteFile" name="deleteFile" value="true" checked>
						<label class="custom-control-label" for="expDeleteFile">Also
							delete the uploaded file</label>
					</div>
					<small class="text-muted d-block mt-2">You can only delete
						items that are not CFO-approved.</small>
				</div>
				<div class="modal-footer">
					<button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>
					<button type="submit" class="btn btn-danger">Yes, delete</button>
				</div>
			</div>
		</form>
	</div>
</div>


