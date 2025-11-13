<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
  <title>Activate your account</title>

  <!-- Remove this line if Bootstrap is already included globally -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"/>

  <style>
    .auth-card { max-width: 460px; }
  </style>
</head>
<body class="bg-light">
  <div class="container py-5">
    <div class="row justify-content-center">
      <div class="col-12 col-sm-10 col-md-8 col-lg-5">
        <div class="card shadow-sm auth-card mx-auto">
          <div class="card-body p-4">
            <h1 class="h4 mb-3">Set your password</h1>

            <c:if test="${not empty error}">
              <div class="alert alert-danger fw-semibold" role="alert">${error}</div>
            </c:if>

            <form method="post" action="${pageContext.request.contextPath}/activate" novalidate>
              <input type="hidden" name="token" value="${token}"/>
              <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

              <div class="mb-3">
                <label for="password" class="form-label">New password</label>
                <div class="input-group">
                  <input type="password" class="form-control" id="password" name="password"
                         required minlength="8" aria-describedby="pwdHelp">
                  <button class="btn btn-outline-secondary" type="button" id="togglePwd">Show</button>
                </div>
                <div id="pwdHelp" class="form-text">Minimum 8 characters.</div>
              </div>

              <div class="mb-3">
                <label for="confirm" class="form-label">Confirm password</label>
                <div class="input-group">
                  <input type="password" class="form-control" id="confirm" name="confirm"
                         required minlength="8">
                  <button class="btn btn-outline-secondary" type="button" id="toggleConfirm">Show</button>
                </div>
                <div id="matchMsg" class="text-danger fw-semibold mt-1 d-none">
                  Passwords do not match
                </div>
              </div>

              <button class="btn btn-primary w-100" type="submit">Activate</button>
            </form>
          </div>
        </div>

        <p class="text-center text-muted mt-3 mb-0">
          Already activated? <a href="${pageContext.request.contextPath}/login">Sign in</a>
        </p>
      </div>
    </div>
  </div>

  <script>
    // Show/Hide toggles
    const toggle = (inputId, btnId) => {
      const i = document.getElementById(inputId);
      const b = document.getElementById(btnId);
      b.addEventListener('click', () => {
        const t = i.type === 'password' ? 'text' : 'password';
        i.type = t;
        b.textContent = t === 'password' ? 'Show' : 'Hide';
      });
    };
    toggle('password','togglePwd');
    toggle('confirm','toggleConfirm');

    // Instant "match" feedback (client-side nicety; server still validates)
    const pwd = document.getElementById('password');
    const cfm = document.getElementById('confirm');
    const msg = document.getElementById('matchMsg');
    const check = () => {
      if (!cfm.value || !pwd.value) { msg.classList.add('d-none'); return; }
      const ok = pwd.value === cfm.value;
      msg.classList.toggle('d-none', ok);
    };
    pwd.addetener?.remove; // noop guard
    pwd.addEventListener('input', check);
    cfm.addEventListener('input', check);
  </script>
</body>
</html>
