<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<h6 class="mb-3">
	<c:out value="${mr.vertical.name}" /> <small class="text-muted">(${mr.fileName})</small>
</h6>

<table class="table table-sm">
	<thead>
		<tr>
			<th>When</th>
			<th>Action</th>
			<th>Actor</th>
			<th>Comments</th>
			<th>Note</th>
		</tr>
	</thead>
	<tbody>
		<c:choose>
			<c:when test="${audits.totalElements == 0}">
				<tr>
					<td colspan="4" class="text-muted text-center py-4">No audit
						entries yet.</td>
				</tr>
			</c:when>
			<c:otherwise>
				<c:forEach var="a" items="${audits.content}">
					<tr>
						<!-- Convert Instant -> epoch millis for JSTL -->
						<td><c:choose>
								<c:when test="${not empty a.actedOn}">
									<!-- Build a java.util.Date from the Instant -->
									<jsp:useBean id="actedOnDate" class="java.util.Date" />
									<jsp:setProperty name="actedOnDate" property="time"
										value="${a.actedOn.toEpochMilli()}" />
									<fmt:formatDate value="${actedOnDate}"
										pattern="yyyy-MM-dd HH:mm" timeZone="Africa/Nairobi" />
								</c:when>
								<c:otherwise>-</c:otherwise>
							</c:choose></td>

						<td><c:out value="${a.action}" /></td>
						<td><c:out
								value="${a.actor != null ? a.actor.firstName : a.actorId}" /></td>
								<td><c:out value="${a.comments}" /></td>
						<td><c:out value="${a.note}" /></td>
					</tr>
				</c:forEach>
			</c:otherwise>
		</c:choose>
	</tbody>
</table>

<c:if test="${audits.totalPages > 1}">
	<nav class="d-flex justify-content-between align-items-center">
		<a class="btn btn-sm btn-light ${audits.first ? 'disabled' : ''}"
			href="?page=${audits.number - 1}&size=${audits.size}">Prev</a> <span>Page
			${audits.number + 1} of ${audits.totalPages}</span> <a
			class="btn btn-sm btn-light ${audits.last ? 'disabled' : ''}"
			href="?page=${audits.number + 1}&size=${audits.size}">Next</a>
	</nav>
</c:if>
