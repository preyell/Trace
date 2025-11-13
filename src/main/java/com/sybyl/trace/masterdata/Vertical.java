package com.sybyl.trace.masterdata;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "vertical", uniqueConstraints = @UniqueConstraint(name="uk_vertical_name", columnNames = "name"))
@Getter @Setter
public class Vertical {

    public Vertical(String name) {
        this.name = name;
    }

	public Vertical() {
		// TODO Auto-generated constructor stub
	}
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 500)
  private String description;

  @Column(nullable = false)
  private boolean active = true;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;
}
