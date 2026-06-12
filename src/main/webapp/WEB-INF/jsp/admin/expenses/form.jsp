<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<div class="card">
	<div class="card-body">
		<form method="post" action="<c:out value='${mode eq "edit" ? pageContext.request.contextPath.concat("/admin/expenses/").concat(label.id) : pageContext.request.contextPath.concat("/admin/expenses")}'/>">
			<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

			<div class="mb-3">
				<label class="form-label">Name</label> 
                <input name="name" class="form-control" value="<c:out value='${label.name}'/>" required maxlength="100" ${label.system ? 'readonly' : ''} />
                <c:if test="${label.system}">
                    <small class="text-danger">System labels cannot be renamed.</small>
                </c:if>
			</div>

			<div class="mb-3">
				<label class="form-label">Description</label>
				<textarea name="description" class="form-control" rows="3" maxlength="500"><c:out value="${label.description}" /></textarea>
			</div>

            <c:choose>
                <c:when test="${not label.system}">
                    <div class="form-check form-switch mb-3">
                        <input type="hidden" name="_active" value="on" /> 
                        <input class="form-check-input" type="checkbox" id="active" name="active" value="true" <c:if test="${label.active || mode eq 'create'}">checked</c:if> /> 
                        <label class="form-check-label" for="active">Active</label>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="mb-3">
                        <span class="badge badge-secondary">System defaults are always active.</span>
                    </div>
                </c:otherwise>
            </c:choose>

			<div class="d-flex gap-2">
				<button class="btn btn-primary" type="submit">
					<c:out value='${mode eq "edit" ? "Update" : "Create"}' />
				</button>
				<a class="btn btn-secondary" href="${pageContext.request.contextPath}/admin/expenses">Cancel</a>
			</div>
		</form>
	</div>
</div>