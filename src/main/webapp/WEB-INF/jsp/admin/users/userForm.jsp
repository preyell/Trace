<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<c:choose>
	<c:when test="${mode eq 'edit'}">
		<c:url var="postUrl" value="/admin/users/${form.id}" />
	</c:when>
	<c:otherwise>
		<c:url var="postUrl" value="/admin/users" />
	</c:otherwise>
</c:choose>

<div class="card">
	<div class="card-body">
		<form:form method="post" modelAttribute="form" action="${postUrl}">
			<input type="hidden" name="${_csrf.parameterName}"
				value="${_csrf.token}" />

			<!-- Username -->
			<spring:bind path="username">
				<div class="mb-3">
					<label class="form-label" for="username">Username</label>
					<form:input path="username" id="username"
						cssClass="form-control ${status.error ? 'is-invalid' : ''}"
						maxlength="50" required="required" readonly="${mode eq 'edit'}" />
					<form:errors path="username"
						cssClass="invalid-feedback d-block fw-semibold fs-6" />
				</div>
			</spring:bind>

			<!-- Email -->
			<spring:bind path="email">
				<div class="mb-3">
					<label class="form-label" for="email">Email</label>
					<form:input path="email" id="email" type="email"
						cssClass="form-control ${status.error ? 'is-invalid' : ''}"
						maxlength="200" required="required" />
					<form:errors path="email"
						cssClass="invalid-feedback d-block fw-semibold fs-6" />
				</div>
			</spring:bind>

			<!-- First name -->
			<spring:bind path="firstName">
				<div class="mb-3">
					<label class="form-label" for="firstName">First name</label>
					<form:input path="firstName" id="firstName"
						cssClass="form-control ${status.error ? 'is-invalid' : ''}"
						required="required" />
					<form:errors path="firstName"
						cssClass="invalid-feedback d-block fw-semibold fs-6" />
				</div>
			</spring:bind>

			<!-- Last name -->
			<spring:bind path="lastName">
				<div class="mb-3">
					<label class="form-label" for="lastName">Last name</label>
					<form:input path="lastName" id="lastName"
						cssClass="form-control ${status.error ? 'is-invalid' : ''}"
						required="required" />
					<form:errors path="lastName"
						cssClass="invalid-feedback d-block fw-semibold fs-6" />
				</div>
			</spring:bind>

			<!-- Roles (dropdown with checkboxes) -->
			<!-- Roles (dropdown with checkboxes; selected shown inside trigger) -->
			<spring:bind path="roles">
				<div class="mb-3">
					<label class="form-label">Roles</label>

					<div class="dropdown d-inline-block w-100">
						<button
							class="btn btn-outline-secondary dropdown-toggle text-left w-100"
							type="button" id="rolesDropdownBtn" data-toggle="dropdown"
							aria-haspopup="true" aria-expanded="false"
							style="white-space: normal; min-height: 38px;">
							<span id="rolesSummary" class="text-muted">Select roles</span>
						</button>

						<div class="dropdown-menu p-2 w-100"
							aria-labelledby="rolesDropdownBtn" id="rolesMenu"
							style="max-height: 260px; overflow: auto;">
							<c:forEach var="r" items="${roles}">
								<div class="form-check">
									<input class="form-check-input role-checkbox" type="checkbox"
										name="roles" value="${r}" id="role_${r}"
										<c:if test="${not empty form.roles and form.roles.contains(r)}">checked</c:if>>
									<label class="form-check-label" for="role_${r}">${r.label()}</label>
								</div>
							</c:forEach>
						</div>
					</div>

					<form:errors path="roles"
						cssClass="invalid-feedback d-block fw-semibold fs-6" />
				</div>
			</spring:bind>


			<!-- Verticals (dropdown with checkboxes) -->
			<!-- Verticals (dropdown with checkboxes; selected shown inside trigger) -->
			<spring:bind path="verticalIds">
				<div class="mb-3">
					<label class="form-label">Verticals</label>

					<div class="dropdown d-inline-block w-100">
						<button
							class="btn btn-outline-secondary dropdown-toggle text-left w-100"
							type="button" id="verticalsDropdownBtn" data-toggle="dropdown"
							aria-haspopup="true" aria-expanded="false"
							style="white-space: normal; min-height: 38px;">
							<span id="verticalsSummary" class="text-muted">Select
								verticals</span>
						</button>

						<div class="dropdown-menu p-2 w-100"
							aria-labelledby="verticalsDropdownBtn" id="verticalsMenu"
							style="max-height: 300px; overflow: auto;">
							<c:forEach var="v" items="${verticals}">
								<div class="form-check">
									<input class="form-check-input vertical-checkbox"
										type="checkbox" name="verticalIds" value="${v.id}"
										id="vert_${v.id}"
										<c:if test="${not empty form.verticalIds and form.verticalIds.contains(v.id)}">checked</c:if>>
									<label class="form-check-label" for="vert_${v.id}"> <c:out
											value="${v.name}" />
									</label>
								</div>
							</c:forEach>
						</div>
					</div>

					<form:errors path="verticalIds"
						cssClass="invalid-feedback d-block fw-semibold fs-6" />
				</div>
			</spring:bind>


			<!-- Locations (dropdown with checkboxes) -->
			<!-- Locations (dropdown with checkboxes; selected shown inside trigger) -->
			<spring:bind path="locations">
				<div class="mb-3">
					<label class="form-label">Locations</label>

					<div class="dropdown d-inline-block w-100">
						<button
							class="btn btn-outline-secondary dropdown-toggle text-left w-100"
							type="button" id="locationsDropdownBtn" data-toggle="dropdown"
							aria-haspopup="true" aria-expanded="false"
							style="white-space: normal; min-height: 38px;">
							<span id="locationsSummary" class="text-muted">Select
								locations</span>
						</button>

						<div class="dropdown-menu p-2 w-100"
							aria-labelledby="locationsDropdownBtn" id="locationsMenu"
							style="max-height: 220px; overflow: auto;">
							<c:forEach var="loc" items="${locations}">
								<div class="form-check">
									<input class="form-check-input location-checkbox"
										type="checkbox" name="locations" value="${loc}"
										id="loc_${loc}"
										<c:if test="${not empty form.locations and form.locations.contains(loc)}">checked</c:if>>
									<label class="form-check-label" for="loc_${loc}"> <c:out
											value="${loc.label()}" />
									</label>
								</div>
							</c:forEach>
						</div>
					</div>

					<form:errors path="locations"
						cssClass="invalid-feedback d-block fw-semibold fs-6" />
				</div>
			</spring:bind>



			<!-- Enabled (edit only) -->
			<c:if test="${mode eq 'edit'}">
				<div class="form-check form-switch mb-3">
					<input class="form-check-input" type="checkbox" id="enabled"
						name="enabled" <c:if test="${form.enabled}">checked</c:if>>
					<label class="form-check-label" for="enabled">Enabled</label>
				</div>
			</c:if>


			<div class="d-flex">
				<button class="btn btn-primary mr-2" type="submit">
					<c:out value="${mode eq 'edit' ? 'Update' : 'Create'}" />
				</button>
				<a class="btn btn-secondary" href="<c:url value='/admin/users'/>">Cancel</a>
			</div>
		</form:form>
	</div>
</div>

<!-- Scripts specific to this form (BS4) -->
<script>
	(function() {
		function keepOpen(id) {
			var el = document.getElementById(id);
			if (el)
				el.addEventListener('click', function(e) {
					e.stopPropagation();
				});
		}

		// Build "comma, comma" text inside the button (matches your order form style)
		function makeDropdown(config) {
			// config: {menuId, summaryId, checkboxClass, placeholder}
			keepOpen(config.menuId);

			function updateSummary() {
				var boxes = Array.prototype.slice.call(document
						.getElementsByClassName(config.checkboxClass));
				var selected = boxes.filter(function(cb) {
					return cb.checked;
				}).map(
						function(cb) {
							var lab = document.querySelector('label[for="'
									+ cb.id + '"]');
							return lab ? lab.textContent.trim() : cb.value;
						});

				var summary = document.getElementById(config.summaryId);
				if (!summary)
					return;

				if (selected.length === 0) {
					summary.textContent = config.placeholder;
					summary.classList.add('text-muted');
				} else {
					summary.textContent = selected.join(', ');
					summary.classList.remove('text-muted');
				}
			}

			document.addEventListener('change', function(e) {
				if (e.target
						&& e.target.classList.contains(config.checkboxClass)) {
					updateSummary();
				}
			});

			// initial (handles edit mode pre-checked values)
			if (document.readyState === 'loading') {
				document.addEventListener('DOMContentLoaded', updateSummary);
			} else {
				updateSummary();
				setTimeout(updateSummary, 0);
			}
		}

		// Wire all three
		makeDropdown({
			menuId : 'rolesMenu',
			summaryId : 'rolesSummary',
			checkboxClass : 'role-checkbox',
			placeholder : 'Select roles'
		});
		makeDropdown({
			menuId : 'verticalsMenu',
			summaryId : 'verticalsSummary',
			checkboxClass : 'vertical-checkbox',
			placeholder : 'Select verticals'
		});
		makeDropdown({
			menuId : 'locationsMenu',
			summaryId : 'locationsSummary',
			checkboxClass : 'location-checkbox',
			placeholder : 'Select locations'
		});
	})();
</script>

