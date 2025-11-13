// com.sybyl.trace.order.MarginReport.java
package com.sybyl.trace.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sybyl.trace.masterdata.Vertical;
import com.sybyl.trace.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "margin_reports")
public class MarginReport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// --- Associations ---
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id")
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vertical_id")
	private Vertical vertical;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "uploaded_by")
	private AppUser uploadedBy;

	@Size(max = 1000)
	@Column(name = "comments", length = 1000)
	private String comments;

	// --- Prices / FX ---
	@NotNull
	@Digits(integer = 18, fraction = 6)
	@Column(name = "buying_price", precision = 24, scale = 6, nullable = false)
	private BigDecimal buyingPrice;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "buying_currency", length = 3, nullable = false)
	private CurrencyCode buyingCurrency;

	@NotNull
	@Digits(integer = 18, fraction = 6)
	@Column(name = "selling_price", precision = 24, scale = 6, nullable = false)
	private BigDecimal sellingPrice;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "selling_currency", length = 3, nullable = false)
	private CurrencyCode sellingCurrency;

	/**
	 * Assumed to be "units of the relevant non-USD currency per 1 USD". Example: if
	 * KES is the non-USD currency, 130.00 means 1 USD = 130 KES.
	 */
	@NotNull
	@Digits(integer = 18, fraction = 8)
	@Column(name = "conversion_rate", precision = 24, scale = 8, nullable = false)
	private BigDecimal conversionRate;

	// --- NEW: persisted margin in USD ---
	@Digits(integer = 18, fraction = 6)
	@Column(name = "margin_amount_usd", precision = 24, scale = 6, nullable = true)
	private BigDecimal marginAmount;

	// --- File meta ---
	@NotBlank
	@Size(max = 255)
	@Column(name = "file_name", length = 255, nullable = false)
	private String fileName;

	@NotBlank
	@Size(max = 255)
	@Column(name = "storage_key", length = 255, nullable = false)
	private String storageKey;

	// --- Status & audit ---
	@Enumerated(EnumType.STRING)
	@Column(name = "approval_status", nullable = false)
	private ApprovalStatus approvalStatus = ApprovalStatus.FINANCE_PENDING;

	@Column(name = "uploaded_on", nullable = false, updatable = false)
	private Instant uploadedOn = Instant.now();

	@Transient
	public Date getUploadedOnDate() {
		return uploadedOn == null ? null : Date.from(uploadedOn);
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approved_by")
	private com.sybyl.trace.user.AppUser approvedBy;

	@Column(name = "approved_on")
	private Instant approvedOn;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "finance_approved_by_id")
	private AppUser financeApprovedBy;

	private Instant financeApprovedOn;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ceo_approved_by_id")
	private AppUser ceoApprovedBy;

	private Instant ceoApprovedOn;

	@Column(columnDefinition = "text")
	private String rejectionReason;


	@OneToMany(mappedBy = "marginReport")
	@OrderBy("actedOn DESC")
	private List<MarginReportAudit> audits = new ArrayList<>();

	// ---- Existing transient getter (now USD, if you still want a quick view) ----
	@Transient
	public BigDecimal getMarginAmountUsdRounded() {
		return marginAmount == null ? null : marginAmount.setScale(2, RoundingMode.HALF_UP);
	}

	// ---- Lifecycle: compute margin in USD before save/update ----
	@PrePersist
	@PreUpdate
	private void computeDerivedFields() {
		this.marginAmount = calculateMarginInUSD();
	}

	// ---- Core calculation (USD) ----
	private BigDecimal calculateMarginInUSD() {
		if (buyingPrice == null || sellingPrice == null)
			return null;

		// If either side is non-USD, conversionRate must be present
		if ((buyingCurrency != CurrencyCode.USD || sellingCurrency != CurrencyCode.USD) && conversionRate == null) {
			return null;
		}

		// If both are non-USD and different, one rate is insufficient
		if (buyingCurrency != CurrencyCode.USD && sellingCurrency != CurrencyCode.USD
				&& buyingCurrency != sellingCurrency) {
			// You can throw, log, or return null; choosing null to avoid breaking
			// persistence.
			return null;
		}

		BigDecimal sellingUSD = toUSD(sellingPrice, sellingCurrency, conversionRate);
		BigDecimal buyingUSD = toUSD(buyingPrice, buyingCurrency, conversionRate);

		if (sellingUSD == null || buyingUSD == null)
			return null;

		// Store with reasonable precision; format to 6 dp (display can round to 2)
		return sellingUSD.subtract(buyingUSD).setScale(6, RoundingMode.HALF_UP);
	}

	private static BigDecimal toUSD(BigDecimal amount, CurrencyCode currency, BigDecimal rate) {
		if (amount == null)
			return null;
		if (currency == CurrencyCode.USD)
			return amount;
		if (rate == null || BigDecimal.ZERO.compareTo(rate) == 0)
			return null;

		// amount (non-USD) ÷ (units of that non-USD per 1 USD) = USD
		return amount.divide(rate, 8, RoundingMode.HALF_UP);
	}
}
