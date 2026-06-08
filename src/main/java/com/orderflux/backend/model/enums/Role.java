package com.orderflux.backend.model.enums;

/**
 * User roles for Role-Based Access Control (RBAC).
 *
 * Why an enum over a String?
 * - Compile-time safety: Role.ADMINN won't compile. "ADMINN" string silently fails.
 * - IDE autocomplete: no typos possible
 * - Refactoring safe: rename in one place
 *
 * Why a separate enum file over inner enum?
 * - Reused across User entity, JWT claims, Security config
 * - Cleaner imports
 */
public enum Role {
	Role_Customer, //Regular Buyer
	Role_Admin,    //Full System access
	Role_Seller    //can manage own product(may be in future)
}
