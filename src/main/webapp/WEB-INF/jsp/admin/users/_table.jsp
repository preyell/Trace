<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<c:choose>
	<c:when test="${page.totalElements == 0}">
		<div class="p-3 text-muted">No users found.</div>
	</c:when>
	<c:otherwise>
		<c:set var="start" value="${page.number * page.size + 1}" />
		<c:set var="end"
			value="${page.number * page.size + page.numberOfElements}" />
		<div class="px-3 pt-3 small text-muted">Showing ${start}-${end}
			of ${page.totalElements}</div>

		<table class="table mb-0">
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
						<td><c:out value="${u.username}" /></td>
						<td><c:out value="${u.firstName}" /> <c:out
								value="${u.lastName}" /></td>
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
										<c:out value="${loc.label()}" /></span>
										<c:if test="${!ls.last}">,</c:if>
									</c:forEach>
								</c:otherwise>
							</c:choose></td>

						<td><span
							class="badge ${u.enabled ? 'bg-primary' : 'bg-secondary'}">
								${u.enabled ? 'Yes' : 'No'} </span></td>
						<td><a class="btn btn-sm btn-outline-primary"
							href="${pageContext.request.contextPath}/admin/users/${u.id}/edit">Edit</a>

							<form method="post" class="d-inline"
								action="${pageContext.request.contextPath}/admin/users/${u.id}/delete"
								onsubmit="return confirm('Delete this user?');">
								<input type="hidden" name="${_csrf.parameterName}"
									value="${_csrf.token}" />
								<button class="btn btn-sm btn-outline-danger" type="submit">Delete</button>
							</form> <c:if test="${!u.enabled}">
								<form method="post" class="d-inline"
									action="${pageContext.request.contextPath}/admin/users/${u.id}/resend-activation">
									<input type="hidden" name="${_csrf.parameterName}"
										value="${_csrf.token}" />
									<button class="btn btn-sm btn-outline-secondary" type="submit">Resend
										Activation</button>
								</form>
							</c:if></td>
					</tr>
				</c:forEach>
			</tbody>
		</table>

		<!-- Pagination -->
		<c:set var="current" value="${page.number}" />
		<c:set var="totalPages" value="${page.totalPages}" />
		<nav class="px-3 pb-3 pt-2">
			<ul class="pagination pagination-sm mb-0">
				<!-- Prev -->
				<li class="page-item ${page.first ? 'disabled' : ''}"><c:url
						var="prevUrl" value="/admin/users">
						<c:param name="q" value="${empty param.q ? '' : param.q}" />
						<c:param name="size" value="${empty param.size ? 10 : param.size}" />
						<c:param name="page" value="${current - 1}" />
					</c:url> <a class="page-link js-page" data-page="${current - 1}"
					href="${prevUrl}">Previous</a></li>

				<!-- Windowed page numbers (±2) -->
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
					<li class="page-item ${i == current ? 'active' : ''}"><a
						class="page-link js-page" data-page="${i}" href="${pUrl}">${i + 1}</a>
					</li>
				</c:forEach>

				<!-- Next -->
				<c:url var="nextUrl" value="/admin/users">
					<c:param name="q" value="${empty param.q ? '' : param.q}" />
					<c:param name="size" value="${empty param.size ? 10 : param.size}" />
					<c:param name="page" value="${current + 1}" />
				</c:url>
				<li class="page-item ${page.last ? 'disabled' : ''}"><a
					class="page-link js-page" data-page="${current + 1}"
					href="${nextUrl}">Next</a></li>
			</ul>
		</nav>
	</c:otherwise>
</c:choose>
</html>