package com.Group2.Ecommerce.User;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String label; // e.g. "Home", "Work"

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String line1;

    private String line2;

    @Column(nullable = false)
    private String city;

    private String state;

    @Column(nullable = false)
    private String postalCode;

    @Column(nullable = false, length = 100)
    private String country;

    private String phone;

    @Column(nullable = false)
    private boolean isDefault = false;
}