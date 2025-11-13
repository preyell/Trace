<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<!-- Optional page-scoped polish -->
<style>
  .order-meta .meta-item { min-width: 160px; }
  .order-meta .meta-item .label { font-size: .75rem; color: #6c757d; }
  .order-meta .meta-item .value { font-weight: 600; }
  .nav-tabs .nav-link { padding: .5rem 1rem; }
  .nav-tabs .nav-link.active { font-weight: 600; }
  .section-title { font-size: 1rem; font-weight: 600; margin-bottom: .75rem; }
</style>

<!-- Page heading / breadcrumb -->
<div class="content-header px-0">
  <div class="container-fluid px-0">
    <div class="row mb-2">
      <div class="col-sm-7">
        <h1 class="m-0 text-dark">
          Order <span class="text-primary">#<c:out value="${order.salesOrderId}"/></span>
        </h1>
      </div>
      <div class="col-sm-5">
        <ol class="breadcrumb float-sm-right mb-0">
          <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/">Home</a></li>
          <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/orders">Orders</a></li>
          <li class="breadcrumb-item active">#<c:out value="${order.salesOrderId}"/></li>
        </ol>
      </div>
    </div>
  </div>
</div>

<!-- Flash message -->
<c:if test="${not empty message}">
  <div class="alert alert-primary alert-dismissible fade show">
    <i class="fa fa-check-circle mr-1"></i> ${message}
    <button type="button" class="close" data-dismiss="alert">&times;</button>
  </div>
</c:if>

<!-- Order meta card -->
<div class="card card-outline card-primary mb-3">
  <div class="card-header d-flex align-items-center flex-wrap">
    <h3 class="card-title mb-0">
      <i class="fa fa-file-alt mr-2"></i> Details
    </h3>
    <div class="ml-auto">
      <!-- Smarter Back preserving list params if present -->
      <c:url var="backUrl" value="/orders">
        <c:if test="${not empty param.fromPage}"><c:param name="page" value="${param.fromPage}"/></c:if>
        <c:if test="${not empty param.fromSize}"><c:param name="size" value="${param.fromSize}"/></c:if>
        <c:if test="${not empty param.q}"><c:param name="q" value="${param.q}"/></c:if>
        <c:if test="${not empty param.loc}"><c:param name="loc" value="${param.loc}"/></c:if>
      </c:url>
      <a class="btn btn-sm btn-secondary" href="${backUrl}">
        <i class="fa fa-arrow-left mr-1"></i> Back to Orders
      </a>
      <a class="btn btn-sm btn-primary" href="${pageContext.request.contextPath}/orders/${order.id}/edit">
        <i class="fa fa-edit mr-1"></i> Edit
      </a>
    </div>
  </div>

  <div class="card-body">
    <div class="row order-meta">
      <div class="col-md-3 col-sm-6 mb-3 meta-item">
        <div class="label">Customer</div>
        <div class="value"><c:out value="${order.customer.name}"/></div>
      </div>
      <div class="col-md-3 col-sm-6 mb-3 meta-item">
        <div class="label">Sales Manager</div>
        <div class="value">
          <c:out value="${order.salesManager.firstName}"/> <c:out value="${order.salesManager.lastName}"/>
        </div>
      </div>
      <div class="col-md-3 col-sm-6 mb-3 meta-item">
        <div class="label">Location</div>
        <div class="value">
          <c:choose>
            <c:when test="${order.location.name() == 'KENYA'}">
              <span class="badge badge-primary"><c:out value="${order.location.label()}"/></span>
            </c:when>
            <c:when test="${order.location.name() == 'TANZANIA'}">
              <span class="badge badge-secondary"><c:out value="${order.location.label()}"/></span>
            </c:when>
            <c:otherwise>
              <span class="badge badge-secondary"><c:out value="${order.location.label()}"/></span>
            </c:otherwise>
          </c:choose>
        </div>
      </div>
      <div class="col-md-3 col-sm-6 mb-3 meta-item">
        <div class="label">Created</div>
        <div class="value"><fmt:formatDate value="${order.createdAtDate}" pattern="yyyy-MM-dd HH:mm"/></div>
      </div>
      <div class="col-12">
        <div class="label">Description</div>
        <div class="value text-muted"><c:out value="${order.description}"/></div>
      </div>
    </div>
  </div>
</div>

<!-- Tabs card -->
<div class="card">
  <div class="card-header p-0 border-bottom-0">
    <ul class="nav nav-tabs card-header-tabs" role="tablist">
      <li class="nav-item">
        <a class="nav-link ${activeTab == 'margin' ? 'active' : ''}"
           href="${pageContext.request.contextPath}/orders/${order.id}?tab=margin" role="tab">
          <i class="fa fa-chart-line mr-1"></i> Margin Report
        </a>
      </li>
      <li class="nav-item">
        <a class="nav-link ${activeTab == 'finance' ? 'active' : ''}"
           href="${pageContext.request.contextPath}/orders/${order.id}?tab=finance" role="tab">
          <i class="fa fa-coins mr-1"></i> Finance
        </a>
      </li>
    </ul>
  </div>

  <div class="card-body">
    <c:choose>
      <c:when test="${activeTab == 'finance'}">
        <jsp:include page="/WEB-INF/jsp/orders/detail/_finance.jsp"/>
      </c:when>
      <c:otherwise>
        <jsp:include page="/WEB-INF/jsp/orders/detail/_margin.jsp"/>
      </c:otherwise>
    </c:choose>
  </div>
</div>
