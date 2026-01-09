<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<table class="table mb-0">
	<thead>
		<tr>
			<th style="width: 160px;">Sales Order ID</th>
			<!-- renamed -->
			<th>Customer</th>
			<th>Description</th>
			<th>Sales Manager</th>
			<th>Status</th>
			<th>Verticals</th>
			<th>Location</th>
			<th>Created By</th>
			<th style="width: 180px;">Created At</th>
			<th style="width: 170px;">Actions</th>
		</tr>
	</thead>
	<tbody>
		<c:choose>
			<c:when test="${page.totalElements == 0}">
				<tr>
					<td colspan="9" class="text-muted text-center py-4">No orders
						to display.</td>
				</tr>
			</c:when>
			<c:otherwise>
				<c:forEach var="o" items="${page.content}">
					<tr>
						<!-- Show Sales Order ID, not numeric id -->
						<td><c:url var="detailUrl" value="/orders/${o.id}">
								<!-- Optional: preserve list filters when you navigate back -->
								<c:if test="${not empty param.page}">
									<c:param name="fromPage" value="${param.page}" />
								</c:if>
								<c:if test="${not empty param.size}">
									<c:param name="fromSize" value="${param.size}" />
								</c:if>
								<c:if test="${not empty q}">
									<c:param name="q" value="${q}" />
								</c:if>
								<c:if test="${selectedLoc != null}">
									<c:param name="loc" value="${selectedLoc.name()}" />
								</c:if>
							</c:url> <a href="${detailUrl}" class="font-weight-bold">${o.salesOrderId}</a>
						</td>


						<td><c:out value="${o.customer.name}" /></td>
						<td><c:out value="${o.description}" /></td>

						<td><c:out value="${o.salesManager.firstName}" /> <c:out
								value=" " /> <c:out value="${o.salesManager.lastName}" /></td>
						<td><c:choose>
								<c:when
									test="${o.statusCode == 'MR_APPROVED_NO_EXPENSE' 
                           or o.statusCode == 'EXP_CFO_APPROVED'}">
									<span class="badge badge-success"> <c:out
											value="${o.statusLabel}" />
									</span>
								</c:when>

								<c:when
									test="${o.statusCode == 'MR_FINANCE_PENDING'
                           or o.statusCode == 'MR_CEO_PENDING'
                           or o.statusCode == 'EXP_WAITING'
                           or o.statusCode == 'EXP_CEO_APPROVED'}">
									<span class="badge badge-warning"> <c:out
											value="${o.statusLabel}" />
									</span>
								</c:when>

								<c:when
									test="${o.statusCode == 'MR_REJECTED'
                           or o.statusCode == 'EXP_REJECTED'}">
									<span class="badge badge-danger"> <c:out
											value="${o.statusLabel}" />
									</span>
								</c:when>

								<c:otherwise>
									<span class="badge badge-secondary"> <c:out
											value="${o.statusLabel}" />
									</span>
								</c:otherwise>
							</c:choose></td>
						<td><c:forEach var="v" items="${o.verticals}" varStatus="vs">
								<c:out value="${v.name}" />
								<c:if test="${!vs.last}">, </c:if>
							</c:forEach></td>

						<td><c:choose>
								<c:when test="${o.location.name() == 'KENYA'}">
									<span class="badge badge-primary"><c:out
											value="${o.location.label()}" /></span>
								</c:when>
								<c:when test="${o.location.name() == 'TANZANIA'}">
									<span class="badge badge-secondary"><c:out
											value="${o.location.label()}" /></span>
								</c:when>
								<c:otherwise>
									<span class="badge bg-secondary"><c:out
											value="${o.location.label()}" /></span>
								</c:otherwise>
							</c:choose></td>

						<td><c:out value="${o.createdBy.firstName}" /> <c:out
								value=" " /> <c:out value="${o.createdBy.lastName}" /></td>

						<td><fmt:formatDate value="${o.createdAtDate}"
								pattern="yyyy-MM-dd HH:mm" /></td>

						<td>
							<!-- Still use internal numeric id in URLs --> <a
							class="btn btn-sm btn-outline-primary"
							href="${pageContext.request.contextPath}/orders/${o.id}/edit">Edit</a>

							<form method="post" class="d-inline"
								action="${pageContext.request.contextPath}/orders/${o.id}/delete"
								onsubmit="return confirm('Delete this order?');">
								<input type="hidden" name="${_csrf.parameterName}"
									value="${_csrf.token}" />
								<button class="btn btn-sm btn-outline-danger" type="submit">Delete</button>
							</form>
						</td>
					</tr>
				</c:forEach>
			</c:otherwise>
		</c:choose>
	</tbody>
</table>

