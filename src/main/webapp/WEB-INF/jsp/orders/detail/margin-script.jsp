<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>

</body>

<script>
(function () {
  var CTX = '${pageContext.request.contextPath}';
  var ORDER_ID = '${order.id}'; // keep as string-safe

  function updateLabel(input) {
    var label = input && input.nextElementSibling;
    if (!label || !label.classList.contains('custom-file-label')) return;
    var name = (input.files && input.files.length) ? input.files[0].name : 'Choose file...';
    label.textContent = name;
    label.classList.add('selected');
  }

  function normToFixed(input, dp, fallback) {
    var v = (input.value || '').toString().trim().replace(',', '.');
    if (v === '') { input.value = fallback; return; }
    var n = Number(v);
    if (isNaN(n)) { input.value = fallback; return; }
    input.value = n.toFixed(dp);
  }

  function isValidPair(buy, sell) {
    return buy && sell && (buy === sell || buy === 'USD' || sell === 'USD');
  }

  function validateCurrencyPairOrBlock(form) {
    var buy = form.querySelector('select[name="buyingCurrency"]').value;
    var sell = form.querySelector('select[name="sellingCurrency"]').value;
    if (!isValidPair(buy, sell)) {
      alert('Invalid currency pair. Use the same currency on both sides, or make one side USD.');
      return false;
    }
    return true;
  }

  function fillEditFormFromBtn($btn){
    var mrId = $btn.data('mrid');
    if (!mrId) { console.error('[MR-EDIT] Missing mrId on button'); return; }

    var $form = $('#mrEditForm');
    var actionUrl = CTX + '/orders/' + ORDER_ID + '/margin-reports/' + mrId + '/update';
    $form.attr('action', actionUrl);

    $form.find('[name="buyingPrice"]').val($btn.data('buying'));
    $form.find('[name="buyingCurrency"]').val($btn.data('bcur'));
    $form.find('[name="sellingPrice"]').val($btn.data('selling'));
    $form.find('[name="sellingCurrency"]').val($btn.data('scur'));
    $form.find('[name="conversionRate"]').val($btn.data('fx'));
    $form.find('[name="verticalId"]').val($btn.data('vertical'));
    $form.find('[name="comments"]').val($btn.data('comments'));

    console.log('[MR-EDIT] action set ->', actionUrl);
  }

  $(function () {
    // ---- File label ----
    document.addEventListener('change', function(e) {
      if (e.target && e.target.matches('input[type="file"].custom-file-input')) updateLabel(e.target);
    });
    if (window.bsCustomFileInput && typeof bsCustomFileInput.init === 'function') {
      bsCustomFileInput.init();
    }

    // ---- Create modal: normalize + validate ----
    $('#mrModal').on('shown.bs.modal', function() {
      var scope = document.getElementById('mrModal');
      var fx = scope.querySelector('input[name="conversionRate"]');
      var buy = scope.querySelector('select[name="buyingCurrency"]');
      var sell = scope.querySelector('select[name="sellingCurrency"]');
      if (buy && sell && fx) {
        var bothUSD = buy.value === 'USD' && sell.value === 'USD';
        if (bothUSD) {
          fx.value = '1.000000';
          fx.setAttribute('readonly','readonly');
          fx.classList.add('bg-light');
        } else {
          fx.removeAttribute('readonly');
          fx.classList.remove('bg-light');
          if (!fx.value) fx.value = '1.000000';
        }
      }
    });
    $('#mrModal form').on('submit', function(e) {
      var scope = document.getElementById('mrModal');
      scope.querySelectorAll('.js-money-2dp').forEach(function(el) {
        normToFixed(el, 2, '0.00');
      });
      var fx = scope.querySelector('input[name="conversionRate"]');
      if (fx) normToFixed(fx, 6, '1.000000');
      if (!validateCurrencyPairOrBlock(this)) { e.preventDefault(); return false; }
    });

    // ---- Edit modal: set action + populate (Bootstrap 4 uses data-toggle/data-target) ----
    $(document).on('show.bs.modal', '#mrEditModal', function (e) {
      var $btn = $(e.relatedTarget);
      if (!$btn || !$btn.length) {
        console.warn('[MR-EDIT] show.bs.modal fired but no relatedTarget');
        return;
      }
      fillEditFormFromBtn($btn);
    });

    // Fallback: also on click of the Edit button
    $(document).on('click', 'button[data-target="#mrEditModal"]', function(){
      fillEditFormFromBtn($(this));
    });

    // ---- Edit form: safety + validate ----
    $(document).on('submit', '#mrEditForm', function(e){
      if (!this.action || !/\/orders\/\d+\/margin-reports\/\d+\/update$/.test(this.action)) {
        e.preventDefault();
        alert('Edit form did not initialize. Please click Edit again.');
        console.warn('[MR-EDIT] Blocked submit; bad action:', this.action);
        return false;
      }
      if (!validateCurrencyPairOrBlock(this)) { e.preventDefault(); return false; }
    });

    // ---- Audit modal loader ----
    $(document).on('show.bs.modal', '#auditModal', function (e) {
      var $btn = $(e.relatedTarget);
      var url  = $btn && $btn.data('audit-url');
      var $body = $('#auditModalBody');
      $body.html('Loading');
      if (!url) { $body.html('<div class="text-danger">Invalid audit URL.</div>'); return; }
      fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' }})
        .then(function(r){ return r.text(); })
        .then(function(html){ $body.html(html); })
        .catch(function(){ $body.html('<div class="text-danger">Failed to load audit.</div>'); });
    });

    // ---- Diag ----
    console.log('[MR-EDIT] Ready. Edit buttons:',
      document.querySelectorAll('button[data-target="#mrEditModal"]').length);
  });
})();
</script>
<script>
(function(){
  $(document).on('show.bs.modal', '#rejectModal', function(e){
    var btn = $(e.relatedTarget);
    var mrId = btn.data('mrid');
    var form = $('#rejectForm');
    // default to FINANCE rejection; server will validate stage+role
    form.attr('action', '${pageContext.request.contextPath}/orders/${order.id}/margin-reports/' + mrId + '/reject');
  });
})();
</script>

<script>
(function(){
  function waitForJQ(fn){ if (typeof window.jQuery==='undefined') return setTimeout(()=>waitForJQ(fn),50); fn(); }
  waitForJQ(function(){
    // Load audit into modal
    $('#expAuditModal').on('show.bs.modal', function(e){
      var btn = $(e.relatedTarget);
      var url = btn && btn.data('audit-url');
      var body = $('#expAuditBody');
      body.text('Loading...');
      if (!url) { body.text('Missing audit URL'); return; }
      fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' }})
        .then(r=>r.text()).then(html=> body.html(html))
        .catch(()=> body.html('<div class="text-danger">Failed to load audit.</div>'));
    });

  });
})();
</script>
<script>
(function(){
  // Edit modal: fill fields & set action
  $('#expEditModal').on('show.bs.modal', function (e) {
    var btn = $(e.relatedTarget);
    var id  = btn.data('expid');

    $('#expEditForm').attr('action', 
      '${pageContext.request.contextPath}/orders/${order.id}/expenses/' + id + '/update');

    $('#expEditLabel').val(btn.data('labelid'));
    $('#expEditAmount').val(btn.data('amount'));
    $('#expEditCurrency').val(btn.data('currency'));
    $('#expEditRate').val(btn.data('rate'));
    $('#expEditVertical').val(btn.data('verticalid'));
    $('#expEditComments').val(btn.data('comments'));
  });

  // Delete modal: set action
  $('#expDeleteModal').on('show.bs.modal', function (e) {
    var btn = $(e.relatedTarget);
    var id  = btn.data('expid');
    $('#expDeleteForm').attr('action', 
      '${pageContext.request.contextPath}/orders/${order.id}/expenses/' + id + '/delete');
  });
})();
</script>

<script>
(function(){
  // Build a safe context path like "/app" (no trailing slash)
  const ctx = '<c:url value="/" />'.replace(/\/$/, '');
  const orderId = '${order.id}';

  // Utility: read CSRF token
  function csrf() {
    return {
      header: 'X-CSRF-TOKEN',
      token: document.getElementById('csrfTokenField')?.value || ''
    };
  }

  // Utility: simple number format
  function fmt(n) {
    if (n == null || isNaN(n)) return '0.00';
    return Number(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  // When modal opens
  $('#expDisburseModal').on('show.bs.modal', function (e) {
    const btn = $(e.relatedTarget);
    const expId = btn.data('expid');

    // Expense meta from data-* on the button (optional but handy)
    const expAmount   = parseFloat(btn.data('exp-amount')) || 0;
    const expCurrency = (btn.data('exp-currency') || '').toString();
    const expStatus   = (btn.data('exp-status') || '').toString();

    // 1) Update form action (replace placeholder)
    $('#disburseForm').attr('action', '${ctx}/orders/${orderId}/expenses/${expId}/disburse');

    // 2) Show top meta
    $('#metaAmount').text(fmt(expAmount));
    $('#metaCurrency').text(expCurrency);
    $('#metaStatus').text(expStatus);
    $('#metaDisbursed').text('0');
    $('#metaDisbursedCur').text(expCurrency);

    // 3) Load past disbursements table fragment
    const wrap = $('#disbTableWrap').html('<div class="text-muted">Loading...</div>');
    fetch('${ctx}/orders/${orderId}/expenses/${expId}/disbursements', {
      headers: { 'X-Requested-With': 'XMLHttpRequest' }
    })
    .then(r => r.text())
    .then(html => {
      wrap.html(html);

      // After load, compute total disbursed from rows (expects data-amount on <tr> or parse cell)
      let total = 0;
      wrap.find('tr[data-amount]').each(function(){
        const amt = parseFloat($(this).attr('data-amount')) || 0;
        total += amt;
      });
      // If your fragment doesn’t include data-amount, you can parse from a known cell instead.

      $('#metaDisbursed').text(fmt(total));
      $('#metaDisbursedCur').text(expCurrency);
    })
    .catch(() => wrap.html('<div class="text-danger">Failed to load.</div>'));

    // 4) Attach delegated delete handler (once)
    //    The table fragment should render delete buttons as:
    //    <button class="btn btn-sm btn-outline-danger js-disb-del" data-disbid="ID">Delete</button>
    $('#disbTableWrap').off('click', '.js-disb-del').on('click', '.js-disb-del', function(){
      const disbId = $(this).data('disbid');
      if (!confirm('Delete this disbursement?')) return;

      fetch('${ctx}/orders/${orderId}/expenses/${expId}/disbursements/${disbId}', {
        method: 'DELETE',
        headers: {
          'X-Requested-With': 'XMLHttpRequest',
          [csrf().header]: csrf().token
        }
      })
      .then(r => {
        if (!r.ok) throw new Error('Delete failed');
        // Reload the fragment after delete
        return fetch('${ctx}/orders/${orderId}/expenses/${expId}/disbursements', {
          headers: { 'X-Requested-With': 'XMLHttpRequest' }
        });
      })
      .then(r => r.text())
      .then(html => {
        $('#disbTableWrap').html(html);
        // recompute total
        let total = 0;
        $('#disbTableWrap').find('tr[data-amount]').each(function(){
          const amt = parseFloat($(this).attr('data-amount')) || 0;
          total += amt;
        });
        $('#metaDisbursed').text(fmt(total));
      })
      .catch(() => alert('Failed to delete disbursement.'));
    });

  });

  // Client-side validation before submit
  $('#disburseForm').on('submit', function(ev){
    ev.preventDefault();

    const form = this;
    const action = form.getAttribute('action'); // .../orders/{orderId}/expenses/{expId}/disburse
    const match = action.match(/\/expenses\/(\d+)\/disburse$/);
    if (!match) {
      alert('Invalid form action.');
      return;
    }
    const expId = match[1];

    const amount = parseFloat(form.amount.value || '0');
    const currency = form.currency.value;

    // Quick input checks
    if (!amount || amount <= 0) {
      alert('Amount must be greater than 0.');
      return;
    }

    // Hit a small JSON endpoint for server truth (recommended)
    // Should return: { approved:true/false, expenseAmount: number, expenseCurrency: "UGX", totalDisbursed: number }
    fetch('${ctx}/orders/${orderId}/expenses/${expId}/disbursements/summary', {
      headers: { 'X-Requested-With': 'XMLHttpRequest' }
    })
    .then(r => r.ok ? r.json() : Promise.reject())
    .then(sum => {
      // 1) CFO approval required
      if (!sum.approved) {
        alert('This expense is not CFO-approved yet. You cannot disburse.');
        return;
      }
      // 2) Currency must match
      if (currency !== sum.expenseCurrency) {
        alert('Currency mismatch. Expense currency is ${sum.expenseCurrency}.');
        return;
      }
      // 3) Cap total disbursed
      const newTotal = (Number(sum.totalDisbursed) || 0) + amount;
      if (newTotal > Number(sum.expenseAmount)) {
    	  alert(
    			  'Over-disbursement.\n' +
    			  'Approved Expense: ' + fmt(sum.expenseAmount) + ' ' + sum.expenseCurrency + '\n' +
    			  'Already Disbursed: ' + fmt(sum.totalDisbursed) + '\n' +
    			  'This Entry: ' + fmt(amount) + '\n' +
    			  '=> Would exceed the approved amount.'
    			);

        return;
      }

      // Passed checks -> submit
      form.submit();
    })
    .catch(() => {
      // Fallback (no summary endpoint): compare against the visible meta numbers
      const expAmt = parseFloat($('#metaAmount').text().replace(/,/g,'')) || 0;
      const already = parseFloat($('#metaDisbursed').text().replace(/,/g,'')) || 0;
      const status = $('#metaStatus').text().trim();

      if (status !== 'CFO_APPROVED') {
        alert('This expense is not CFO-approved yet. You cannot disburse.');
        return;
      }
      if ((already + amount) > expAmt) {
        alert('Over-disbursement (computed from current table).');
        return;
      }
      form.submit();
    });
  });

})();
</script>


</html>