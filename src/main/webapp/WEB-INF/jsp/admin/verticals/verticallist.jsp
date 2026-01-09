<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
  <h5 class="mb-0">Verticals</h5>
  <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/admin/verticals/new">
    New Vertical
  </a>
</div>

<c:if test="${not empty message}">
  <div class="alert alert-primary py-2">${message}</div>
</c:if>

<c:if test="${not empty error}">
  <div class="alert alert-danger py-2">${error}</div>
</c:if>


<!-- Search + page size (no buttons) -->
<div class="mb-3">
  <div class="input-group">
    <input id="searchQ" name="q" class="form-control"
           value="<c:out value='${param.q}'/>"
           placeholder="Search name or description"/>

    <div class="input-group-append">
      <span class="input-group-text">Rows per page</span>
    </div>
    <select id="searchSize" name="size" class="custom-select" style="max-width:120px;">
      <option value="10" <c:if test="${param.size == '10' || empty param.size}">selected</c:if>>10</option>
      <option value="25" <c:if test="${param.size == '25'}">selected</c:if>>25</option>
      <option value="50" <c:if test="${param.size == '50'}">selected</c:if>>50</option>
    </select>
  </div>
</div>

<div class="card">
  <div class="card-body p-0">
    <div id="vertTableWrap">
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
      var $q    = $('#searchQ');
      var $size = $('#searchSize');
      var $wrap = $('#vertTableWrap');
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
        xhr = $.get(ctx + '/admin/verticals/table?' + params)
          .done(function(html){
            $wrap.html(stripScripts(html));
            if (history.replaceState) history.replaceState(null, '', ctx + '/admin/verticals?' + params);
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
