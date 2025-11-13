<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>
<!DOCTYPE html>
<html lang="en">
<head>
<title>Two-Step Verification</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
	rel="stylesheet">
</head>
<body class="bg-light">
	<div class="container d-flex align-items-center justify-content-center"
		style="min-height: 100vh;">
		<div class="card shadow-sm" style="max-width: 420px; width: 100%;">
			<div class="card-body p-4">
				<h1 class="h4 mb-1 text-center">Enter verification code</h1>
				<p class="text-secondary text-center mb-4">
					<c:if test="${not empty username}">
						<strong>${username}</strong> - </c:if>
					We have sent a 6-digit code. Please enter it below.
				</p>

				<c:if test="${param.error == 'true'}">
					<div class="alert alert-danger">Invalid or expired code. Try
						again.</div>
				</c:if>

				<form method="post" action="${pageContext.request.contextPath}/otp">
					<div class="mb-3">
						<label for="code" class="form-label">One-time code</label> <input
							id="code" name="code" maxlength="6" pattern="[0-9]{6}"
							class="form-control" required autofocus
							title="Enter the 6-digit code (numbers only)" />
						<div class="form-text">6 digits, valid for 5 minutes.</div>
					</div>
					<!-- ✅ CSRF token -->
					<input type="hidden" name="${_csrf.parameterName}"
						value="${_csrf.token}" />

					<button class="btn btn-primary w-100" type="submit">Verify</button>
				</form>

				<p class="text-center text-secondary mt-3 mb-0"
					style="font-size: .9rem;">
					Entered the wrong account? <a
						href="${pageContext.request.contextPath}/login">Back to login</a>
				</p>
			</div>
		</div>
	</div>
</body>
</html>
