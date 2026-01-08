<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<div class="d-flex justify-content-between align-items-center mb-3">
	<div class="section-title mb-0">
		<i class="fa fa-list-alt mr-1"></i> Application Audit Log
	</div>

	<!-- Search form -->
	<form class="form-inline" method="get">
		<div class="input-group input-group-sm">
			<input type="text" class="form-control" name="q"
				placeholder="Search..." value="${fn:escapeXml(param.q)}" />
			<div class="input-group-append">
				<button class="btn btn-outline-secondary" type="submit">
					<i class="fa fa-search"></i>
				</button>
			</div>
		</div>
		<!-- keep page size on search -->
		<input type="hidden" name="size" value="${size != null ? size : 20}" />
	</form>
</div>

<div class="table-responsive">
	<table class="table table-sm table-hover mb-2">
		<thead class="thead-light">
			<tr>
				<th style="white-space: nowrap;">Time</th>
				<th>User</th>
				<th>Action / Target</th>
				<th>Message</th>
				<th style="white-space: nowrap;">IP</th>
			</tr>
		</thead>
		<tbody>
			<c:forEach var="e" items="${logs}">
				<tr>
					<!-- Time -->
					<td style="white-space: nowrap; font-size: 0.85rem;"><fmt:formatDate
							value="${e.eventTimeDate}" pattern="yyyy-MM-dd HH:mm:ss"
							timeZone="Africa/Kampala" /></td>

					<!-- User -->
					<td style="font-size: 0.85rem;"><strong>${e.actorDisplayName}</strong><br />
						<span class="text-muted">@${e.actorUsername}</span></td>

					<!-- Action + Target -->
					<td style="font-size: 0.85rem; white-space: nowrap;">
						<!-- Action as badge --> <span
						class="badge badge-pill
  <c:choose>
    <c:when test="${e.action == 'DELETE' || e.action == 'DISBURSE_DELETE'}">badge-danger</c:when>
    <c:when test="${e.action == 'CREATE' || e.action == 'DISBURSE'}">badge-success</c:when>
    <c:when test="${e.action == 'UPDATE'}">badge-info</c:when>
    <c:when test="${fn:endsWith(e.action, '_APPROVE')}">badge-primary</c:when>
    <c:when test="${e.action == 'REJECT'}">badge-warning</c:when>
    <c:otherwise>badge-secondary</c:otherwise>
  </c:choose>">
							${e.action} </span> <br /> <span class="text-muted">
							${e.entityType}#${e.entityId} <c:if test="${not empty e.salesOrderId}">
                &nbsp;on Order&nbsp;<strong>#${e.salesOrderId}</strong>
							</c:if>
					</span>
					</td>

					<!-- Message -->
					<td style="font-size: 0.85rem; max-width: 400px;"><span
						title="${e.message}"> ${e.message} </span></td>

					<!-- IP -->
					<td style="white-space: nowrap; font-size: 0.8rem;"><span
						class="text-monospace text-muted">${e.actorIp}</span></td>
				</tr>
			</c:forEach>

			<c:if test="${empty logs}">
				<tr>
					<td colspan="5" class="text-center text-muted">No audit
						entries found.</td>
				</tr>
			</c:if>
		</tbody>
	</table>
</div>

<!-- Pagination controls -->
<c:if test="${totalPages gt 1}">
	<nav aria-label="Audit log pagination">
		<ul class="pagination pagination-sm mb-0">

			<!-- Previous -->
			<li class="page-item ${page <= 0 ? 'disabled' : ''}"><a
				class="page-link"
				href="<c:url value=''>
                   <c:param name='page' value='${page - 1}'/>
                   <c:param name='size' value='${size}'/>
                   <c:if test='${not empty param.q}'>
                     <c:param name='q' value='${param.q}'/>
                   </c:if>
                 </c:url>"
				tabindex="-1">Previous</a></li>

			<!-- Current page + neighbors (simple window) -->
			<c:set var="start" value="${page - 2}" />
			<c:set var="end" value="${page + 2}" />
			<c:if test="${start < 0}">
				<c:set var="start" value="0" />
			</c:if>
			<c:if test="${end >= totalPages}">
				<c:set var="end" value='${totalPages - 1}' />
			</c:if>

			<c:forEach var="p" begin="${start}" end="${end}">
				<li class="page-item ${p == page ? 'active' : ''}"><a
					class="page-link"
					href="<c:url value=''>
                     <c:param name='page' value='${p}'/>
                     <c:param name='size' value='${size}'/>
                     <c:if test='${not empty param.q}'>
                       <c:param name='q' value='${param.q}'/>
                     </c:if>
                   </c:url>">
						${p + 1} </a></li>
			</c:forEach>

			<!-- Next -->
			<li class="page-item ${page + 1 >= totalPages ? 'disabled' : ''}">
				<a class="page-link"
				href="<c:url value=''>
                   <c:param name='page' value='${page + 1}'/>
                   <c:param name='size' value='${size}'/>
                   <c:if test='${not empty param.q}'>
                     <c:param name='q' value='${param.q}'/>
                   </c:if>
                 </c:url>">
					Next </a>
			</li>
		</ul>
	</nav>
	<small class="text-muted"> Page ${page + 1} of ${totalPages} </small>
</c:if>
