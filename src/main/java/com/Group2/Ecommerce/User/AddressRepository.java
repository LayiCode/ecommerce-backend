package com.Group2.Ecommerce.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);

    Optional<Address> findByUserIdAndFullNameAndLine1AndCityAndPostalCodeAndCountry(
            Long userId, String fullName, String line1, String city, String postalCode, String country);
}