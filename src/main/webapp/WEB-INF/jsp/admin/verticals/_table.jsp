<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>

<c:choose>
  <c:when test="${page.totalElements == 0}">
    <div class="p-3 text-muted">No verticals found.</div>
  </c:when>
  <c:otherwise>
    <c:set var="start" value="${page.number * page.size + 1}" />
    <c:set var="end"   value="${page.number * page.size + page.numberOfElements}" />
    <div class="px-3 pt-3 small text-muted">Showing ${start}-${end} of ${page.totalElements}</div>

    <table class="table mb-0">
      <thead>
        <tr>
          <th style="width:60px;">#</th>
          <th>Name</th>
          <th>Description</th>
          <th style="width:120px;">Active</th>
          <th style="width:160px;">Actions</th>
        </tr>
      </thead>
      <tbody>
      <c:forEach var="v" items="${page.content}" varStatus="st">
        <tr>
          <td>${st.index + 1 + (page.number * page.size)}</td>
          <td>${v.name}</td>
          <td><c:out value="${v.description}"/></td>
          <td>
            <span class="badge ${v.active ? 'bg-primary' : 'bg-secondary'}">
              ${v.active ? 'Yes' : 'No'}
            </span>
          </td>
          <td>
            <a class="btn btn-sm btn-outline-primary"
               href="${pageContext.request.contextPath}/admin/verticals/${v.id}/edit">Edit</a>
            <form method="post" class="d-inline"
                  action="${pageContext.request.contextPath}/admin/verticals/${v.id}/delete"
                  onsubmit="return confirm('Delete this vertical?');">
              <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
              <button class="btn btn-sm btn-outline-danger" type="submit">Delete</button>
            </form>
          </td>
        </tr>
      </c:forEach>
      </tbody>
    </table>

    <!-- Pagination -->
    <c:set var="current" value="${page.number}" />
    <c:set var="totalPages" value="${page.totalPages}" />
    <nav class="px-3 pb-3 pt-2">
      <ul class="pagination pagination-sm mb-0">
        <li class="page-item ${page.first ? 'disabled' : ''}">
          <c:url var="prevUrl" value="/admin/verticals">
            <c:param name="q" value="${empty param.q ? '' : param.q}" />
            <c:param name="size" value="${empty param.size ? 10 : param.size}" />
            <c:param name="page" value="${current - 1}" />
          </c:url>
          <a class="page-link js-page" data-page="${current - 1}" href="${prevUrl}">Previous</a>
        </li>

        <c:set var="begin" value="${current - 2}" />
        <c:if test="${begin < 0}"><c:set var="begin" value="0"/></c:if>
        <c:set var="endIdx" value="${begin + 4}" />
        <c:if test="${endIdx > totalPages - 1}">
          <c:set var="endIdx" value="${totalPages - 1}" />
          <c:set var="begin" value="${endIdx - 4}" />
          <c:if test="${begin < 0}"><c:set var="begin" value="0"/></c:if>
        </c:if>

        <c:forEach var="i" begin="${begin}" end="${endIdx}">
          <c:url var="pUrl" value="/admin/verticals">
            <c:param name="q" value="${empty param.q ? '' : param.q}" />
            <c:param name="size" value="${empty param.size ? 10 : param.size}" />
            <c:param name="page" value="${i}" />
          </c:url>
          <li class="page-item ${i == current ? 'active' : ''}">
            <a class="page-link js-page" data-page="${i}" href="${pUrl}">${i + 1}</a>
          </li>
        </c:forEach>

        <c:url var="nextUrl" value="/admin/verticals">
          <c:param name="q" value="${empty param.q ? '' : param.q}" />
          <c:param name="size" value="${empty param.size ? 10 : param.size}" />
          <c:param name="page" value="${current + 1}" />
        </c:url>
        <li class="page-item ${page.last ? 'disabled' : ''}">
          <a class="page-link js-page" data-page="${current + 1}" href="${nextUrl}">Next</a>
        </li>
      </ul>
    </nav>
  </c:otherwise>
</c:choose>
