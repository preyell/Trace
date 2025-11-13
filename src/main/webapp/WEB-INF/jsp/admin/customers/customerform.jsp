<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<c:set var="isEdit" value="${customer.id ne null}" />

<!-- Page header (match Users/Verticals) -->
<div class="d-flex justify-content-between align-items-center mb-3">
  <h5 class="mb-0">
    <c:choose>
      <c:when test="${isEdit}">Edit Customer</c:when>
      <c:otherwise>New Customer</c:otherwise>
    </c:choose>
  </h5>
  <a class="btn btn-secondary btn-sm" href="<c:url value='/admin/customers'/>">Back</a>
</div>

<!-- Build post URL -->
<c:choose>
  <c:when test="${isEdit}">
    <c:url var="postUrl" value="/admin/customers/${customer.id}" />
  </c:when>
  <c:otherwise>
    <c:url var="postUrl" value="/admin/customers" />
  </c:otherwise>
</c:choose>

<div class="card">
  <div class="card-body">
    <form:form method="post" modelAttribute="customer" action="${postUrl}">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

      <!-- Name -->
      <spring:bind path="name">
        <div class="form-group mb-3">
          <label class="form-label" for="name">Name <span class="text-danger">*</span></label>
          <form:input path="name" id="name"
                      cssClass="form-control ${status.error ? 'is-invalid' : ''}"
                      maxlength="128" required="required" />
          <form:errors path="name" cssClass="invalid-feedback d-block fw-semibold"/>
        </div>
      </spring:bind>

      <!-- Contact -->
      <spring:bind path="contactName">
        <div class="form-group mb-3">
          <label class="form-label" for="contactName">Contact</label>
          <form:input path="contactName" id="contactName"
                      cssClass="form-control ${status.error ? 'is-invalid' : ''}"
                      maxlength="128" />
          <form:errors path="contactName" cssClass="invalid-feedback d-block fw-semibold"/>
        </div>
      </spring:bind>

      <!-- Email -->
      <spring:bind path="email">
        <div class="form-group mb-3">
          <label class="form-label" for="email">Email</label>
          <form:input path="email" id="email" type="email"
                      cssClass="form-control ${status.error ? 'is-invalid' : ''}"
                      maxlength="128" />
          <form:errors path="email" cssClass="invalid-feedback d-block fw-semibold"/>
        </div>
      </spring:bind>

      <!-- Phone -->
      <spring:bind path="phone">
        <div class="form-group mb-3">
          <label class="form-label" for="phone">Phone</label>
          <form:input path="phone" id="phone"
                      cssClass="form-control ${status.error ? 'is-invalid' : ''}"
                      maxlength="32" />
          <form:errors path="phone" cssClass="invalid-feedback d-block fw-semibold"/>
        </div>
      </spring:bind>

      <!-- Active (AdminLTE v3 / BS4 switch) -->
      <spring:bind path="active">
        <div class="custom-control custom-switch mb-3">
          <form:checkbox path="active" id="active" cssClass="custom-control-input"/>
          <label class="custom-control-label" for="active">Active</label>
          <form:errors path="active" cssClass="invalid-feedback d-block fw-semibold"/>
        </div>
      </spring:bind>

      <div class="d-flex">
        <button class="btn btn-primary mr-2" type="submit">
          <c:out value="${isEdit ? 'Update' : 'Create'}"/>
        </button>
        <a class="btn btn-secondary" href="<c:url value='/admin/customers'/>">Cancel</a>
      </div>
    </form:form>
  </div>
</div>
