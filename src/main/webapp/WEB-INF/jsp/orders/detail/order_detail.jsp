<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<style>
  /* Compact details bar styles */
  .details-bar .small { font-size: 0.75rem; }
  .details-bar strong { font-size: 0.85rem; }
  .details-bar { font-size: 0.85rem; }

  .nav-tabs .nav-link { padding: .5rem 1rem; }
  .nav-tabs .nav-link.active { font-weight: 600; }
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

<!-- COMPACT SINGLE-LINE ORDER DETAILS -->
<div class="card card-outline card-primary mb-2">
  <div class="card-body py-2 px-3 d-flex flex-wrap align-items-center details-bar">

    <div class="mr-4 mb-1">
      <span class="text-muted small">Customer:</span>
      <strong><c:out value="${order.customer.name}"/></strong>
    </div>

    <div class="mr-4 mb-1">
      <span class="text-muted small">Sales Manager:</span>
      <strong>
        <c:out value="${order.salesManager.firstName}"/> 
        <c:out value="${order.salesManager.lastName}"/>
      </strong>
    </div>

    <div class="mr-4 mb-1">
      <span class="text-muted small">Location:</span>
      <strong>
        <span class="badge badge-info">
          <c:out value="${order.location.label()}"/>
        </span>
      </strong>
    </div>

    <div class="mr-4 mb-1">
      <span class="text-muted small">Created:</span>
      <strong><fmt:formatDate value="${order.createdAtDate}" pattern="yyyy-MM-dd HH:mm"/></strong>
    </div>

    <div class="mr-4 mb-1 flex-grow-1">
      <span class="text-muted small">Description:</span>
      <span class="text-muted"><c:out value="${order.description}"/></span>
    </div>

    <div class="ml-auto">
      <c:url var="backUrl" value="/orders">
        <c:if test="${not empty param.fromPage}"><c:param name="page" value="${param.fromPage}"/></c:if>
        <c:if test="${not empty param.fromSize}"><c:param name="size" value="${param.fromSize}"/></c:if>
        <c:if test="${not empty param.q}"><c:param name="q" value="${param.q}"/></c:if>
        <c:if test="${not empty param.loc}"><c:param name="loc" value="${param.loc}"/></c:if>
      </c:url>

      <a class="btn btn-sm btn-secondary mr-1" href="${backUrl}">
        <i class="fa fa-arrow-left"></i>
      </a>

      <a class="btn btn-sm btn-primary" href="${pageContext.request.contextPath}/orders/${order.id}/edit">
        <i class="fa fa-edit"></i>
      </a>
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
