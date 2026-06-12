<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<c:if test="${not empty successMessage}">
    <div class="alert alert-success alert-dismissible fade show mx-3 mt-3" role="alert">
        <c:out value="${successMessage}"/>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    </div>
</c:if>

<c:if test="${not empty warningMessage}">
    <div class="alert alert-warning alert-dismissible fade show mx-3 mt-3" role="alert">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        <c:out value="${warningMessage}"/>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    </div>
</c:if>

<c:choose>
	<c:when test="${page.totalElements == 0}">
		<div class="p-3 text-muted">No users found.</div>
	</c:when>
	<c:otherwise>
		<c:set var="start" value="${page.number * page.size + 1}" />
		<c:set var="end" value="${page.number * page.size + page.numberOfElements}" />
		<div class="px-3 pt-3 small text-muted">Showing ${start}-${end} of ${page.totalElements}</div>

		<table class="table mb-0 align-middle">
			<thead>
				<tr>
					<th style="width: 60px;">#</th>
					<th>Username</th>
					<th>Name</th>
					<th>Email</th>
					<th>Roles</th>
					<th>Locations</th>
					<th style="width: 110px;">Enabled</th>
					<th style="width: 240px;">Actions</th>
				</tr>
			</thead>
			<tbody>
				<c:forEach var="u" items="${page.content}" varStatus="st">
					<tr>
						<td>${st.index + 1 + (page.number * page.size)}</td>
						<td><strong><c:out value="${u.username}" /></strong></td>
						<td><c:out value="${u.firstName}" /> <c:out value="${u.lastName}" /></td>
						<td><c:out value="${u.email}" /></td>
						<td><c:forEach var="r" items="${u.roles}" varStatus="rs">
								<c:out value="${r.label()}" />
								<c:if test="${!rs.last}">, </c:if>
							</c:forEach></td>
						<td><c:choose>
								<c:when test="${empty u.locations}">
									<span class="text-muted">-</span>
								</c:when>
								<c:otherwise>
									<c:forEach var="loc" items="${u.locations}" varStatus="ls">
										<c:out value="${loc.label()}" />
										<c:if test="${!ls.last}">,</c:if>
									</c:forEach>
								</c:otherwise>
							</c:choose></td>

						<td><span class="badge ${u.enabled ? 'bg-primary' : 'bg-secondary'}">
								${u.enabled ? 'Yes' : 'No'} </span></td>
						<td>
                            <a class="btn btn-sm btn-outline-primary me-1" href="${pageContext.request.contextPath}/admin/users/${u.id}/edit">Edit</a>

                            <button type="button" class="btn btn-sm btn-outline-danger" 
                                    data-bs-toggle="modal" 
                                    data-bs-target="#deleteModal" 
                                    data-userid="${u.id}" 
                                    data-username="${u.username}">
                                Delete
                            </button>
                        </td>
					</tr>
				</c:forEach>
			</tbody>
		</table>

		<c:set var="current" value="${page.number}" />
		<c:set var="totalPages" value="${page.totalPages}" />
		<nav class="px-3 pb-3 pt-2">
			<ul class="pagination pagination-sm mb-0">
				<li class="page-item ${page.first ? 'disabled' : ''}"><c:url var="prevUrl" value="/admin/users">
						<c:param name="q" value="${empty param.q ? '' : param.q}" />
						<c:param name="size" value="${empty param.size ? 10 : param.size}" />
						<c:param name="page" value="${current - 1}" />
					</c:url> <a class="page-link js-page" data-page="${current - 1}" href="${prevUrl}">Previous</a></li>

				<c:set var="begin" value="${current - 2}" />
				<c:if test="${begin < 0}">
					<c:set var="begin" value="0" />
				</c:if>
				<c:set var="endIdx" value="${begin + 4}" />
				<c:if test="${endIdx > totalPages - 1}">
					<c:set var="endIdx" value="${totalPages - 1}" />
					<c:set var="begin" value="${endIdx - 4}" />
					<c:if test="${begin < 0}">
						<c:set var="begin" value="0" />
					</c:if>
				</c:if>

				<c:forEach var="i" begin="${begin}" end="${endIdx}">
					<c:url var="pUrl" value="/admin/users">
						<c:param name="q" value="${empty param.q ? '' : param.q}" />
						<c:param name="size" value="${empty param.size ? 10 : param.size}" />
						<c:param name="page" value="${i}" />
					</c:url>
					<li class="page-item ${i == current ? 'active' : ''}"><a class="page-link js-page" data-page="${i}" href="${pUrl}">${i + 1}</a>
					</li>
				</c:forEach>

				<c:url var="nextUrl" value="/admin/users">
					<c:param name="q" value="${empty param.q ? '' : param.q}" />
					<c:param name="size" value="${empty param.size ? 10 : param.size}" />
					<c:param name="page" value="${current + 1}" />
				</c:url>
				<li class="page-item ${page.last ? 'disabled' : ''}"><a class="page-link js-page" data-page="${current + 1}" href="${nextUrl}">Next</a></li>
			</ul>
		</nav>
	</c:otherwise>
</c:choose>

<div class="modal fade" id="deleteModal" tabindex="-1" aria-labelledby="deleteModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="deleteModalLabel">Confirm Action</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        Are you sure you want to permanently delete user account <strong id="modalTargetUser"></strong>? 
        <div class="text-muted small mt-2">Note: If this account has registered historical transactions in Trace, deletion will automatically fall back to disabling the profile safely instead.</div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-sm btn-secondary" data-bs-dismiss="modal">Cancel</button>
        <form id="modalDeleteForm" method="post" action="">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <button class="btn btn-sm btn-danger" type="submit">Proceed</button>
        </form>
      </div>
    </div>
  </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    document.addEventListener("DOMContentLoaded", function() {
        const deleteModal = document.getElementById('deleteModal');
        if (deleteModal) {
            deleteModal.addEventListener('show.bs.modal', function (event) {
                // Button that triggered the modal box window frame
                const button = event.relatedTarget;
                
                // Extract contextual variables from the button data attributes
                const userId = button.getAttribute('data-userid');
                const username = button.getAttribute('data-username');
                
                // Dynamically update components inside the modal frame
                const modalUserSpan = deleteModal.querySelector('#modalTargetUser');
                const modalForm = deleteModal.querySelector('#modalDeleteForm');
                
                modalUserSpan.textContent = username;
                modalForm.action = "${pageContext.request.contextPath}/admin/users/" + userId + "/delete";
            });
        }
    });
</script>
</html>