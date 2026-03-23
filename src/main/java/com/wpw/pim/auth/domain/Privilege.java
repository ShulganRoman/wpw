package com.wpw.pim.auth.domain;

/**
 * System privileges that can be assigned to roles.
 * Stored as VARCHAR in the database for readability.
 */
public enum Privilege {
    BULK_IMPORT,
    CREATE_ROLES,
    DELETE_ROLES,
    MODIFY_ROLES,
    MODIFY_PRODUCTS,
    BULK_EXPORT,
    MANAGE_CATALOG
}
