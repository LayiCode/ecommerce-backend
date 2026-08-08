package com.Group2.Ecommerce.User;

import com.Group2.Ecommerce.Common.Exception.ResourceNotFoundException;
import com.Group2.Ecommerce.User.Dto.AddressRequest;
import com.Group2.Ecommerce.User.Dto.AddressResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    public List<AddressResponse> getMyAddresses() {
        User currentUser = getCurrentUser();
        return addressRepository.findByUserId(currentUser.getId()).stream()
                .map(AddressResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse create(AddressRequest request) {
        User currentUser = getCurrentUser();

        Address existing = addressRepository
                .findByUserIdAndFullNameAndLine1AndCityAndPostalCodeAndCountry(
                        currentUser.getId(), request.getFullName(), request.getLine1(),
                        request.getCity(), request.getPostalCode(), request.getCountry())
                .orElse(null);

        if (existing != null) {
            applyRequest(existing, request);
            return AddressResponse.fromEntity(addressRepository.save(existing));
        }

        Address address = new Address();
        applyRequest(address, request);
        address.setUser(currentUser);

        return AddressResponse.fromEntity(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(Long id, AddressRequest request) {
        Address address = findOwnedAddress(id);
        applyRequest(address, request);
        return AddressResponse.fromEntity(addressRepository.save(address));
    }

    @Transactional
    public void delete(Long id) {
        Address address = findOwnedAddress(id);
        addressRepository.delete(address);
    }

    // Ensures a user can only fetch/edit/delete their own addresses,
    // not just any address ID they guess.
    private Address findOwnedAddress(Long id) {
        User currentUser = getCurrentUser();
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + id));

        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Address not found: " + id);
        }
        return address;
    }

    private void applyRequest(Address address, AddressRequest request) {
        address.setLabel(request.getLabel());
        address.setFullName(request.getFullName());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setPhone(request.getPhone());
        address.setDefault(request.isDefault());
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}