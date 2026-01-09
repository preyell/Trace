<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<!-- Optional: plugin; safe to keep even if globally included -->
<script
	src="https://cdn.jsdelivr.net/npm/bs-custom-file-input/dist/bs-custom-file-input.min.js"></script>
<style>
/* Only affect this modal */
#expDisburseModal .modal-body {
	max-height: 70vh; /* keep body within viewport */
	overflow-y: auto; /* scroll body if needed */
}

#expDisburseModal #disbTableWrap {
	max-height: 250px; /* limit table height */
	overflow-y: auto; /* table area scrolls */
}
</style>

<c:set var="mrVerticalIds" value="" />
<c:forEach var="mr" items="${marginReports}">
	<c:if test="${mr != null && mr.vertical != null}">
		<c:set var="mrVerticalIds" value="${mrVerticalIds},${mr.vertical.id}," />
	</c:if>
</c:forEach>

<c:if test="${not empty error}">
	<div class="alert alert-danger">${error}</div>
</c:if>

<c:if test="${not empty message}">
	<div class="alert alert-primary">${message}</div>
</c:if>
<div class="d-flex align-items-center mb-3">

	<!-- Left side buttons -->
	<div class="mr-2">

		<c:if test="${allMarginsApproved}">
			<a
				href="${pageContext.request.contextPath}/orders/${order.id}/net-margin-report/preview"
				target="_blank" class="btn btn-primary btn-sm mr-2"> <i
				class="fa fa-eye mr-1"></i> Preview Net Margin Report
			</a>

			<a
				href="${pageContext.request.contextPath}/orders/${order.id}/net-margin-report"
				class="btn btn-primary btn-sm mr-2"> <i
				class="fa fa-file-pdf-o mr-1"></i> Download Net Margin Report
			</a>
		</c:if>

		<c:if test="${!allMarginsApproved}">
			<button class="btn btn-primary btn-sm mr-2" disabled
				title="Margin report must be fully approved to preview net margin report.">
				<i class="fa fa-eye mr-1"></i> Preview Net Margin Report
			</button>

			<button class="btn btn-primary btn-sm mr-2" disabled
				title="Margin report must be fully approved to download net margin report.">
				<i class="fa fa-file-pdf-o mr-1"></i> Download Net Margin Report
			</button>
		</c:if>

	</div>

	<!-- Right side button -->
	<div class="ml-auto">
		<button type="button" class="btn btn-primary btn-sm"
			data-toggle="modal" data-target="#mrModal">
			<i class="fa fa-plus mr-1"></i> Add Margin Report
		</button>
	</div>

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
				<c:if test="${not empty marginError}">
					<div class="alert alert-danger py-1 small mb-2">
						<i class="fa fa-exclamation-circle mr-1"></i> ${marginError}
					</div>
				</c:if>
				<div id="mrCreateErrorAlert"
					class="alert alert-danger py-1 small mb-2 d-none">
					<i class="fa fa-exclamation-circle mr-1"></i> <span
						id="mrCreateErrorText"></span>
				</div>


				<div class="form-row align-items-center">
					<div class="col-sm-7 mb-3">
						<div class="custom-file">
							<input class="custom-file-input" id="mrFile" type="file"
								name="file" required> <label class="custom-file-label"
								for="mrFile">Choose file...</label>
						</div>
					</div>

				</div>

				<!-- Buying / Selling / FX -->
				<div class="form-row">
					<div class="col-md-4 mb-3">
						<label class="mb-1">Buying price</label> <input
							class="form-control" type="number" step="0.01" min="0"
							name="buyingPrice" required>

					</div>
					<div class="col-md-2 mb-3">
						<label class="mb-1">Currency</label> <select name="buyingCurrency"
							class="form-control mr-buying-currency" required>
							<c:forEach var="cur" items="${currencies}">
								<option value="${cur}">${cur}</option>
							</c:forEach>
						</select> <small class="text-muted d-block mt-1">Allowed pairs:
							same currency OR one side USD.</small>
					</div>

					<div class="col-md-4 mb-3">
						<label class="mb-1">Selling price</label> <input
							class="form-control" type="number" step="0.01" min="0"
							name="sellingPrice" required>

					</div>
					<div class="col-md-2 mb-3">
						<label class="mb-1">Currency</label> <select
							name="sellingCurrency" class="form-control mr-selling-currency"
							required>
							<c:forEach var="cur" items="${currencies}">
								<option value="${cur}">${cur}</option>
							</c:forEach>
						</select>

					</div>
				</div>

				<div class="form-row">
					<div class="col-md-4 mb-3">
						<label class="mb-1">Conversion rate</label> <input type="number"
							name="conversionRate" class="form-control mr-conversion-rate"
							step="0.01" min="0.00" required> <small
							class="text-muted">Auto-set to 1.000000 when both
							currencies are USD.</small>
					</div>

					<div class="col-md-8 mb-3">
						<label class="mb-1">Vertical</label> <select name="verticalId"
							class="form-control" required>
							<c:forEach var="v" items="${verticals}">
								<option value="${v.id}">
									<c:out value="${v.name}" />
								</option>
							</c:forEach>
						</select>


					</div>
				</div>

				<div class="form-group">
					<label class="mb-1">Comments</label>
					<textarea class="form-control" name="comments" rows="3"
						placeholder="Add any notes" required></textarea>
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
										<span class="badge badge-primary">Awaiting Finance</span>
									</c:when>
									<c:when test="${mr.approvalStatus == 'CEO_PENDING'}">
										<span class="badge badge-primary">Awaiting CEO</span>
									</c:when>
									<c:when test="${mr.approvalStatus == 'APPROVED'}">
										<span class="badge badge-primary">Approved</span>
									</c:when>
									<c:when test="${mr.approvalStatus == 'REJECTED'}">
										<span class="badge badge-danger" title="${mr.rejectionReason}">Rejected</span>
									</c:when>
									<c:otherwise>
										<span class="badge badge-primary">${mr.approvalStatus}</span>
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
								</sec:authorize> <c:choose>
									<c:when test="${mr.approvalStatus == 'APPROVED'}">
										<button class="btn btn-sm btn-outline-secondary" type="button"
											disabled title="Approved margin report cannot be edited">
											<i class="fa fa-lock mr-1"></i>
										</button>
									</c:when>

									<c:otherwise>
										<button class="btn btn-sm btn-outline-primary" type="button"
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
									</c:otherwise>
								</c:choose> <sec:authorize access="hasAuthority('ROLE_ADMIN')">
									<button type="button" class="btn btn-sm btn-outline-danger"
										data-toggle="modal" data-target="#mrDeleteModal"
										data-mrid="${mr.id}">
										<i class="fa fa-trash"></i>
									</button>

								</sec:authorize>
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
	<!-- Header: title left, button right -->
	<div class="card-header d-flex align-items-center">
		<h3 class="card-title mb-0">
			<i class="fa fa-sack-dollar mr-2"></i> Additional Expenses
		</h3>

		<c:choose>
			<c:when test="${empty marginReports}">
				<button type="button" class="btn btn-primary btn-sm ml-auto"
					disabled title="Upload at least one margin report first.">
					<i class="fa fa-plus"></i> Add Additional Expense
				</button>
			</c:when>

			<c:when test="${empty mrVerticalIds}">
				<button type="button" class="btn btn-primary btn-sm ml-auto"
					disabled title="Upload at least one margin report first.">
					<i class="fa fa-plus"></i> Add Additional Expense
				</button>
			</c:when>

			<c:otherwise>
				<button type="button" class="btn btn-primary btn-sm ml-auto"
					data-toggle="modal" data-target="#expCreateModal">
					<i class="fa fa-plus"></i> Add Additional Expense
				</button>
			</c:otherwise>
		</c:choose>

	</div>

	<!-- Info text shown only when there are NO margin reports -->
	<c:if test="${empty marginReports}">
		<div class="card-body py-2">
			<span class="text-muted small"> You must create at least one
				Margin Report before uploading additional expenses. </span>
		</div>
	</c:if>

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
									<td><c:choose>
											<c:when test="${ex.approvalStatus == 'WAITING'}">
												<span class="badge badge-primary">Awaiting CEO</span>
											</c:when>
											<c:when test="${ex.approvalStatus == 'CEO_APPROVED'}">
												<span class="badge badge-primary">Awaiting CFO</span>
											</c:when>
											<c:when test="${ex.approvalStatus == 'CFO_APPROVED'}">
												<span class="badge badge-primary">Approved</span>
											</c:when>
											<c:when test="${ex.approvalStatus == 'REJECTED'}">
												<span class="badge badge-danger">Rejected</span>
											</c:when>
											<c:otherwise>
												<span class="badge badge-primary">${mr.approvalStatus}</span>
											</c:otherwise>
										</c:choose></td>
									<td><c:out value="${ex.uploadedBy.firstName}" /></td>
									<td><fmt:formatDate value="${ex.uploadedOnDate}"
											pattern="yyyy-MM-dd HH:mm" timeZone="Africa/Kampala" /></td>
									<td><c:if test="${not empty ex.fileName}">
											<a
												href="${pageContext.request.contextPath}/orders/${order.id}/expenses/${ex.id}/download">
												<i class="fa fa-file mr-1"></i> <c:out
													value="${ex.fileName}" />
											</a>
										</c:if></td>
									<td class="text-right">
										<!-- CEO approve --> <sec:authorize
											access="hasAnyAuthority('ROLE_CEO','ROLE_ADMIN')">
											<c:if test="${ex.approvalStatus == 'WAITING'}">
												<!-- CEO Approve -->
												<form method="post" class="d-inline"
													action="${pageContext.request.contextPath}/orders/${order.id}/expenses/${ex.id}/approve/ceo">
													<input type="hidden" name="${_csrf.parameterName}"
														value="${_csrf.token}" />
													<button class="btn btn-sm btn-outline-primary">
														<i class="fa fa-check mr-1"></i>Approve (CEO)
													</button>
												</form>

												<button type="button" class="btn btn-sm btn-outline-danger"
													data-toggle="modal" data-target="#expenseRejectModal"
													data-expid="${ex.id}">
													<i class="fa fa-times mr-1"></i>Reject
												</button>
											</c:if>
										</sec:authorize> <!-- CFO approve --> <sec:authorize
											access="hasAnyAuthority('ROLE_ADMIN','ROLE_CFO')">
											<c:if test="${ex.approvalStatus == 'CEO_APPROVED'}">
												<form method="post" class="d-inline"
													action="${pageContext.request.contextPath}/orders/${order.id}/expenses/${ex.id}/approve/cfo">
													<input type="hidden" name="${_csrf.parameterName}"
														value="${_csrf.token}" />
													<button class="btn btn-sm btn-outline-primary">
														CFO Approve</button>
												</form>
												<button type="button" class="btn btn-sm btn-outline-danger"
													data-toggle="modal" data-target="#expenseRejectModal"
													data-expid="${ex.id}">
													<i class="fa fa-times mr-1"></i>Reject
												</button>
											</c:if>
										</sec:authorize> <sec:authorize
											access="hasAnyAuthority('ROLE_ADMIN','ROLE_CEO','ROLE_CFO')">

											<c:set var="canConsume"
												value="${ex.approvalStatus == 'CFO_APPROVED'}" />

											<button type="button"
												class="btn btn-sm btn-outline-primary js-exp-consume
                 ${canConsume ? '' : 'disabled'}"
												data-toggle="modal"
												data-target="${canConsume ? '#expDisburseModal' : ''}"
												data-expid="${ex.id}" data-exp-amount="${ex.amount}"
												data-exp-currency="${ex.currency}"
												data-exp-status="${ex.approvalStatus}"
												data-exp-usd="${ex.amountUsd}"
												${canConsume ? '' : 'disabled'}
												title="${canConsume
                    ? 'Consume expense'
                    : 'Expense must be CFO approved before it can be consumed'}">
												Consume</button>

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
												${ex.approvalStatus == 'CFO_APPROVED'
           and not pageContext.request.isUserInRole('ROLE_ADMIN')
             ? 'disabled' : ''}>
												<i class="fa fa-trash"></i>
											</button>

										</sec:authorize> <!-- Audit -->
										<button type="button" class="btn btn-sm btn-outline-secondary"
											data-toggle="modal" data-target="#expAuditModal"
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
		<form method="post" id="mrEditForm" enctype="multipart/form-data">
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
							<label>Buying Currency</label> <select name="buyingCurrency"
								class="form-control mr-buying-currency" required>
								<c:forEach var="cur" items="${currencies}">
									<option value="${cur}"
										${cur == mr.buyingCurrency ? 'selected' : ''}>${cur}</option>
								</c:forEach>
							</select>
						</div>
						<div class="col-sm-3 mb-2">
							<label>Selling Price</label> <input class="form-control"
								type="number" step="0.01" min="0" name="sellingPrice" required>
						</div>
						<div class="col-sm-3 mb-2">
							<label>Selling Currency</label> <select name="sellingCurrency"
								class="form-control mr-selling-currency" required>
								<c:forEach var="cur" items="${currencies}">
									<option value="${cur}"
										${cur == mr.sellingCurrency ? 'selected' : ''}>${cur}</option>
								</c:forEach>
							</select>
						</div>
					</div>

					<div class="form-row">
						<div class="col-sm-3 mb-2">
							<label>Conversion Rate</label> <input type="number"
								name="conversionRate" class="form-control mr-conversion-rate"
								step="0.01" min="0.00" value="${mr.conversionRate}" required>
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

					<c:if test="${not empty mr.fileName}">
						<div class="form-group">
							<label>Current Attachment</label><br /> <a
								href="${pageContext.request.contextPath}/files/margins/${mr.id}"
								target="_blank"> <i class="fa fa-paperclip mr-1"></i> <c:out
									value="${mr.fileName}" />
							</a>
						</div>
					</c:if>

					<div class="form-group">
						<label>Replace Attachment (optional)</label> <input type="file"
							name="file" class="form-control-file"><small
							class="form-text text-muted"> Leave empty to keep the
							existing file. </small>
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
							class="form-control ae-currency" required>
							<c:forEach var="cur" items="${currencies}">
								<option value="${cur}">${cur}</option>
							</c:forEach>
						</select>
					</div>

					<div class="col-md-4 mb-3">
						<label>Conversion Rate</label> <input type="number"
							name="conversionRate" class="form-control ae-conversion-rate"
							step="0.01" min="0.00" required> <small
							class="text-muted">To USD</small>
					</div>

					<div class="col-md-8 mb-3">
						<label>Vertical</label> <select class="form-control"
							name="verticalId"
							${empty expenseVerticals ? 'disabled="disabled"' : ''} required>

							<c:choose>
								<c:when test="${empty expenseVerticals}">
									<option value="">Upload a margin report first</option>
								</c:when>
								<c:otherwise>
									<option value="">Select</option>
									<c:forEach var="v" items="${expenseVerticals}">
										<option value="${v.id}">
											<c:out value="${v.name}" />
										</option>
									</c:forEach>
								</c:otherwise>
							</c:choose>

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


<!-- Consume Modal -->
<div class="modal fade" id="expDisburseModal" tabindex="-1"
	role="dialog" aria-hidden="true">
	<div class="modal-dialog modal-lg modal-dialog-scrollable">
		<div class="modal-content">
			<!-- Action gets set dynamically; __EXPID__ is a placeholder -->
			<form method="post"
				action="<c:url value='/orders/${order.id}/expenses/__EXPID__/consume'/>"
				id="disburseForm">
				<div class="modal-header">
					<h5 class="modal-title">Consume Additional Expense</h5>
					<button type="button" class="close" data-dismiss="modal"
						aria-label="Close">
						<span>&times;</span>
					</button>
				</div>

				<div class="modal-body">

					<div id="expConsumeErrorAlert"
						class="alert alert-danger py-1 small mb-2 d-none">
						<i class="fa fa-exclamation-circle mr-1"></i> <span
							id="expConsumeErrorText"></span>
					</div>

					<!-- Inline expense meta -->
					<div class="alert alert-info py-2 mb-3" id="expMetaBar">
						<strong>Expense:</strong> <span id="metaAmount"></span> <span
							id="metaCurrency"></span> &nbsp;|&nbsp; <strong>Status:</strong>
						<span id="metaStatus"></span> &nbsp;|&nbsp; <strong>Total
							Consumed:</strong> <span id="metaDisbursed">0</span> <span
							id="metaDisbursedCur"></span> &nbsp;|&nbsp; <strong>Remaining:</strong>
						<span id="metaRemaining">0</span> <span id="metaRemainingCur"></span>
					</div>

					<div class="form-row">
						<div class="form-group col-md-4">
							<label>Amount to Consume</label> <input type="number" step="0.01"
								min="0.01" class="form-control" name="amount" required>
						</div>

						<div class="form-group col-md-4">
							<label>Consumption Currency</label> <select name="currency"
								class="form-control ae-disburse-currency">
								<c:forEach var="c" items="${currencies}">
									<option value="${c}">${c}</option>
								</c:forEach>
							</select>
						</div>

						<div class="form-group col-md-4">
							<label>Conversion Rate</label> <input type="text"
								name="conversionRate" class="form-control ae-disburse-rate">
						</div>

						<div class="form-group col-md-12">
							<label>Comments</label>
							<textarea class="form-control" name="note" rows="2" required></textarea>
						</div>
					</div>

					<hr class="my-3" />

					<h6 class="mb-2">Past Consumptions</h6>
					<div id="disbTableWrap" class="table-responsive"
						style="white-space: nowrap;">
						<div class="text-muted">Loading...</div>
					</div>

				</div>

				<div class="modal-footer">
					<input type="hidden" name="${_csrf.parameterName}"
						value="${_csrf.token}" id="csrfTokenField" />
					<button type="submit" class="btn btn-primary">Consume</button>
					<button type="button" class="btn btn-secondary"
						data-dismiss="modal">Close</button>
				</div>
			</form>
		</div>
	</div>
</div>

<c:if test="${not empty expenseError}">
	<div id="consumeErrData" data-expid="${openConsumeExpenseId}"
		data-msg="${expenseError}"></div>
</c:if>



<!-- Edit Additional Expense Modal -->
<div class="modal fade" id="expEditModal" tabindex="-1"
	aria-hidden="true">
	<div class="modal-dialog modal-lg modal-dialog-scrollable">
		<form id="expEditForm" method="post" enctype="multipart/form-data">
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
							<label>Currency</label> <select name="currency"
								id="expEditCurrency" class="form-control ae-currency" required>
								<c:forEach var="cur" items="${currencies}">
									<option value="${cur}">${cur}</option>
								</c:forEach>
							</select>
						</div>

						<div class="form-group col-md-4">
							<label>Conversion Rate</label> <input type="number"
								name="conversionRate" id="expEditRate"
								class="form-control ae-conversion-rate" step="0.01" min="0.00"
								required>
						</div>

						<div class="form-group col-md-4">
							<label>Vertical</label> <select class="form-control"
								name="verticalId" id="expEditVertical" required>
								<c:forEach var="v" items="${expenseVerticals}">
									<option value="${v.id}"><c:out value="${v.name}" /></option>
								</c:forEach>
							</select>
						</div>

						<div class="form-group col-md-12">
							<label>Comments <span class="text-danger">*</span></label>
							<textarea class="form-control" name="comments"
								id="expEditComments" rows="2" required></textarea>
						</div>


						<div class="form-group">
							<label>Replace Attachment (optional)</label> <input type="file"
								name="file" class="form-control-file"> <small
								class="form-text text-muted"> Leave empty to keep the
								existing file. </small>
						</div>
					</div>

					<div class="modal-footer">
						<button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>
						<button type="submit" class="btn btn-primary">Save
							changes</button>
					</div>
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
					<small class="text-muted d-block mt-2">Non-admin users can
						only delete items that are not CFO-approved.</small>
				</div>
				<div class="modal-footer">
					<button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>
					<button type="submit" class="btn btn-danger">Yes, delete</button>
				</div>
			</div>
		</form>
	</div>
</div>

<div class="modal fade" id="expenseRejectModal" tabindex="-1"
	role="dialog" aria-hidden="true">
	<div class="modal-dialog">
		<div class="modal-content">

			<form method="post" id="expenseRejectForm">
				<div class="modal-header">
					<h5 class="modal-title">Reject Additional Expense</h5>
					<button type="button" class="close" data-dismiss="modal">
						<span>&times;</span>
					</button>
				</div>

				<div class="modal-body">
					<label>Reason for rejection</label>
					<textarea name="reason" class="form-control" required></textarea>

					<input type="hidden" name="${_csrf.parameterName}"
						value="${_csrf.token}">
				</div>

				<div class="modal-footer">
					<button type="submit" class="btn btn-danger">Reject</button>
					<button type="button" class="btn btn-secondary"
						data-dismiss="modal">Close</button>
				</div>

			</form>

		</div>
	</div>
</div>

<!-- Delete Margin Report Modal -->
<div class="modal fade" id="mrDeleteModal" tabindex="-1"
	aria-hidden="true">
	<div class="modal-dialog">
		<form id="mrDeleteForm" method="post">
			<input type="hidden" name="${_csrf.parameterName}"
				value="${_csrf.token}" />
			<div class="modal-content">
				<div class="modal-header">
					<h5 class="modal-title text-danger">Delete Margin Report</h5>
					<button type="button" class="close" data-dismiss="modal">&times;</button>
				</div>
				<div class="modal-body">
					<p class="mb-2">Are you sure you want to delete this margin
						report?</p>
					<div class="custom-control custom-checkbox">
						<input type="checkbox" class="custom-control-input"
							id="mrDeleteFile" name="deleteFile" value="true" checked>
						<label class="custom-control-label" for="mrDeleteFile">
							Also delete the uploaded file </label>
					</div>
					<small class="text-muted d-block mt-2"> You can only delete
						margin reports that are not fully approved (depending on your
						role). </small>
				</div>
				<div class="modal-footer">
					<button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>
					<button type="submit" class="btn btn-danger">Yes, delete</button>
				</div>
			</div>
		</form>
	</div>
</div>

