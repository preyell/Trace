<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<div class="card">
	<div class="card-body">
		<form method="post"
			action="<c:out value='${mode eq "edit"
                     ? pageContext.request.contextPath.concat("/admin/verticals/").concat(vertical.id)
                     : pageContext.request.contextPath.concat("/admin/verticals")}'/>">

			<input type="hidden" name="${_csrf.parameterName}"
				value="${_csrf.token}" />

			<div class="mb-3">
				<label class="form-label">Name</label> <input name="name"
					class="form-control" value="<c:out value='${vertical.name}'/>"
					required maxlength="100" />
				<c:if
					test="${not empty requestScope['org.springframework.validation.BindingResult.vertical']}">
					<c:set var="br"
						value="${requestScope['org.springframework.validation.BindingResult.vertical']}" />
					<c:forEach var="fe" items="${br.fieldErrors}">
						<c:if test="${fe.field eq 'name'}">
							<small class="text-danger"><c:out
									value="${fe.defaultMessage}" /></small>
						</c:if>
					</c:forEach>
				</c:if>
			</div>

			<div class="mb-3">
				<label class="form-label">Description</label>
				<textarea name="description" class="form-control" rows="3"
					maxlength="500"><c:out value="${vertical.description}" /></textarea>
			</div>

			<div class="form-check form-switch mb-3">
				<!-- Spring checkbox marker: if unchecked, binder will set active=false -->
				<input type="hidden" name="_active" value="on" /> <input
					class="form-check-input" type="checkbox" id="active" name="active"
					value="true" <c:if test="${vertical.active}">checked</c:if> /> <label
					class="form-check-label" for="active">Active</label>
			</div>

			<div class="d-flex gap-2">
				<button class="btn btn-primary" type="submit">
					<c:out value='${mode eq "edit" ? "Update" : "Create"}' />
				</button>
				<a class="btn btn-secondary"
					href="${pageContext.request.contextPath}/admin/verticals">Cancel</a>
			</div>

		</form>
	</div>
</div>
