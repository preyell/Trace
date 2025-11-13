<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<!DOCTYPE html>
<html lang="en">
<head>
<title>Login · Trace</title>
<meta name="viewport" content="width=device-width, initial-scale=1">

<!-- Bootstrap 5 -->
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
	rel="stylesheet">
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@6/css/all.min.css">

<style>
body {
	min-height: 100vh;
	margin: 0;
	font-family: "Segoe UI", sans-serif;
	display: flex;
	align-items: center;
	justify-content: center;
}

.login-card {
	width: 100%;
	max-width: 420px;
	background: #fff;
	border-radius: 16px;
	padding: 32px 28px;
	box-shadow: 0 12px 40px rgba(0, 0, 0, .25);
}

.brand {
	text-align: center;
	margin-bottom: 24px;
}

.brand h1 {
	font-size: 1.5rem;
	font-weight: 700;
	color: #155e75;
	margin: 0;
}

.brand p {
	font-size: .95rem;
	color: #64748b;
	margin: 0;
}

.alert {
	font-size: .9rem;
	padding: .5rem .75rem;
}
</style>
</head>
<body>
	<div class="login-card">
		<div class="brand">
			<h1>Trace</h1>
		</div>

		<!-- Messages -->
		<c:if test="${param.error == 'true'}">
			<div class="alert alert-danger">Invalid username or password.</div>
		</c:if>
		<c:if test="${param.logout == 'true'}">
			<div class="alert alert-primary">You have been logged out.</div>
		</c:if>
		<c:if test="${param.reset == 'success'}">
			<div class="alert alert-primary">Password updated. Please sign
				in.</div>
		</c:if>

		<!-- Login form -->
		<form method="post" action="${pageContext.request.contextPath}/login">
			<div class="mb-3">
				<label for="username" class="form-label">Username</label> <input
					id="username" name="username" class="form-control" required
					autofocus />
			</div>
			<div class="mb-3">
				<label for="password" class="form-label">Password</label>
				<div class="position-relative">
					<input id="password" type="password" name="password"
						class="form-control pe-5" required /> <i id="togglePassword"
						class="fa fa-eye password-toggle position-absolute top-50 end-0 translate-middle-y me-3"
						aria-label="Show password" tabindex="0"></i>
				</div>
			</div>

			<!-- CSRF -->
			<input type="hidden" name="${_csrf.parameterName}"
				value="${_csrf.token}" />

			<button class="btn btn-primary w-100" type="submit">Sign in</button>

			<div class="text-center mt-3">
				<a href="${pageContext.request.contextPath}/forgot-password"
					class="small">Forgot password?</a>
			</div>

		</form>
	</div>

	<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
	<script>
		document
				.addEventListener(
						"DOMContentLoaded",
						function() {
							const togglePassword = document
									.querySelector("#togglePassword");
							const passwordField = document
									.querySelector("#password");

							togglePassword
									.addEventListener(
											"click",
											function() {
												const type = passwordField
														.getAttribute("type") === "password" ? "text"
														: "password";
												passwordField.setAttribute(
														"type", type);

												// switch icon
												this.classList.toggle("fa-eye");
												this.classList
														.toggle("fa-eye-slash");
											});
						});
	</script>


</body>
</html>
