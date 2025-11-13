// com.sybyl.trace.order.OrderForm
package com.sybyl.trace.order;

import java.util.HashSet;
import java.util.Set;

import com.sybyl.trace.location.Location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderForm {
	private Long id;

	@NotBlank
	@Size(max = 50)
	@Pattern(regexp = "^[A-Za-z0-9._\\-]+$", message = "Only letters, numbers, dot, dash and underscore are allowed")
	private String salesOrderId;

	@NotNull
	private Long customerId;

	@Size(max = 500)
	private String description;

	@NotNull
	private Long salesManagerId;

	@NotNull
	private Location location;

	// vertical IDs chosen in form
	private Set<Long> verticalIds = new HashSet<>();
}
