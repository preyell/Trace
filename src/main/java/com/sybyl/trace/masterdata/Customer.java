package com.sybyl.trace.masterdata;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "customers")
public class Customer {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 128)
	@Column(nullable = false, length = 128)
	private String name;

	@Size(max = 128)
	@Pattern(regexp = "^[A-Za-z][A-Za-z .'-]*$", message = "Contact name must contain letters only")
	private String contactName;

	@Pattern(regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Enter valid email address")
	private String email;

	@Size(max = 32)
	@Pattern(
	  regexp = "^$|^[0-9+()\\-\\s]{7,32}$",
	  message = "Phone number must have at least 7 digits and no letters"
	)
	private String phone;

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();
	private Instant updatedAt;

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	public void setContactName(String contactName) {
		this.contactName = (contactName == null || contactName.trim().isEmpty()) ? null : contactName.trim();
	}
	public void setPhone(String phone) {
		  this.phone = (phone == null) ? "" : phone.trim();
		}
}
