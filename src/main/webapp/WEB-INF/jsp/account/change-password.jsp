<%@ page contentType="text/html;charset=UTF-8"%>
<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

<div class="card">
	<div class="card-header">
		<h3 class="card-title mb-0">Change Password</h3>
	</div>

	<div class="card-body">

		<c:if test="${not empty message}">
			<div class="alert alert-success alert-dismissible fade show">
				${message}
				<button type="button" class="close" data-dismiss="alert">&times;</button>
			</div>
		</c:if>

		<form:form method="post" modelAttribute="form"
			action="${pageContext.request.contextPath}/account/change-password">

			<input type="hidden" name="${_csrf.parameterName}"
				value="${_csrf.token}" />

			<!-- Current password -->
			<div class="form-group">
				<label for="currentPassword">Current Password</label>

				<div class="row">
					<div class="col-md-6 col-lg-5">
						<div class="input-group">
							<form:password path="currentPassword" cssClass="form-control"
								id="currentPassword" />
							<div class="input-group-append">
								<button class="btn btn-outline-secondary toggle-password"
									type="button" data-target="currentPassword">
									<i class="fa fa-eye"></i>
								</button>
							</div>
						</div>

						<form:errors path="currentPassword" cssClass="text-danger small" />
					</div>
				</div>
			</div>

			<!-- New password -->
			<div class="form-group">
				<label for="newPassword">New Password</label>

				<div class="row">
					<div class="col-md-6 col-lg-5">
						<div class="input-group">
							<form:password path="newPassword" cssClass="form-control"
								id="newPassword" />
							<div class="input-group-append">
								<button class="btn btn-outline-secondary toggle-password"
									type="button" data-target="newPassword">
									<i class="fa fa-eye"></i>
								</button>
							</div>
						</div>

						<form:errors path="newPassword" cssClass="text-danger small" />
					</div>

				</div>
			</div>
			<!-- Confirm password -->
			<div class="form-group">
				<label for="confirmPassword">Confirm New Password</label>

				<div class="row">
					<div class="col-md-6 col-lg-5">
						<div class="input-group">
							<form:password path="confirmPassword" cssClass="form-control"
								id="confirmPassword" />
							<div class="input-group-append">
								<button class="btn btn-outline-secondary toggle-password"
									type="button" data-target="confirmPassword">
									<i class="fa fa-eye"></i>
								</button>
							</div>
						</div>

						<form:errors path="confirmPassword" cssClass="text-danger small" />
					</div>

				</div>
			</div>
			<button type="submit" class="btn btn-primary">
				<i class="fa fa-key mr-1"></i> Change Password
			</button>

		</form:form>
	</div>
</div>


<script>
	document.addEventListener("DOMContentLoaded", function() {
		document.querySelectorAll(".toggle-password").forEach(function(btn) {
			btn.addEventListener("click", function() {
				var inputId = this.getAttribute("data-target");
				var input = document.getElementById(inputId);
				var icon = this.querySelector("i");

				if (input.type === "password") {
					input.type = "text";
					icon.classList.remove("fa-eye");
					icon.classList.add("fa-eye-slash");
				} else {
					input.type = "password";
					icon.classList.remove("fa-eye-slash");
					icon.classList.add("fa-eye");
				}
			});
		});
	});
</script>

