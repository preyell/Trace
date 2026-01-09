<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>


<!-- Global helpers / constants -->
<script>
  const ctx      = '${pageContext.request.contextPath}';
  const orderId  = '${order.id}';
  const baseDisbUrl = ctx + '/orders/' + orderId + '/expenses/';

  function fmt(n) {
    if (n === null || n === undefined) return "0.00";
    let num = Number(n);
    if (isNaN(num)) return "0.00";
    return num.toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }

  // helper to get CSRF token/header (global)
  function csrf() {
    const token = document.getElementById('csrfTokenField')?.value || '';
    return { header: 'X-CSRF-TOKEN', token: token };
  }
</script>

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

  function showMrCreateError(msg) {
	  var $a = $('#mrCreateErrorAlert');
	  var $t = $('#mrCreateErrorText');
	  if ($t.length) $t.text(msg);
	  if ($a.length) $a.removeClass('d-none').show();
	}

	function clearMrCreateError() {
	  $('#mrCreateErrorAlert').addClass('d-none').hide();
	  $('#mrCreateErrorText').text('');
	}

	function parseMoneyStrict(raw) {
	  // allow: 123, 123.45, 1,234.56 (commas removed)
	  var s = (raw || '').toString().trim().replace(/,/g, '');
	  if (s === '') return { ok:false, reason:'required' };
	  if (!/^\d+(\.\d{1,6})?$/.test(s)) return { ok:false, reason:'format' };
	  var n = Number(s);
	  if (!isFinite(n)) return { ok:false, reason:'format' };
	  return { ok:true, value:n };
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

    wireMrEditNoChangeDisable();
    console.log('[MR-EDIT] action set ->', actionUrl);
  }
  function wireMrEditNoChangeDisable() {
	  var $form = $('#mrEditForm');
	  if (!$form.length) return;

	  // enable submit temporarily while taking snapshot
	  var $submit = $form.find('button[type="submit"]');

	  // snapshot AFTER values are populated
	  var original = $form.serialize();

	  // default: disabled until something changes
	  $submit.prop('disabled', true);

	  // avoid double-binding if modal opened multiple times
	  $form.off('.mrChanged');

	  $form.on('change.mrChanged keyup.mrChanged', 'input, select, textarea', function () {
	    var changed = $form.serialize() !== original;
	    $submit.prop('disabled', !changed);
	  });

	  // if you also want file change to enable submit (file input not in serialize)
	  $form.on('change.mrChanged', 'input[type="file"]', function () {
	    $submit.prop('disabled', false);
	  });
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
    	  clearMrCreateError();

    	  var scope = document.getElementById('mrModal');
    	  var buyEl  = scope.querySelector('input[name="buyingPrice"]');
    	  var sellEl = scope.querySelector('input[name="sellingPrice"]');
    	  var fxEl   = scope.querySelector('input[name="conversionRate"]');

    	  // reset field UI
    	  [buyEl, sellEl, fxEl].forEach(function(el){
    	    if (!el) return;
    	    el.classList.remove('is-invalid');
    	  });

    	  // strict numeric validation
    	  var buy  = parseMoneyStrict(buyEl.value);
    	  var sell = parseMoneyStrict(sellEl.value);

    	  if (!buy.ok) {
    	    buyEl.classList.add('is-invalid');
    	    showMrCreateError('Buying price must be a valid number (e.g. 1200 or 1200.50).');
    	    buyEl.focus();
    	    e.preventDefault(); return false;
    	  }
    	  if (!sell.ok) {
    	    sellEl.classList.add('is-invalid');
    	    showMrCreateError('Selling price must be a valid number (e.g. 1500 or 1500.50).');
    	    sellEl.focus();
    	    e.preventDefault(); return false;
    	  }

    	  // normalize to 2dp AFTER validation
    	  buyEl.value  = buy.value.toFixed(2);
    	  sellEl.value = sell.value.toFixed(2);

    	  // conversion rate (6dp) - validate too
    	  var fx = parseMoneyStrict(fxEl.value);
    	  if (!fx.ok || fx.value <= 0) {
    	    fxEl.classList.add('is-invalid');
    	    showMrCreateError('Conversion rate must be a valid number greater than 0.');
    	    fxEl.focus();
    	    e.preventDefault(); return false;
    	  }
    	  fxEl.value = fx.value.toFixed(6);

    	  // keep your existing currency pair validation
    	  if (!validateCurrencyPairOrBlock(this)) { e.preventDefault(); return false; }

    	  return true;
    	});


    // ---- Edit modal: set action + populate ----
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
  $('#expEditComments').val(btn.data('comments'));

  var currentVerticalId = String(btn.data('verticalid') || '');
  var $sel = $('#expEditVertical');

  $sel.html('<option value="">Loading...</option>');

  fetch('${pageContext.request.contextPath}/orders/${order.id}/expenses/' + id + '/verticals', {
    headers: { 'X-Requested-With': 'XMLHttpRequest' }
  })
  .then(r => r.ok ? r.json() : [])
  .then(items => {
    var html = '<option value="">Select</option>';
    items.forEach(v => {
      html += '<option value="' + v.id + '">' + (v.name || ('Vertical ' + v.id)) + '</option>';
    });
    $sel.html(html);

    // set current after options exist
    if (currentVerticalId) $sel.val(currentVerticalId);
  })
  .catch(() => {
    $sel.html('<option value="">Failed to load</option>');
  });
});


  $(function () {
	  $(document).on('show.bs.modal', '#expDeleteModal', function (e) {
	    var btn = $(e.relatedTarget);
	    var id  = btn.data('expid');

	    $('#expDeleteForm').attr('action',
	      '${pageContext.request.contextPath}/orders/${order.id}/expenses/' + id + '/delete'
	    );
	  });
	});

})();
</script>

<!-- Margin Report FX auto-handling (create + edit) -->
<script>
$(function () {

  function wireMarginModal(modalSelector) {
    const $modal = $(modalSelector);

    const $buy  = $modal.find('.mr-buying-currency');
    const $sell = $modal.find('.mr-selling-currency');
    const $rate = $modal.find('.mr-conversion-rate');

    if ($buy.length === 0 || $sell.length === 0 || $rate.length === 0) {
      return;
    }

    function normalizeRateDecimals() {
      let val = $rate.val();
      if (!val) return;
      const n = Number(val);
      if (isNaN(n)) return;
      $rate.val(n.toFixed(2));  // always 2 decimals
    }

    function updateConversionRateState() {
      const buy  = $buy.val();
      const sell = $sell.val();
      const bothUSD = (buy === 'USD' && sell === 'USD');

      if (bothUSD) {
        $rate.val('1.00');
        $rate.prop('readonly', true);
        $rate.addClass('bg-light');
      } else {
        $rate.prop('readonly', false);
        $rate.removeClass('bg-light');
        normalizeRateDecimals();
      }
    }

    $buy.off('change.mrfx').on('change.mrfx', updateConversionRateState);
    $sell.off('change.mrfx').on('change.mrfx', updateConversionRateState);
    $rate.off('blur.mrfx').on('blur.mrfx', normalizeRateDecimals);

    $modal.off('show.bs.modal.mrfx').on('show.bs.modal.mrfx', function () {
      setTimeout(updateConversionRateState, 10);
    });

    updateConversionRateState();
  }

  wireMarginModal('#mrModal');      // create margin report
  wireMarginModal('#mrEditModal');  // update margin report

});
</script>

<!-- Additional Expense FX auto-handling (create + edit) -->
<script>
$(function () {

  function wireAdditionalExpenseModal(modalSelector) {
    const $modal = $(modalSelector);

    const $cur  = $modal.find('.ae-currency');
    const $rate = $modal.find('.ae-conversion-rate');

    if ($cur.length === 0 || $rate.length === 0) {
      return;
    }

    function normalizeRateDecimals() {
      let val = $rate.val();
      if (!val) return;
      const n = Number(val);
      if (isNaN(n)) return;
      $rate.val(n.toFixed(2));  // force 2 decimals
    }

    function updateRateState() {
      const cur = $cur.val();
      const isUSD = (cur === 'USD');

      if (isUSD) {
        $rate.val('1.00');
        $rate.prop('readonly', true);
        $rate.addClass('bg-light');
      } else {
        $rate.prop('readonly', false);
        $rate.removeClass('bg-light');
        normalizeRateDecimals();
      }
    }

    $cur.off('change.aefx').on('change.aefx', updateRateState);
    $rate.off('blur.aefx').on('blur.aefx', normalizeRateDecimals);

    $modal.off('show.bs.modal.aefx').on('show.bs.modal.aefx', function () {
      setTimeout(updateRateState, 10);
    });

    updateRateState();
  }

  wireAdditionalExpenseModal('#expCreateModal');   // add
  wireAdditionalExpenseModal('#expEditModal');     // edit

});
</script>

<!-- Disburse (Consume) modal FX for currency/rate fields -->
<script>
$(function () {

  function wireDisburseModal() {
    const $modal = $('#expDisburseModal');
    const $cur   = $modal.find('.ae-disburse-currency');
    const $rate  = $modal.find('.ae-disburse-rate');

    if ($cur.length === 0 || $rate.length === 0) return;

    function normalizeRateDecimals() {
      let val = $rate.val();
      if (!val) return;
      const n = Number(val);
      if (isNaN(n)) return;
      $rate.val(n.toFixed(2));
    }

    function updateRateState() {
      const cur = $cur.val();
      const isUSD = (cur === 'USD');

      if (isUSD) {
        $rate.val('1.00');
        $rate.prop('readonly', true);
        $rate.addClass('bg-light');
      } else {
        $rate.prop('readonly', false);
        $rate.removeClass('bg-light');
        normalizeRateDecimals();
      }
    }

    $cur.off('change.disbfx').on('change.disbfx', updateRateState);
    $rate.off('blur.disbfx').on('blur.disbfx', normalizeRateDecimals);

    $modal.off('show.bs.modal.disbfx').on('show.bs.modal.disbfx', function () {
      setTimeout(updateRateState, 10);
    });

    updateRateState();
  }

  wireDisburseModal();

});
</script>

<!-- Expense reject + MR delete modals -->
<script>
$('#expenseRejectModal').on('show.bs.modal', function (e) {
    const btn = $(e.relatedTarget);
    const expId = btn.data('expid');

    $('#expenseRejectForm').attr('action',
        '${pageContext.request.contextPath}/orders/${order.id}/expenses/' + expId + '/reject');
});

// Margin Report delete modal
$('#mrDeleteModal').on('show.bs.modal', function (e) {
  const btn  = $(e.relatedTarget);
  const mrId = btn.data('mrid');

  $('#mrDeleteForm').attr(
    'action',
    '${pageContext.request.contextPath}/orders/${order.id}/margin-reports/' + mrId + '/delete'
  );
});
</script>

<!-- Auto-open Margin Report modal when needed -->
<script>
(function () {
  document.addEventListener('DOMContentLoaded', function () {
    var show = '${showMarginModal}' === 'true';
    if (show) {
      $('#mrModal').modal('show');
    }
  });
})();
</script>

<!-- Unified Consume (Disburse) modal behaviour with error handling -->
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<script>
(function() {
  // From RedirectAttributes (used in JSP)
  const openErrId = '${openConsumeExpenseId}';
  const errMsg    = '${fn:escapeXml(expenseError)}';

  let consumeErrorShownOnce = false;

  // Auto-open the failing expense's modal once after redirect
  $(function() {
    if (openErrId && openErrId !== 'null' && openErrId !== '') {
      const $btn = $('button.js-exp-consume[data-expid="' + openErrId + '"]');
      if ($btn.length) {
        $btn.trigger('click');
      }
    }
  });

  $('#expDisburseModal').on('show.bs.modal', function (e) {
    const btn   = $(e.relatedTarget);
    const expId = String(btn.data('expid'));

    const expAmount   = parseFloat(btn.data('exp-amount')) || 0;
    const expCurrency = (btn.data('exp-currency') || '').toString();
    const expStatus   = (btn.data('exp-status') || '').toString();
    const expCapUsd   = parseFloat(btn.data('exp-usd')) || 0;

    const $err     = $('#expConsumeErrorAlert');
    const $errText = $('#expConsumeErrorText');

    if ($err.length) {
      const hasErrCtx = openErrId && openErrId !== 'null' && openErrId !== '';
      const sameExp   = expId === String(openErrId);

      if (hasErrCtx && sameExp && !consumeErrorShownOnce && errMsg) {
        $errText.text(errMsg);
        $err.removeClass('d-none').show();
        consumeErrorShownOnce = true;
      } else {
        $err.addClass('d-none').hide();
        $errText.text('');
      }
    }

    // form action
    $('#disburseForm').attr('action', baseDisbUrl + expId + '/consume');

    // meta bar basics
    $('#metaAmount').text(fmt(expAmount));
    $('#metaCurrency').text(expCurrency);
    $('#metaStatus').text(expStatus);

    $('#metaDisbursed').text('0.00');
    $('#metaDisbursedCur').text('USD');

    $('#metaRemaining').text(fmt(expCapUsd));
    $('#metaRemainingCur').text('USD');

    // load past consumptions
    const wrap = $('#disbTableWrap').html('<div class="text-muted">Loading...</div>');
    fetch(baseDisbUrl + expId + '/disbursements', {
      headers: { 'X-Requested-With': 'XMLHttpRequest' }
    })
    .then(r => r.text())
    .then(html => {
      wrap.html(html);

      let total = 0;
      wrap.find('tr[data-amount]').each(function(){
        const amt = parseFloat($(this).attr('data-amount')) || 0;
        total += amt;
      });

      const remaining = Math.max(0, expCapUsd - total);

      $('#metaDisbursed').text(fmt(total));
      $('#metaDisbursedCur').text('USD');

      $('#metaRemaining').text(fmt(remaining));
      $('#metaRemainingCur').text('USD');
    })
    .catch(() => wrap.html('<div class="text-danger">Failed to load.</div>'));

    // delete consumption handler (unchanged)
    $('#disbTableWrap').off('click', '.js-disb-del').on('click', '.js-disb-del', function(){
      const disbId = $(this).data('disbid');
      if (!confirm('Delete this consumption?')) return;

      fetch(baseDisbUrl + expId + '/disbursements/' + disbId + '/delete', {
        method: 'POST',
        headers: {
          'X-Requested-With': 'XMLHttpRequest',
          [csrf().header]: csrf().token
        }
      })
      .then(r => {
        if (!r.ok) throw new Error('Delete failed');
        return fetch(baseDisbUrl + expId + '/disbursements', {
          headers: { 'X-Requested-With': 'XMLHttpRequest' }
        });
      })
      .then(r => r.text())
      .then(html => {
        $('#disbTableWrap').html(html);

        let total = 0;
        $('#disbTableWrap').find('tr[data-amount]').each(function(){
          const amt = parseFloat($(this).attr('data-amount')) || 0;
          total += amt;
        });

        const remaining = Math.max(0, expCapUsd - total);

        $('#metaDisbursed').text(fmt(total));
        $('#metaDisbursedCur').text('USD');

        $('#metaRemaining').text(fmt(remaining));
        $('#metaRemainingCur').text('USD');
      })
      .catch(() => alert('Failed to delete consumption.'));
    });
  });

  $('#expDisburseModal').on('hidden.bs.modal', function () {
    $('#expConsumeErrorAlert').addClass('d-none').hide();
    $('#expConsumeErrorText').text('');
  });

})();
</script>

</html>
