<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<div class="card">
  <div class="card-header d-flex align-items-center">
    <h3 class="card-title mb-0">Notifications</h3>

    <form method="get" action="${pageContext.request.contextPath}/notifications"
          class="form-inline ml-auto">
      <label class="mr-2 small text-muted">Show:</label>
      <select name="filter" class="form-control form-control-sm mr-2"
              onchange="this.form.submit()">
        <option value="all"    ${filter == 'all' ? 'selected' : ''}>All</option>
        <option value="unread" ${filter == 'unread' ? 'selected' : ''}>Unread only</option>
      </select>
      <input type="hidden" name="page" value="${page}" />
      <input type="hidden" name="size" value="${size}" />
    </form>
  </div>

  <div class="card-body p-2">
    <c:choose>
      <c:when test="${empty notifications}">
        <div class="text-muted">No notifications.</div>
      </c:when>
      <c:otherwise>
        <ul class="list-unstyled mb-0">
          <c:forEach var="n" items="${notifications}">
            <li class="mb-2 ${n.readFlag ? '' : 'font-weight-bold'}">
              <div>${fn:escapeXml(n.title)}</div>

              <c:if test="${not empty n.message}">
                <div class="small text-muted">
                  ${fn:escapeXml(n.message)}
                </div>
              </c:if>

              <c:if test="${not empty n.createdAtDate}">
                <div class="small text-muted">
                  <fmt:formatDate value="${n.createdAtDate}" pattern="yyyy-MM-dd HH:mm"/>
                </div>
              </c:if>

              <c:if test="${not empty n.targetUrl}">
                <div class="small">
                  <a href="${pageContext.request.contextPath}${n.targetUrl}">Open</a>
                </div>
              </c:if>
            </li>
          </c:forEach>
        </ul>
      </c:otherwise>
    </c:choose>
  </div>

  <c:if test="${totalPages > 1}">
    <div class="card-footer py-2">
      <nav aria-label="Notification pages">
        <ul class="pagination pagination-sm mb-0">

          <c:set var="prevPage" value="${page - 1}" />
          <c:set var="nextPage" value="${page + 1}" />

          <li class="page-item ${page == 0 ? 'disabled' : ''}">
            <a class="page-link"
               href="${pageContext.request.contextPath}/notifications?page=${prevPage}&size=${size}&filter=${filter}">
              &laquo;
            </a>
          </li>

          <c:forEach var="i" begin="0" end="${totalPages - 1}">
            <li class="page-item ${i == page ? 'active' : ''}">
              <a class="page-link"
                 href="${pageContext.request.contextPath}/notifications?page=${i}&size=${size}&filter=${filter}">
                ${i + 1}
              </a>
            </li>
          </c:forEach>

          <li class="page-item ${page + 1 >= totalPages ? 'disabled' : ''}">
            <a class="page-link"
               href="${pageContext.request.contextPath}/notifications?page=${nextPage}&size=${size}&filter=${filter}">
              &raquo;
            </a>
          </li>
        </ul>
      </nav>
    </div>
  </c:if>
</div>
