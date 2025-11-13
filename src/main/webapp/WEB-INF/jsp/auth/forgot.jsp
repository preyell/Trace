<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Forgot Password · Trace</title>
  <meta name="viewport" content="width=device-width, initial-scale=1">

  <!-- Bootstrap 5 -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">

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
      box-shadow: 0 12px 40px rgba(0,0,0,.25);
    }
    .brand { text-align: center; margin-bottom: 24px; }
    .brand h1 { font-size: 1.5rem; font-weight: 700; color: #155e75; margin: 0; }
    .brand p { font-size: .95rem; color: #64748b; margin: 0; }
    .alert { font-size: .9rem; padding: .5rem .75rem; }
  </style>
</head>
<body>
  <div class="login-card">
    <div class="brand">
      <h1>Trace</h1>
    </div>

    <c:if test="${not empty message}">
      <div class="alert alert-primary">${message}</div>
    </c:if>
    <c:if test="${not empty error}">
      <div class="alert alert-danger">${error}</div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/forgot-password">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

      <div class="mb-3">
        <label class="form-label">Email</label>
        <input name="email" type="email" class="form-control" required />
      </div>

      <button class="btn btn-primary w-100" type="submit">Send reset link</button>

      <div class="text-center mt-3">
        <a href="${pageContext.request.contextPath}/login" class="small">Back to login</a>
      </div>
    </form>
  </div>

  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
