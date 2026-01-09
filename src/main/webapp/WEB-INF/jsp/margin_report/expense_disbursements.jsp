<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<table class="table table-bordered mb-0">
	<thead class="thead-light">
		<tr>
			<th>Amount</th>
			<th>Amount (USD)</th>
			<th>Consumed On</th>
			<th>Consumed By</th>
			<th>Comments</th>
			<th style="width: 1%;">Actions</th>
		</tr>
	</thead>
	<tbody>
		<c:choose>
			<c:when test="${empty disbursements}">
				<tr>
					<td colspan="6" class="text-muted text-center">No
						consumptions.</td>
				</tr>
			</c:when>
			<c:otherwise>
				<c:forEach var="d" items="${disbursements}">
					<tr data-amount="${d.amountUsd}">

						<td><fmt:formatNumber value="${d.amount}" type="number"
								minFractionDigits="2" maxFractionDigits="2" /> <span
							class="text-muted">/ ${d.currency}</span></td>
						<td><fmt:formatNumber value="${d.amountUsd}" type="number"
								minFractionDigits="2" maxFractionDigits="2" /></td>
						<td><fmt:formatDate value="${d.disbursedOnDate}"
									pattern="yyyy-MM-dd HH:mm" timeZone="Africa/Kampala" /></td>
						<td><c:out
								value="${d.actor != null ? (d.actor.firstName.concat(' ').concat(d.actor.lastName)) : '-'}" />
						</td>
						<td><c:out value="${d.note}" /></td>
						<td class="text-right">
							<button type="button"
								class="btn btn-sm btn-outline-danger js-disb-del"
								data-disbid="${d.id}">
								<i class="fa fa-trash"></i>
							</button>
						</td>

					</tr>
				</c:forEach>
			</c:otherwise>
		</c:choose>
	</tbody>
</table>
