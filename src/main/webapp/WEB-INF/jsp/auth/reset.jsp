<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Reset Password · Trace</title>
  <meta name="viewport" content="width=device-width, initial-scale=1">

  <!-- Bootstrap 5 -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@6/css/all.min.css">

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
    .password-toggle { cursor: pointer; color: #6c757d; }
    .password-toggle:hover { color: #0ea5a4; }
  </style>
</head>
<body>
  <div class="login-card">
    <div class="brand">
      <h1>Trace</h1>
    </div>

    <c:if test="${not empty error}">
      <div class="alert alert-danger">${error}</div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/reset-password">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
      <input type="hidden" name="token" value="${param.token != null ? param.token : token}" />

      <!-- New password -->
      <div class="mb-3">
        <label class="form-label">New password</label>
        <div class="position-relative">
          <input id="password" name="password" type="password" class="form-control pe-5" required minlength="8"/>
          <i class="fa fa-eye password-toggle position-absolute top-50 end-0 translate-middle-y me-3" 
             id="togglePassword" aria-label="Show password"></i>
        </div>
      </div>

      <!-- Confirm password -->
      <div class="mb-3">
        <label class="form-label">Confirm password</label>
        <div class="position-relative">
          <input id="confirm" name="confirm" type="password" class="form-control pe-5" required minlength="8"/>
          <i class="fa fa-eye password-toggle position-absolute top-50 end-0 translate-middle-y me-3" 
             id="toggleConfirm" aria-label="Show password"></i>
        </div>
      </div>

      <button class="btn btn-primary w-100" type="submit">Set password</button>

      <div class="text-center mt-3">
        <a href="${pageContext.request.contextPath}/login" class="small">Back to login</a>
      </div>
    </form>
  </div>

  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
  <script>
    // Toggle for new password
    document.getElementById("togglePassword").addEventListener("click", function () {
      const pwd = document.getElementById("password");
      const type = pwd.type === "password" ? "text" : "password";
      pwd.type = type;
      this.classList.toggle("fa-eye");
      this.classList.toggle("fa-eye-slash");
    });

    // Toggle for confirm password
    document.getElementById("toggleConfirm").addEventListener("click", function () {
      const pwd = document.getElementById("confirm");
      const type = pwd.type === "password" ? "text" : "password";
      pwd.type = type;
      this.classList.toggle("fa-eye");
      this.classList.toggle("fa-eye-slash");
    });
  </script>
</body>
</html>
