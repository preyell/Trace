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

      <!-- Notification bell + dropdown -->
      <div class="dropdown mr-3">
        <button id="notifBell"
                class="btn btn-link position-relative p-0"
                type="button"
                data-toggle="dropdown"
                aria-haspopup="true"
                aria-expanded="false"
                style="font-size: 1.25rem;">
          <i class="fa fa-bell"></i>
          <span id="notifBadge"
                class="badge badge-danger navbar-badge"
                style="display:none; position:absolute; top:-4px; right:-4px;">
            0
          </span>
        </button>

        <div class="dropdown-menu dropdown-menu-right p-2"
             aria-labelledby="notifBell"
             style="width: 360px; max-height: 400px; overflow-y: auto;"
             id="notifPanel">
          <div class="d-flex justify-content-between align-items-center mb-1">
            <strong class="small">Notifications</strong>
            <small id="notifCountLabel" class="text-muted small"></small>
          </div>
          <div id="notifList" class="small">
            <div class="text-muted">Loading...</div>
          </div>
          <div class="dropdown-divider"></div>
          <div class="d-flex justify-content-between align-items-center">
            <a href="${pageContext.request.contextPath}/notifications"
               class="small">View all</a>

            <!-- mark-all form (used only for CSRF token; JS does the POST) -->
            <form id="notifMarkAllForm" class="m-0 p-0">
              <input type="hidden"
                     name="${_csrf.parameterName}"
                     value="${_csrf.token}"/>
              <button type="button"
                      id="notifMarkAllBtn"
                      class="btn btn-sm btn-light small">
                Mark all as read
              </button>
            </form>
          </div>
        </div>
      </div>
      <!--Notification bell -->

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

  <!-- summary line -->
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

<!-- Existing JS for search + pagination -->
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

<!-- Notification bell JS -->
<script>
(function () {
  var ctx = '${pageContext.request.contextPath}';

  // API endpoints
  var UNREAD_URL   = ctx + '/notifications/api/unread-count';
  var RECENT_URL   = ctx + '/notifications/api/recent';
  var MARK_ALL_URL = ctx + '/notifications/api/mark-all-read';

  function updateBadge(badge, count) {
    if (!badge) return;
    if (!count || count <= 0) {
      badge.style.display = 'none';
    } else {
      badge.textContent = count;
      badge.style.display = 'inline-block';
    }
  }

  function formatDate(iso) {
    if (!iso) return '';
    var d = new Date(iso);
    if (isNaN(d.getTime())) return '';
    return d.toLocaleString(undefined, {
      year:   'numeric',
      month:  '2-digit',
      day:    '2-digit',
      hour:   '2-digit',
      minute: '2-digit'
    });
  }

  function renderList(listBox, countLabel, badge, items) {
    if (!listBox) return;

    if (!items || items.length === 0) {
      listBox.innerHTML = '<div class="text-muted">No notifications.</div>';
      if (countLabel) countLabel.textContent = '';
      updateBadge(badge, 0);
      return;
    }

    var unread = 0;
    var html = '';

    items.forEach(function (n) {
      if (!n.readFlag) unread++;
      var readClass = n.readFlag ? '' : 'font-weight-bold';

      html += '<div class="mb-2 ' + readClass + '">';
      html +=   '<div>' + (n.title || '') + '</div>';

      if (n.message) {
        html += '<div class="small text-muted">' + n.message + '</div>';
      }
      if (n.createdAt) {
        html += '<div class="small text-muted">' + formatDate(n.createdAt) + '</div>';
      }
      if (n.targetUrl) {
        html += '<div class="small"><a href="' + ctx + n.targetUrl + '">Open</a></div>';
      }

      html += '</div><hr class="my-1"/>';
    });

    listBox.innerHTML = html;
    if (countLabel) {
      countLabel.textContent = unread > 0 ? (unread + ' unread') : 'All caught up';
    }
    updateBadge(badge, unread);
  }

  function bindNotificationUI() {
    var badge      = document.getElementById('notifBadge');
    var listBox    = document.getElementById('notifList');
    var countLabel = document.getElementById('notifCountLabel');
    var markAllBtn = document.getElementById('notifMarkAllBtn');
    var csrfInput  = document.querySelector('#notifMarkAllForm input[name="${_csrf.parameterName}"]');
    var csrfToken  = csrfInput ? csrfInput.value : '';

    if (!listBox) {
      console.warn('[NOTIF] listBox not found, skipping wiring.');
      return;
    }

    function fetchUnreadCount() {
      fetch(UNREAD_URL, { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
        .then(function (r) {
          if (!r.ok) return 0;
          return r.json();
        })
        .then(function (data) {
          // Supports either plain number or {count: n}
          var c = (typeof data === 'number') ? data :
                  (typeof data.count === 'number' ? data.count : 0);
          updateBadge(badge, c);
        })
        .catch(function () {
          // ignore errors
        });
    }

    function fetchRecentNotifications() {
      listBox.innerHTML = '<div class="text-muted">Loading...</div>';

      fetch(RECENT_URL, { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
        .then(function (r) { return r.ok ? r.json() : []; })
        .then(function (data) {
          var items = Array.isArray(data) ? data : (data.items || []);
          renderList(listBox, countLabel, badge, items);
        })
        .catch(function () {
          listBox.innerHTML = '<div class="text-danger">Failed to load notifications.</div>';
        });
    }

    function markAllRead() {
      if (!MARK_ALL_URL || !csrfToken || !markAllBtn) return;

      markAllBtn.disabled = true;

      fetch(MARK_ALL_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Requested-With': 'XMLHttpRequest',
          'X-CSRF-TOKEN': csrfToken
        },
        body: '{}'
      })
      .then(function (r) {
        if (!r.ok) throw new Error('Failed');
        return r.json().catch(function () { return {}; });
      })
      .then(function () {
        fetchUnreadCount();
        fetchRecentNotifications();
      })
      .catch(function () {
        alert('Failed to mark notifications as read.');
      })
      .finally(function () {
        markAllBtn.disabled = false;
      });
    }

    // Wire bell click AFTER DOM is ready
    if (window.jQuery) {
      jQuery('#notifBell')
        .off('click.notif')
        .on('click.notif', function () {
          fetchRecentNotifications();
        });
    } else {
      var bellBtn = document.getElementById('notifBell');
      if (bellBtn) {
        bellBtn.addEventListener('click', fetchRecentNotifications);
      }
    }

    if (markAllBtn) {
      markAllBtn.addEventListener('click', function (e) {
        e.preventDefault();
        markAllRead();
      });
    }

    // Initial badge + periodic refresh
    fetchUnreadCount();
    setInterval(fetchUnreadCount, 20000);
  }

  // Make sure DOM is ready before wiring
  if (window.jQuery) {
    jQuery(function () {
      bindNotificationUI();
    });
  } else {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', bindNotificationUI);
    } else {
      bindNotificationUI();
    }
  }

})();
</script>
