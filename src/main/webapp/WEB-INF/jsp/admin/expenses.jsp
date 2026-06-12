<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<div class="card card-outline card-primary">
	<div class="card-header d-flex align-items-center">
		<h3 class="card-title mb-0">
			<i class="fa fa-tags mr-2"></i> Additional Expense Labels
		</h3>
	</div>
	<c:if test="${not empty message}">
		<div class="alert alert-primary py-2">${message}</div>
	</c:if>
	<c:if test="${not empty error}">
		<div class="alert alert-danger py-2">${error}</div>
	</c:if>

	<div class="card-body">
		<form class="form-inline mb-3" method="post"
			action="${pageContext.request.contextPath}/admin/expenses/create">
			<input type="hidden" name="${_csrf.parameterName}"
				value="${_csrf.token}" /> <input class="form-control mr-2"
				name="name" placeholder="New label name" required /> <input
				class="form-control mr-2" name="description"
				placeholder="Description (optional)" />
			<button class="btn btn-primary">Add</button>
		</form>

		<table class="table table-bordered">
			<thead>
				<tr>
					<th>Name</th>
					<th>Description</th>
					<th>System</th>
					<th>Active</th>
					<th class="text-right">Actions</th>
				</tr>
			</thead>
			<tbody>
				<c:forEach var="l" items="${labels}">
					<tr>
						<td><c:out value="${l.name}" /></td>
						<td><c:out value="${l.description}" /></td>
						<td><span
							class="badge ${l.system ? 'badge-secondary' : 'badge-light'}">
								${l.system ? 'Yes' : 'No'} </span></td>
						<td><span
							class="badge ${l.active ? 'badge-primary' : 'badge-secondary'}">
								${l.active ? 'Active' : 'Inactive'} </span></td>
						<td class="text-right"><c:choose>
								<c:when test="${l.active}">
									<form method="post"
										action="${pageContext.request.contextPath}/admin/expenses/${l.id}/deactivate"
										class="d-inline">
										<input type="hidden" name="${_csrf.parameterName}"
											value="${_csrf.token}" />
										<button class="btn btn-sm btn-outline-secondary"
											${l.system ? 'disabled' : ''}>Deactivate</button>
									</form>
								</c:when>
								<c:otherwise>
									<form method="post"
										action="${pageContext.request.contextPath}/admin/expenses/${l.id}/reactivate"
										class="d-inline">
										<input type="hidden" name="${_csrf.parameterName}"
											value="${_csrf.token}" />
										<button class="btn btn-sm btn-outline-primary">Reactivate</button>
									</form>
								</c:otherwise>
							</c:choose>
							<button type="button" class="btn btn-sm btn-outline-info" data-toggle="modal" data-target="#editModal${l.id}">
    Edit
</button>
							<form method="post"
								action="${pageContext.request.contextPath}/admin/expenses/${l.id}/delete"
								class="d-inline"
								onsubmit="return confirm('Delete this label?');">
								<input type="hidden" name="${_csrf.parameterName}"
									value="${_csrf.token}" />
								<button class="btn btn-sm btn-outline-danger"
									${l.system ? 'disabled' : ''}>Delete</button>
							</form></td>
					</tr>
					
					<div class="modal fade text-left" id="editModal${l.id}" tabindex="-1" role="dialog" aria-labelledby="editModalLabel${l.id}" aria-hidden="true">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <form method="post" action="${pageContext.request.contextPath}/admin/expenses/${l.id}/edit">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                
                <div class="modal-header">
                    <h5 class="modal-title" id="editModalLabel${l.id}">Edit Label</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                
                <div class="modal-body">
                    <div class="form-group">
                        <label>Name</label>
                        <input type="text" class="form-control" name="name" value="${l.name}" required ${l.system ? 'readonly' : ''} />
                        <c:if test="${l.system}">
                            <small class="text-danger">System labels cannot be renamed.</small>
                        </c:if>
                    </div>
                    <div class="form-group mt-3">
                        <label>Description</label>
                        <input type="text" class="form-control" name="description" value="${l.description}" />
                    </div>
                </div>
                
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary">Save Changes</button>
                </div>
            </form>
        </div>
    </div>
</div>
				</c:forEach>
			</tbody>
		</table>
	</div>
</div>
