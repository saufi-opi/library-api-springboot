package com.saufi.library_api.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleEnum {
    ADMIN("ADMIN"),
    LIBRARIAN("LIBRARIAN"),
    MEMBER("MEMBER");

    private final String name;
}
