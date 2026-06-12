<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Login - Trace</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
    <div class="container d-flex align-items-center justify-content-center" style="min-height: 100vh;">
        <div class="card shadow-sm" style="max-width: 400px; width: 100%;">
            <div class="card-body p-4">
            
            <img src="${pageContext.request.contextPath}/images/logo.png" 
                     alt="Sybyl Trace Logo" 
                     class="d-block mx-auto mb-4 login-logo" />
                <h2 class="text-center mb-3 fw-bold text-primary">Trace</h2>
                <p class="text-center text-muted small mb-4">Enter your registered username or email address to receive a secure login code.</p>

                <c:if test="${not empty error}">
                    <div class="alert alert-danger text-center py-2 small">${error}</div>
                </c:if>

                <form method="POST" action="${pageContext.request.contextPath}/login/send-otp">
                    <div class="mb-3">
                        <label for="usernameOrEmail" class="form-label fw-medium">Username or Email</label>
                        <input type="text" id="usernameOrEmail" name="usernameOrEmail" class="form-control py-2" placeholder="name@company.com" required autofocus />
                    </div>

                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

                    <button type="submit" class="btn btn-primary w-100 py-2 fw-semibold">Send Verification Code</button>
                </form>
            </div>
        </div>
    </div>
</body>
</html>