<%@ include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/css/select2.min.css" />
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@ttskch/select2-bootstrap4-theme@1.x.x/dist/select2-bootstrap4.min.css" />

<div class="card">
<div class="card-header d-flex flex-column flex-md-row align-items-start align-items-md-center w-100 row-gap-2">
		<h3 class="card-title mb-2 mb-md-0">Order Summary</h3>

		<c:if test="${not empty error}">
			<div class="alert alert-danger ml-0 ml-md-3 mb-2 mb-md-0">${error}</div>
		</c:if>
		<c:if test="${not empty message}">
			<div class="alert alert-primary alert-dismissible fade show m-0 ml-md-3 mb-2 mb-md-0">
				${message}
				<button type="button" class="close" data-dismiss="alert">&times;</button>
			</div>
		</c:if>

		<div class="ml-0 ml-md-auto d-flex flex-wrap align-items-center w-100 w-md-auto justify-content-between justify-content-md-end">
			<div class="dropdown mr-2 mb-1 mb-sm-0">
				<button id="notifBell" class="btn btn-link position-relative p-0"
					type="button" data-toggle="dropdown" aria-haspopup="true"
					aria-expanded="false" style="font-size: 1.25rem;">
					<i class="fa fa-bell"></i> 
					<span id="notifBadge" class="badge badge-danger navbar-badge" style="display: none; position: absolute; top: -4px; right: -4px;">0</span>
				</button>
				<div class="dropdown-menu dropdown-menu-right p-2" aria-labelledby="notifBell" style="width: 360px; max-height: 400px; overflow-y: auto;" id="notifPanel">
					<div class="d-flex justify-content-between align-items-center mb-1">
						<strong class="small">Notifications</strong> <small id="notifCountLabel" class="text-muted small"></small>
					</div>
					<div id="notifList" class="small"><div class="text-muted">Loading...</div></div>
					<div class="dropdown-divider"></div>
					<div class="d-flex justify-content-between align-items-center">
						<a href="${pageContext.request.contextPath}/notifications" class="small">View all</a>
						<form id="notifMarkAllForm" class="m-0 p-0">
							<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
							<button type="button" id="notifMarkAllBtn" class="btn btn-sm btn-light small">Mark all as read</button>
						</form>
					</div>
				</div>
			</div>

			<div class="position-relative mr-2 mb-1 mb-sm-0">
				<div class="input-group input-group-sm" style="width: 280px;">
					<input id="ordersSearch" type="text" name="q" value="${fn:escapeXml(q)}" class="form-control" placeholder="Search by Order #, Customer, Desc..." />
				</div>
			</div>

			<c:if test="${allowedLocations != null and allowedLocations.size() > 1}">
				<div class="mr-2 mb-1 mb-sm-0">
					<div class="input-group input-group-sm" style="width: 200px;">
						<div class="input-group-prepend"><span class="input-group-text">Location</span></div>
						<select id="ordersLoc" class="form-control">
							<option value="">All (allowed)</option>
							<c:forEach var="l" items="${allowedLocations}">
								<option value="${l.name()}" ${selectedLoc == l ? 'selected' : ''}><c:out value="${l.label()}" /></option>
							</c:forEach>
						</select>
					</div>
				</div>
			</c:if>

			<button class="btn btn-sm btn-primary mr-2 mb-1 mb-sm-0" type="button" data-toggle="collapse" data-target="#advancedFilterPanel" aria-expanded="false" id="btnToggleAdvanced">
				<i class="fa fa-sliders-h mr-1"></i> Advanced Filters
			</button>

			<a href="${pageContext.request.contextPath}/orders/new" class="btn btn-sm btn-primary mb-1 mb-sm-0"> 
				<i class="fa fa-plus mr-1"></i> Create Order
			</a>
		</div>
	</div>

	<div class="collapse" id="advancedFilterPanel">
		<div class="card-body bg-light border-bottom p-3">
			<form id="advFilterForm" onsubmit="return false;">
				<div class="row">
					<div class="col-md-3 form-group mb-2">
						<label class="small font-weight-bold mb-1">Customer</label>
						<select name="advCustomer" id="advCustomer" class="form-control form-control-sm select2-adv">
							<option value="">Select an Option</option>
							<c:forEach var="c" items="${customersLookup}">
								<option value="${c.id}"><c:out value="${c.name}" /></option>
							</c:forEach>
						</select>
					</div>
					<div class="col-md-3 form-group mb-2">
						<label class="small font-weight-bold mb-1">Sales Manager</label>
						<select name="advManager" id="advManager" class="form-control form-control-sm select2-adv">
							<option value="">Select an Option</option>
							<c:forEach var="sm" items="${managersLookup}">
								<option value="${sm.id}"><c:out value="${sm.firstName} ${sm.lastName}" /></option>
							</c:forEach>
						</select>
					</div>
					<div class="col-md-3 form-group mb-2">
						<label class="small font-weight-bold mb-1">Creation Start Date</label>
						<input type="date" name="advStartDate" id="advStartDate" class="form-control form-control-sm" placeholder="dd/mm/yyyy" />
					</div>
					<div class="col-md-3 form-group mb-2">
						<label class="small font-weight-bold mb-1">Creation End Date</label>
						<input type="date" name="advEndDate" id="advEndDate" class="form-control form-control-sm" placeholder="dd/mm/yyyy" />
					</div>
				</div>
				<div class="row mt-2">
					<div class="col-md-3 form-group mb-2">
						<label class="small font-weight-bold mb-1">Business Vertical</label>
						<select name="advVertical" id="advVertical" class="form-control form-control-sm select2-adv">
							<option value="">Select an Option</option>
							<c:forEach var="v" items="${verticalsLookup}">
								<option value="${v.id}"><c:out value="${v.name}" /></option>
							</c:forEach>
						</select>
					</div>
					<div class="col-md-5 form-group mb-2">
						<label class="small font-weight-bold mb-1">Description Keywords</label>
						<input type="text" name="advDesc" id="advDesc" class="form-control form-control-sm" placeholder="Search text inside descriptions..." />
					</div>
					<div class="col-md-4 form-group mb-2 d-flex align-items-end justify-content-end">
						<button type="button" id="btnAdvSubmit" class="btn btn-sm btn-primary px-3 mr-2">Apply Criteria</button>
						<button type="button" id="btnAdvReset" class="btn btn-sm btn-outline-secondary px-3">Reset Panel</button>
					</div>
				</div>
			</form>
		</div>
	</div>

	<c:set var="start" value="${page.number * page.size + 1}" />
	<c:set var="end" value="${page.number * page.size + page.numberOfElements}" />
	<div class="px-3 pt-3 small text-muted" id="ordersSummary">
		<c:choose>
			<c:when test="${page.totalElements > 0}">
        Showing ${start}-${end} of ${page.totalElements}
        <c:if test="${not empty q}"> for "<c:out value="${q}" />"</c:if>
				<c:if test="${selectedLoc != null}"> in <strong><c:out value="${selectedLoc.label()}" /></strong></c:if>
			</c:when>
			<c:otherwise>
        No orders found
        <c:if test="${not empty q}"> for "<c:out value="${q}" />"</c:if>
				<c:if test="${selectedLoc != null}"> in <strong><c:out value="${selectedLoc.label()}" /></strong></c:if>.
      </c:otherwise>
		</c:choose>
	</div>

	<div id="ordersList">
		<jsp:include page="/WEB-INF/jsp/orders/_listContent.jsp" />
	</div>
</div>

<script>
(function () {
  var ctx = '${pageContext.request.contextPath}';

  // ==========================================
  // 1. NOTIFICATION SYSTEM LOGIC
  // ==========================================
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
      year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
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
      html += '<div class="mb-2 ' + readClass + '"><div>' + (n.title || '') + '</div>';
      if (n.message) html += '<div class="small text-muted">' + n.message + '</div>';
      if (n.createdAt) html += '<div class="small text-muted">' + formatDate(n.createdAt) + '</div>';
      if (n.targetUrl) html += '<div class="small"><a href="' + ctx + '/notifications/' + n.id + '/open">Open</a></div>';
      html += '</div><hr class="my-1"/>';
    });

    listBox.innerHTML = html;
    if (countLabel) countLabel.textContent = unread > 0 ? (unread + ' unread') : 'All caught up';
    updateBadge(badge, unread);
  }

  function bindNotificationUI() {
    var badge      = document.getElementById('notifBadge');
    var listBox    = document.getElementById('notifList');
    var countLabel = document.getElementById('notifCountLabel');
    var markAllBtn = document.getElementById('notifMarkAllBtn');

    if (!listBox) return;

    function fetchUnreadCount() {
      fetch(UNREAD_URL, { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
        .then(function (r) { return r.ok ? r.json() : 0; })
        .then(function (data) {
          var c = (typeof data === 'number') ? data : (typeof data.count === 'number' ? data.count : 0);
          updateBadge(badge, c);
        }).catch(function(){});
    }

    function fetchRecentNotifications() {
      listBox.innerHTML = '<div class="text-muted">Loading...</div>';
      fetch(RECENT_URL, { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
        .then(function (r) { return r.ok ? r.json() : []; })
        .then(function (data) {
          var items = Array.isArray(data) ? data : (data.items || []);
          renderList(listBox, countLabel, badge, items);
        }).catch(function () {
          listBox.innerHTML = '<div class="text-danger">Failed to load notifications.</div>';
        });
    }

    function markAllRead() {
      if (!MARK_ALL_URL || !markAllBtn) return;
      markAllBtn.disabled = true;
      var body = new URLSearchParams();
      body.append('${_csrf.parameterName}', '${_csrf.token}');

      fetch(MARK_ALL_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
          'X-Requested-With': 'XMLHttpRequest'
        },
        body: body.toString()
      }).then(function (r) {
        if (!r.ok) throw new Error('Failed');
        fetchUnreadCount();
        fetchRecentNotifications();
      }).catch(function () {
        alert('Failed to mark notifications as read.');
      }).finally(function () {
        markAllBtn.disabled = false;
      });
    }

    if (window.jQuery) {
      window.jQuery('#notifBell').off('click.notif').on('click.notif', fetchRecentNotifications);
    } else {
      var bellBtn = document.getElementById('notifBell');
      if (bellBtn) bellBtn.addEventListener('click', fetchRecentNotifications);
    }

    if (markAllBtn) markAllBtn.addEventListener('click', function (e) { e.preventDefault(); markAllRead(); });

    fetchUnreadCount();
    setInterval(fetchUnreadCount, 20000);
  }

  // ==========================================
  // 2. UNIFIED SEARCH, PAGINATION & ADVANCED FILTERS
  // ==========================================
  window.addEventListener('load', function() {
    //  Step A: Wait dynamically until your layout template fully initializes jQuery
    function safeInitializerLoop() {
      if (typeof window.jQuery !== 'undefined') {
        var $ = window.jQuery;
        
        //  Step B: Inject Select2 JavaScript script source ONLY after jQuery is present
        if (!$.fn.select2) {
          var script = document.createElement('script');
          script.src = "https://cdn.jsdelivr.net/npm/select2@4.1.0-rc.0/dist/js/select2.min.js";
          script.onload = function() {
            initializeDashboardLogic($);
          };
          document.head.appendChild(script);
        } else {
          initializeDashboardLogic($);
        }
      } else {
        setTimeout(safeInitializerLoop, 50);
      }
    }

    function initializeDashboardLogic($) {
      bindNotificationUI();

      function debounce(fn, ms){ let t; return function(){ clearTimeout(t); t=setTimeout(()=>fn.apply(this, arguments), ms); }; }

      function currentQuery(page) {
        var size = ${page.size};
        var p = (typeof page === 'number') ? page : ${page.number};
        var params = new URLSearchParams();
        
        params.set('page', p);
        params.set('size', size);

        var q = $('#ordersSearch').val() || '';
        var loc = $('#ordersLoc').length ? $('#ordersLoc').val() : '';
        if (q)   params.set('q', q);
        if (loc) params.set('loc', loc);

        var cust = $('#advCustomer').val();
        var manager = $('#advManager').val();
        var vertical = $('#advVertical').val();
        var desc = $('#advDesc').val();
        var start = $('#advStartDate').val();
        var end = $('#advEndDate').val();

        if (cust) params.set('advCustomer', cust);
        if (manager) params.set('advManager', manager);
        if (vertical) params.set('advVertical', vertical);
        if (desc) params.set('advDesc', desc);
        if (start) params.set('advStartDate', start);
        if (end) params.set('advEndDate', end);

        return params.toString();
      }

      function loadFragment(page) {
        var url = '${pageContext.request.contextPath}/orders/fragment?' + currentQuery(page);
        $('#ordersList').load(url, function(response, status, xhr){
          if (status === 'error') return;
          wirePagination();
        });
      }

      function wirePagination(){
        $('#ordersList').find('a.page-link').off('click').on('click', function(e){
          e.preventDefault();
          var href = $(this).attr('href'); if (!href) return;
          var u = new URL(href, window.location.origin);
          var pageParam = u.searchParams.get('page');
          loadFragment(pageParam ? parseInt(pageParam, 10) : 0);
        });
      }

      $('#ordersSearch').on('keyup', debounce(function(){ loadFragment(0); }, 300));
      $('#ordersLoc').on('change', function(){ loadFragment(0); });

      $('#btnAdvSubmit').on('click', function() { loadFragment(0); });
      
      $('#btnAdvReset').on('click', function() {
          $('#advFilterForm')[0].reset();
          $('.select2-adv').val('').trigger('change');
          loadFragment(0);
      });

      //  Step C: Initialize Select2 properties safely on target elements
      $('.select2-adv').select2({
        theme: 'bootstrap4',
        width: '100%',
        allowClear: true,
        minimumResultsForSearch: 0,
        dropdownParent: $('#advancedFilterPanel')
      });

      wirePagination();
      console.log("All advanced dashboard components successfully active with search text capabilities!");
    }

    // Trigger loading verification hook
    safeInitializerLoop();
  });
})();
</script>