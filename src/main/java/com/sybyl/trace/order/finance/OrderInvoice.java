package com.sybyl.trace.order.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.sybyl.trace.order.CurrencyCode;
import com.sybyl.trace.order.Order;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "order_invoice")
@Getter
@Setter
public class OrderInvoice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@Column(name = "invoice_number", nullable = false, length = 64)
	private String invoiceNumber;

	@Column(name = "amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "currency", length = 8, nullable = false)
	private CurrencyCode currency;

	@Column(name = "invoice_delivery_date", nullable = false)
	private LocalDate invoiceDeliveryDate;

	@Column(name = "expected_payment_date", nullable = false)
	private LocalDate expectedPaymentDate;

	@Column(name = "payment_received_date")
	private LocalDate paymentReceivedDate; // NEW

	@Column(name = "invoice_percent", precision = 6, scale = 2, nullable = false)
	private BigDecimal invoicePercent;

	@Column(name = "conversion_rate", precision = 18, scale = 6)
	private BigDecimal conversionRate; // NEW

	@Column(name = "final_invoice", nullable = false)
	private boolean finalInvoice = false; // NEW

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "storage_key")
	private String storageKey;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uploaded_by_id")
	private AppUser uploadedBy;

	@Column(name = "uploaded_on")
	private Instant uploadedOn;
}
