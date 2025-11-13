<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>


<div class="card">
	<div class="card-body">
		<c:if test="${not empty message}">
			<div class="alert alert-primary alert-dismissible fade show m-3">
				${message}
				<button type="button" class="close" data-dismiss="alert">&times;</button>
			</div>
		</c:if>

		<c:choose>
			<c:when test="${empty form.id}">
				<c:url var="postUrl" value="/orders" />
			</c:when>
			<c:otherwise>
				<c:url var="postUrl" value="/orders/${form.id}" />
			</c:otherwise>
		</c:choose>

		<form:form method="post" modelAttribute="form" action="${postUrl}">
			<input type="hidden" name="${_csrf.parameterName}"
				value="${_csrf.token}" />

			<!-- Sales Order ID -->
			<spring:bind path="salesOrderId">
				<div class="mb-3">
					<label class="form-label">Sales Order ID</label>
					<form:input path="salesOrderId"
						cssClass="form-control ${status.error ? 'is-invalid' : ''}"
						maxlength="50" placeholder="e.g., SO-UG-2025-00123" />
					<small class="text-muted">Allowed: letters, numbers, dot
						(.), dash (-), underscore (_)</small>
					<form:errors path="salesOrderId"
						cssClass="invalid-feedback d-block fw-semibold fs-6" />
				</div>
			</spring:bind>

			<!-- Customer -->
			<spring:bind path="customerId">
				<div class="mb-3">
					<label class="form-label">Customer</label>
					<form:select path="customerId"
						cssClass="form-control ${status.error ? 'is-invalid' : ''}">
						<form:option value="" label="-- Select Customer --" />
						<c:forEach var="c" items="${customers}">
							<form:option value="${c.id}" label="${c.name}" />
						</c:forEach>
					</form:select>
					<form:errors path="customerId"
						cssClass="invalid-feedback d-block fw-semibold fs-6" />
				</div>
			</spring:bind>

			<!-- Description -->
			<spring:bind path="description">
				<div class="mb-3">
					<label class="form-label">Description</label>
					<form:textarea path="description" rows="3"
						cssClass="form-control ${status.error ? 'is-invalid' : ''}"
						maxlength="500" />
					<form:errors path="description"
						cssClass="invalid-feedback d-block fw-semibold fs-6" />
				</div>
			</spring:bind>

			<!-- Sales Manager -->
			<spring:bind path="salesManagerId">
				<div class="mb-3">
					<label class="form-label">Sales Manager</label>
					<form:select path="salesManagerId"
						cssClass="form-control ${status.error ? 'is-invalid' : ''}">
						<form:option value="" label="-- Select Sales Manager --" />
						<c:forEach var="u" items="${salesManagers}">
							<form:option value="${u.id}"
								label="${u.firstName} ${u.lastName} (${u.username})" />
						</c:forEach>
					</form:select>
					<form:errors path="salesManagerId"
						cssClass="invalid-feedback d-block fw-semibold fs-6" />
				</div>
			</spring:bind>

			<!-- Verticals (multi-select dropdown; selected shown inside trigger as plain text) -->
			<spring:bind path="verticalIds">
				<div class="mb-3">
					<label class="form-label">Verticals</label>

					<div class="dropdown d-inline-block w-100">
						<!-- Trigger -->
						<button
							class="btn btn-outline-secondary dropdown-toggle text-left w-100"
							type="button" id="verticalsDropdownBtn" data-toggle="dropdown"
							aria-haspopup="true" aria-expanded="false"
							style="white-space: normal; min-height: 38px;">

							<!-- Selected items text goes here -->
							<span id="verticalsSummary" class="text-muted">Select
								verticals</span>
						</button>

						<!-- Menu with checkboxes -->
						<div class="dropdown-menu p-2 w-100"
							aria-labelledby="verticalsDropdownBtn" id="verticalsMenu"
							style="max-height: 260px; overflow: auto;">
							<c:forEach var="v" items="${verticals}">
								<div class="form-check">
									<input class="form-check-input vertical-checkbox"
										type="checkbox" name="verticalIds" value="${v.id}"
										id="v_${v.id}"
										<c:if test="${form.verticalIds != null and form.verticalIds.contains(v.id)}">checked</c:if>>
									<label class="form-check-label" for="v_${v.id}">${v.name}</label>
								</div>
							</c:forEach>
						</div>
					</div>

					<form:errors path="verticalIds"
						cssClass="invalid-feedback d-block fw-semibold fs-6" />
				</div>
			</spring:bind>


			<!-- Location -->
			<spring:bind path="location">
				<div class="mb-3">
					<label class="form-label">Location</label>
					<form:select path="location"
						cssClass="form-control ${status.error ? 'is-invalid' : ''}">
						<form:option value="" label="-- Select Location --" />
						<c:forEach var="l" items="${locations}">
							<form:option value="${l.name()}" label="${l.label()}" />
						</c:forEach>
					</form:select>
					<form:errors path="location"
						cssClass="invalid-feedback d-block fw-semibold fs-6" />
				</div>
			</spring:bind>

			<div class="d-flex">
				<button class="btn btn-primary mr-2" type="submit">
					<c:out value="${empty form.id ? 'Create' : 'Update'}" />
				</button>
				<a class="btn btn-secondary" href="<c:url value='/orders'/>">Cancel</a>
			</div>
		</form:form>
	</div>
</div>

<script>
	(function() {
		// Keep dropdown open when clicking inside
		var menu = document.getElementById('verticalsMenu');
		if (menu)
			menu.addEventListener('click', function(e) {
				e.stopPropagation();
			});

		function updateVerticalsSummary() {
			var boxes = document.querySelectorAll('.vertical-checkbox');
			var selected = [];
			boxes.forEach(function(cb) {
				if (cb.checked) {
					var label = document.querySelector('label[for="' + cb.id
							+ '"]');
					selected.push(label ? label.textContent.trim() : cb.value);
				}
			});

			var summary = document.getElementById('verticalsSummary');
			if (!summary)
				return;

			if (selected.length === 0) {
				summary.textContent = 'Select verticals';
				summary.classList.add('text-muted');
			} else {
				summary.textContent = selected.join(', ');
				summary.classList.remove('text-muted');
			}
		}

		document.addEventListener('change', function(e) {
			if (e.target && e.target.classList.contains('vertical-checkbox')) {
				updateVerticalsSummary();
			}
		});

		document.addEventListener('DOMContentLoaded', updateVerticalsSummary);
		window.setTimeout(updateVerticalsSummary, 0);
	})();
</script>
