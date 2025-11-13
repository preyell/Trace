<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<jsp:include page="/WEB-INF/jsp/orders/_table.jsp"/>

<c:set var="current" value="${page.number}" />
<c:set var="totalPages" value="${page.totalPages}" />

<c:if test="${totalPages > 0}">
  <nav class="px-3 pb-3 pt-2">
    <ul class="pagination pagination-sm mb-0">
      <!-- Prev -->
      <li class="page-item ${page.first ? 'disabled' : ''}">
        <c:url var="prevUrl" value="/orders">
          <c:param name="page" value="${current - 1}" />
          <c:param name="size" value="${empty param.size ? 10 : param.size}" />
          <c:if test="${selectedLoc != null}">
            <c:param name="loc" value="${selectedLoc.name()}" />
          </c:if>
          <c:if test="${not empty q}">
            <c:param name="q" value="${q}" />
          </c:if>
        </c:url>
        <a class="page-link" href="${prevUrl}">Previous</a>
      </li>

      <!-- Windowed pages -->
      <c:set var="begin" value="${current - 2}" />
      <c:if test="${begin < 0}"><c:set var="begin" value="0"/></c:if>
      <c:set var="endIdx" value="${begin + 4}" />
      <c:if test="${endIdx > totalPages - 1}">
        <c:set var="endIdx" value="${totalPages - 1}" />
        <c:set var="begin" value="${endIdx - 4}" />
        <c:if test="${begin < 0}"><c:set var="begin" value="0"/></c:if>
      </c:if>

      <c:forEach var="i" begin="${begin}" end="${endIdx}">
        <c:url var="pUrl" value="/orders">
          <c:param name="page" value="${i}" />
          <c:param name="size" value="${empty param.size ? 10 : param.size}" />
          <c:if test="${selectedLoc != null}">
            <c:param name="loc" value="${selectedLoc.name()}" />
          </c:if>
          <c:if test="${not empty q}">
            <c:param name="q" value="${q}" />
          </c:if>
        </c:url>
        <li class="page-item ${i == current ? 'active' : ''}">
          <a class="page-link" href="${pUrl}">${i + 1}</a>
        </li>
      </c:forEach>

      <!-- Next -->
      <c:url var="nextUrl" value="/orders">
        <c:param name="page" value="${current + 1}" />
        <c:param name="size" value="${empty param.size ? 10 : param.size}" />
        <c:if test="${selectedLoc != null}">
          <c:param name="loc" value="${selectedLoc.name()}" />
        </c:if>
        <c:if test="${not empty q}">
          <c:param name="q" value="${q}" />
        </c:if>
      </c:url>
      <li class="page-item ${page.last ? 'disabled' : ''}">
        <a class="page-link" href="${nextUrl}">Next</a>
      </li>
    </ul>
  </nav>
</c:if>
