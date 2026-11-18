package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.domain.common.Address;
import jakarta.validation.constraints.NotBlank;

public record AddressDto(String line, @NotBlank String locality, @NotBlank String city) {

    public static AddressDto from(Address address) {
        return new AddressDto(address.line(), address.locality(), address.city());
    }

    public Address toAddress() {
        return new Address(line, locality, city);
    }
}
