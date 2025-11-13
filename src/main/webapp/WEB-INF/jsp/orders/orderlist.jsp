<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<div class="card">
  <div class="card-header d-flex align-items-center w-100">
    <h3 class="card-title mb-0">Order Summary</h3>

    <c:if test="${not empty message}">
      <div class="alert alert-primary alert-dismissible fade show m-3">
        ${message}
        <button type="button" class="close" data-dismiss="alert">&times;</button>
      </div>
    </c:if>

    <div class="ml-auto d-flex align-items-center">
      <!-- Search -->
      <div class="mr-2 d-inline-block">
        <div class="input-group input-group-sm" style="width: 280px;">
          <input id="ordersSearch" type="text" name="q" value="${fn:escapeXml(q)}"
                 class="form-control" placeholder="Search orders..."/>

        </div>
      </div>

      <!-- Location filter (only if user has >1 location) -->
      <c:if test="${allowedLocations != null and allowedLocations.size() > 1}">
        <div class="mr-2 d-inline-block">
          <div class="input-group input-group-sm" style="width: 260px;">
            <div class="input-group-prepend">
              <span class="input-group-text">Location</span>
            </div>
            <select id="ordersLoc" class="form-control">
              <option value="">All (allowed)</option>
              <c:forEach var="l" items="${allowedLocations}">
                <option value="${l.name()}" ${selectedLoc == l ? 'selected' : ''}>
                  <c:out value="${l.label()}"/>
                </option>
              </c:forEach>
            </select>
          </div>
        </div>
      </c:if>

      <a href="${pageContext.request.contextPath}/orders/new" class="btn btn-primary btn-sm">
        <i class="fa fa-plus mr-1"></i> Create Order
      </a>
    </div>
  </div>

  <!-- summary line (optional to keep here; it's static text) -->
  <c:set var="start" value="${page.number * page.size + 1}" />
  <c:set var="end"   value="${page.number * page.size + page.numberOfElements}" />
  <div class="px-3 pt-3 small text-muted" id="ordersSummary">
    <c:choose>
      <c:when test="${page.totalElements > 0}">
        Showing ${start}-${end} of ${page.totalElements}
        <c:if test="${not empty q}"> for "<c:out value="${q}"/>"</c:if>
        <c:if test="${selectedLoc != null}"> in <strong><c:out value="${selectedLoc.label()}"/></strong></c:if>
      </c:when>
      <c:otherwise>
        No orders found
        <c:if test="${not empty q}"> for "<c:out value="${q}"/>"</c:if>
        <c:if test="${selectedLoc != null}"> in <strong><c:out value="${selectedLoc.label()}"/></strong></c:if>.
      </c:otherwise>
    </c:choose>
  </div>

  <!-- Content fragment (table + pagination) -->
  <div id="ordersList">
    <jsp:include page="/WEB-INF/jsp/orders/_listContent.jsp"/>
  </div>
</div>

<!-- jQuery (already on layout via AdminLTE v3 stack); if not, include it here -->
<script>
window.addEventListener('load', function(){
  var $ = window.jQuery;
  if (!$) { console.error('jQuery not found'); return; }

  // debounce helper
  function debounce(fn, ms){ let t; return function(){ clearTimeout(t); t=setTimeout(()=>fn.apply(this, arguments), ms); }; }

  // Build query for fragment
  function currentQuery(page) {
    var q   = $('#ordersSearch').val() || '';
    var loc = $('#ordersLoc').length ? $('#ordersLoc').val() : '';
    var size = ${page.size};
    var p = (typeof page === 'number') ? page : ${page.number};
    var params = new URLSearchParams();
    params.set('page', p);
    params.set('size', size);
    if (q)   params.set('q', q);
    if (loc) params.set('loc', loc);
    return params.toString();
  }

  function loadFragment(page) {
    var url = '${pageContext.request.contextPath}/orders/fragment?' + currentQuery(page);
    $('#ordersList').load(url, function(response, status, xhr){
      if (status === 'error') {
        console.error('Fragment load failed:', xhr.status, xhr.statusText);
        return;
      }
      wirePagination();
      // (Optional) update summary via a dedicated summary fragment if you add one
    });
  }

  function wirePagination(){
    $('#ordersList').find('a.page-link').off('click').on('click', function(e){
      var href = $(this).attr('href'); if (!href) return;
      e.preventDefault();
      var u = new URL(href, window.location.origin);
      var pageParam = u.searchParams.get('page');
      var p = pageParam ? parseInt(pageParam, 10) : 0;
      loadFragment(p);
    });
  }

  // Handlers
  $('#ordersSearch').on('keyup', debounce(function(){ loadFragment(0); }, 300));
  $('#ordersClearBtn').on('click', function(){ $('#ordersSearch').val(''); loadFragment(0); });
  $('#ordersLoc').on('change', function(){ loadFragment(0); });

  // First bind for initial pagination
  wirePagination();
});
</script>
