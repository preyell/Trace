<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width,initial-scale=1" />
<title><c:out value="${pageTitle != null ? pageTitle : 'Trace'}" /></title>

<!-- AdminLTE + Bootstrap 5 -->
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/admin-lte@^3.2/dist/css/adminlte.min.css">
<!-- (v3 uses BS4; for BS5 use AdminLTE v4 CDN below) -->
<!-- AdminLTE v4 (Bootstrap 5) -->
<!-- <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/admin-lte@4.0.0/dist/css/adminlte.min.css"> -->

<!-- Icons (optional) -->
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@6/css/all.min.css">
</head>
<body class="hold-transition sidebar-mini layout-fixed">
	<div class="wrapper">

		<!-- Navbar -->
		<nav
			class="main-header navbar navbar-expand navbar-white navbar-light border-bottom">
			<ul class="navbar-nav">
				<li class="nav-item"><a class="nav-link" data-widget="pushmenu"
					role="button"><i class="fas fa-bars"></i></a></li>
				<li class="nav-item d-none d-sm-inline-block"><a
					href="${pageContext.request.contextPath}/orders" class="nav-link">Home</a></li>
			</ul>
			<ul class="navbar-nav ml-auto">
				<li class="nav-item"><span class="nav-link text-muted small">Hi,
						<sec:authentication property="name" />
				</span></li>
				<li class="nav-item">
					<form action="${pageContext.request.contextPath}/logout"
						method="post" class="d-inline">
						<input type="hidden" name="${_csrf.parameterName}"
							value="${_csrf.token}" />
						<button class="btn btn-outline-danger btn-sm ml-2" type="submit">Logout</button>
					</form>
				</li>
			</ul>
		</nav>

		<c:set var="ctx" value="${pageContext.request.contextPath}" />

		<c:set var="reqUri"
			value="${not empty requestScope['jakarta.servlet.forward.request_uri']
                     ? requestScope['jakarta.servlet.forward.request_uri']
                     : pageContext.request.requestURI}" />

		<c:set var="path"
			value="${fn:substring(reqUri, fn:length(ctx), fn:length(reqUri))}" />


		<!-- Sidebar -->
		<aside class="main-sidebar sidebar-dark-primary elevation-4">
			<a href="${ctx}/" class="brand-link"> <span
				class="brand-text fw-bold ml-2">Trace</span>
			</a>
			<div class="sidebar">
				<nav class="mt-2">
					<ul class="nav nav-pills nav-sidebar flex-column"
						data-widget="treeview" role="menu">

						<sec:authorize access="hasRole('ROLE_ADMIN')">

							<%-- figure out what's active inside /admin --%>
							<c:set var="isUsers"
								value="${fn:startsWith(path, '/admin/users')}" />
							<c:set var="isVerticals"
								value="${fn:startsWith(path, '/admin/verticals')}" />
							<c:set var="isCustomers"
								value="${fn:startsWith(path, '/admin/customers')}" />
							<c:set var="isExpenses"
								value="${fn:startsWith(path, '/admin/expenses')}" />
								<c:set var="isAudits"
								value="${fn:startsWith(path, '/admin/audit-log')}" />

							<c:set var="mdOpen"
								value="${isUsers or isVerticals or isCustomers or isExpenses}" />

							<!-- Master Data tree -->
							<li class="nav-item has-treeview ${mdOpen ? 'menu-open' : ''}">
								<a href="#" class="nav-link ${mdOpen ? 'active' : ''}"> <i
									class="nav-icon fas fa-database"></i>
									<p>
										Master Data <i class="right fas fa-angle-left"></i>
									</p>
							</a>

								<ul class="nav nav-treeview">
									<li class="nav-item"><a href="${ctx}/admin/users"
										class="nav-link ${isUsers ? 'active' : ''}"> <i
											class="far fa-circle nav-icon"></i>
											<p>Users</p>
									</a></li>

									<li class="nav-item"><a href="${ctx}/admin/verticals"
										class="nav-link ${isVerticals ? 'active' : ''}"> <i
											class="far fa-circle nav-icon"></i>
											<p>Verticals</p>
									</a></li>

									<li class="nav-item"><a href="${ctx}/admin/customers"
										class="nav-link ${isCustomers ? 'active' : ''}"> <i
											class="far fa-circle nav-icon"></i>
											<p>Customers</p>
									</a></li>

									<li class="nav-item"><a href="${ctx}/admin/expenses"
										class="nav-link ${isExpenses ? 'active' : ''}"> <i
											class="far fa-circle nav-icon"></i>
											<p>Additional Expense Labels</p>
									</a></li>
								</ul>
							</li>

						</sec:authorize>


						<%-- Orders and other non-admin menus --%>
						<c:set var="isOrders" value="${fn:startsWith(path, '/orders')}" />
						<c:set var="isExpensesOverview"
							value="${fn:startsWith(path, '/reports/expenses')}" />
						<c:set var="isChangePassword"
							value="${fn:startsWith(path, '/account/change-password')}" />

						<li class="nav-item"><a href="${ctx}/orders"
							class="nav-link ${isOrders ? 'active' : ''}"> <i
								class="nav-icon fas fa-file-invoice"></i>
								<p>Orders</p>
						</a></li>

						<sec:authorize
							access="hasAnyAuthority('ROLE_ADMIN','ROLE_CEO','ROLE_FINANCE_APPROVER','ROLE_FINANCE','ROLE_CFO')">
								<li class="nav-item"><a href="${ctx}/admin/audit-log"
								class="nav-link ${isAudits ? 'active' : ''}"> <i
									class="nav-icon fa fa-list"></i>
									<p>Audit Log</p>
							</a></li>
							<li class="nav-item"><a href="${ctx}/reports/expenses"
								class="nav-link ${isExpensesOverview ? 'active' : ''}"> <i
									class="nav-icon fa fa-list"></i>
									<p>Expense Overview</p>
							</a></li>
							<li class="nav-item"><a
								href="${ctx}/account/change-password"
								class="nav-link ${isChangePassword ? 'active' : ''}"> <i
									class="fa fa-key mr-1"></i> <p>Change Password</p>
							</a></li>
						</sec:authorize>


					</ul>
				</nav>
			</div>
		</aside>


		<!-- Content -->
		<div class="content-wrapper">
			<section class="content pt-3">
				<div class="container-fluid">
					<jsp:include page="${contentJsp}" />
				</div>
			</section>
		</div>
		<%@ taglib uri="jakarta.tags.fmt" prefix="fmt"%>
		<jsp:useBean id="now" class="java.util.Date" />

		<footer class="main-footer small py-2">
			<div class="text-nowrap">
				<span class="mr-2">v1.0</span> <strong>&copy; <fmt:formatDate
						value="${now}" pattern="yyyy" /> Trace.
				</strong>
			</div>
		</footer>
	</div>

	<!-- Scripts -->
	<!-- SCRIPTS: jQuery -> Bootstrap 4 bundle -> AdminLTE v3 (order matters) -->
	<script src="https://cdn.jsdelivr.net/npm/jquery@3/dist/jquery.min.js"></script>
	<script
		src="https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/js/bootstrap.bundle.min.js"></script>
	<script
		src="https://cdn.jsdelivr.net/npm/admin-lte@3.2/dist/js/adminlte.min.js"></script>
	<c:if test="${not empty scriptJsp}">
		<jsp:include page="${scriptJsp}" />
	</c:if>
	<script>
(function(){
  function refreshNotifCount() {
    fetch('${pageContext.request.contextPath}/notifications/unread-count', {
      headers: { 'X-Requested-With': 'XMLHttpRequest' }
    })
    .then(r => r.ok ? r.text() : '0')
    .then(txt => {
      var n = parseInt(txt, 10) || 0;
      var b = document.getElementById('notifBadge');
      if (!b) return;
      if (n > 0) {
        b.textContent = n;
        b.style.display = 'inline-block';
      } else {
        b.style.display = 'none';
      }
    })
    .catch(() => {});
  }
  document.addEventListener('DOMContentLoaded', refreshNotifCount);
})();
</script>
</body>
</html>
