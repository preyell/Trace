package com.sybyl.trace.masterdata;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "customers")
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 128)
    @Column(nullable = false, length = 128)
    private String name;

    @Size(max = 128)
    private String contactName;

    @Email @Size(max = 128)
    private String email;

    @Size(max = 32)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    private Instant updatedAt;

    // getters/setters

    @PreUpdate void onUpdate(){ this.updatedAt = Instant.now(); }
}
