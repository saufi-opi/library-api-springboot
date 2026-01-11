package com.saufi.library_api.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum PermissionEnum {
    // Books permissions
    BOOKS_CREATE("books:create", "Create books"),
    BOOKS_READ("books:read", "Read books"),
    BOOKS_UPDATE("books:update", "Update books"),
    BOOKS_DELETE("books:delete", "Delete books"),

    // Borrows permissions
    BORROWS_CREATE("borrows:create", "Borrow a book"),
    BORROWS_RETURN("borrows:return", "Return a borrowed book"),
    BORROWS_READ("borrows:read", "View own borrow records"),
    BORROWS_READ_ALL("borrows:read_all", "View all borrow records"),

    // Users permissions
    USERS_READ("users:read", "Read users"),
    USERS_MANAGE("users:manage", "Manage users");

    private final String codeName;
    private final String description;

    public static Set<PermissionEnum> getPermissionsByRole(RoleEnum role) {
        return switch (role) {
            case ADMIN -> Set.of(values());
            case LIBRARIAN -> Set.of(
                    BOOKS_CREATE,
                    BOOKS_READ,
                    BOOKS_UPDATE,
                    BOOKS_DELETE,
                    BORROWS_READ,
                    BORROWS_READ_ALL,
                    USERS_READ);
            case MEMBER -> Set.of(
                    BOOKS_READ,
                    BORROWS_CREATE,
                    BORROWS_RETURN,
                    BORROWS_READ);
        };
    }
}
