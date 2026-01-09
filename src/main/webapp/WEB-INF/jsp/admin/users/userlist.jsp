<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
  <h5 class="mb-0">Users</h5>
  <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/admin/users/userForm">New User</a>
</div>

<c:if test="${param.created == 'true'}"><div class="alert alert-primary py-2">User created. Activation email sent.</div></c:if>
<c:if test="${param.updated == 'true'}"><div class="alert alert-primary py-2">User updated.</div></c:if>
<c:if test="${param.deleted == 'true'}"><div class="alert alert-primary py-2">User deleted.</div></c:if>
<c:if test="${param.activationResent == 'true'}"><div class="alert alert-primary py-2">Activation email re-sent.</div></c:if>

<c:if test="${not empty message}">
  <div class="alert alert-primary py-2">${message}</div>
</c:if>
<c:if test="${not empty error}">
  <div class="alert alert-danger py-2">${error}</div>
</c:if>


<!-- Search + page size (no buttons) -->
<div class="mb-3">
  <div class="input-group">
    <input id="userSearchQ" name="q" class="form-control"
           value="<c:out value='${param.q}'/>"
           placeholder="Search username, name or email"/>

    <div class="input-group-append">
      <span class="input-group-text">Rows per page</span>
    </div>
    <select id="userSearchSize" name="size" class="custom-select" style="max-width:120px;">
      <option value="10" <c:if test="${param.size == '10' || empty param.size}">selected</c:if>>10</option>
      <option value="25" <c:if test="${param.size == '25'}">selected</c:if>>25</option>
      <option value="50" <c:if test="${param.size == '50'}">selected</c:if>>50</option>
    </select>
  </div>
</div>

<div class="card">
  <div class="card-body p-0">
    <div id="userTableWrap">
      <jsp:include page="_table.jsp" />
    </div>
  </div>
</div>

<!-- Live search (waits for jQuery from AdminLTE v3) -->
<script>
(function(w, d){
  function onReady(fn){ d.readyState !== 'loading' ? fn() : d.addEventListener('DOMContentLoaded', fn); }
  function waitForJQ(cb){ w.jQuery ? cb(w.jQuery) : setTimeout(function(){ waitForJQ(cb); }, 50); }

  onReady(function(){
    waitForJQ(function($){
      var ctx   = '${pageContext.request.contextPath}';
      var $q    = $('#userSearchQ');
      var $size = $('#userSearchSize');
      var $wrap = $('#userTableWrap');
      var xhr   = null, debounceTimer = null;

      function stripScripts(html){
        var tmp = d.createElement('div');
        tmp.innerHTML = html;
        tmp.querySelectorAll('script').forEach(function(s){ s.parentNode.removeChild(s); });
        return tmp.innerHTML;
      }

      function load(page){
        var params = $.param({
          q:    $q.val() || '',
          size: $size.val() || 10,
          page: (typeof page === 'number' ? page : 0)
        });
        if (xhr) xhr.abort();
        $wrap.css('opacity', .6);
        xhr = $.get(ctx + '/admin/users/table?' + params)
          .done(function(html){
            $wrap.html(stripScripts(html));
            if (history.replaceState) history.replaceState(null, '', ctx + '/admin/users?' + params);
          })
          .always(function(){ $wrap.css('opacity', 1); });
      }

      // Type-to-search (debounced)
      $q.on('input', function(){ clearTimeout(debounceTimer); debounceTimer = setTimeout(function(){ load(0); }, 250); });

      // Auto-reload when size changes
      $size.on('change', function(){ load(0); });

      // AJAX paginate
      $wrap.on('click', 'a.js-page', function(e){
        if (e.ctrlKey || e.metaKey) return;
        e.preventDefault();
        var p = parseInt($(this).data('page'));
        if (!isNaN(p)) load(p);
      });
    });
  });
})(window, document);
</script>
