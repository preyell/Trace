<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
  <h5 class="mb-0">Customers</h5>
  <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/admin/customers/new">
    New Customer
  </a>
</div>

<c:if test="${param.created == 'true'}"><div class="alert alert-primary py-2">Customer created.</div></c:if>
<c:if test="${param.updated == 'true'}"><div class="alert primary py-2">Customer updated.</div></c:if>
<c:if test="${param.deleted == 'true'}"><div class="alert alert-primary py-2">Customer deleted.</div></c:if>

<!-- Always-visible search + page size (no buttons) -->
<div class="mb-3">
  <div class="input-group">
    <input id="searchQ" name="q" class="form-control"
           value="<c:out value='${param.q}'/>"
           placeholder="Search name, contact, email or phone"/>

    <!-- append label + page-size select -->
    <div class="input-group-append">
      <span class="input-group-text">Rows per page</span>
    </div>
    <select id="searchSize" name="size" class="custom-select" style="max-width:110px;">
      <option value="10" <c:if test="${param.size == '10' || empty param.size}">selected</c:if>>10</option>
      <option value="25" <c:if test="${param.size == '25'}">selected</c:if>>25</option>
      <option value="50" <c:if test="${param.size == '50'}">selected</c:if>>50</option>
    </select>
  </div>
</div>


<div class="card">
  <div class="card-body p-0">
    <!-- This block is replaced via AJAX -->
    <div id="custTableWrap">
      <jsp:include page="_table.jsp" />
    </div>
  </div>
</div>

<!-- Live search script (uses jQuery from your AdminLTE layout) -->
<script>
(function(w, d){
  function onReady(fn){ if(d.readyState !== 'loading') fn(); else d.addEventListener('DOMContentLoaded', fn); }
  function waitForJQ(cb){ if(w.jQuery) cb(w.jQuery); else setTimeout(function(){ waitForJQ(cb); }, 50); }

  onReady(function(){
    waitForJQ(function($){
      var ctx   = '${fn:escapeXml(pageContext.request.contextPath)}';
      var $q    = $('#searchQ');
      var $size = $('#searchSize');
      var $wrap = $('#custTableWrap');
      var xhr   = null, debounceTimer = null;

      // Remove any <script> tags from the HTML fragment (no regex needed)
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
        xhr = $.get(ctx + '/admin/customers/table?' + params)
          .done(function(html){
            $wrap.html(stripScripts(html));
            if (history.replaceState) history.replaceState(null, '', ctx + '/admin/customers?' + params);
          })
          .fail(function(jq){ console.error('Live search failed', jq.status); })
          .always(function(){ $wrap.css('opacity', 1); });
      }

      $q.on('input', function(){ clearTimeout(debounceTimer); debounceTimer = setTimeout(function(){ load(0); }, 250); });
      $size.on('change', function(){ load(0); });
      $wrap.on('click', 'a.js-page', function(e){
        if (e.ctrlKey || e.metaKey) return;
        e.preventDefault();
        var p = parseInt($(this).data('page')); if (!isNaN(p)) load(p);
      });
    });
  });
})(window, document);
</script>
