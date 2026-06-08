package com.orderflux.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.orderflux.backend.model.User;
import com.orderflux.backend.model.enums.Role;

/**
 * UserRepository — Data access layer for User entity.
 *
 * @Repository:
 *   1. Marks this as a Spring Bean (component scan picks it up)
 *   2. Enables Spring's exception translation:
 *      Raw JDBC/Hibernate exceptions → Spring's DataAccessException hierarchy
 *      e.g., ConstraintViolationException → DataIntegrityViolationException
 *   Technically optional when extending JpaRepository (Spring Data
 *   adds it implicitly), but ALWAYS include it for clarity.
 *
 * JpaRepository<User, Long>:
 *   First type param:  the Entity class
 *   Second type param: the type of the Primary Key (@Id field)
 *
 * Free methods from JpaRepository:
 *   save(entity)           → INSERT or UPDATE
 *   findById(id)           → SELECT by PK → Optional<T>
 *   findAll()              → SELECT * (dangerous on large tables!)
 *   deleteById(id)         → DELETE by PK
 *   existsById(id)         → SELECT COUNT > 0
 *   count()                → SELECT COUNT(*)
 */
@Repository
public interface UserRepository extends JpaRepository<User,Long> {
	/**
     * Spring Data Query Method — Derived Queries.
     *
     * Spring parses the method name and generates SQL automatically:
     * findBy     → SELECT WHERE
     * Email      → column: email
     * Result:    SELECT * FROM users WHERE email = ?
     *
     * Why Optional<User> instead of User?
     *   Optional forces the caller to handle the case where no user exists.
     *   Returning User directly would return null → NullPointerException risk.
     *   Optional<T> is the modern Java way to handle "might not exist."
     */
	
	Optional<User> findByEmail(String email);
	
	// checks phone number is duplicate or not
	boolean existsByPhoneNumber(String phoneNumber);
	
	/**
     * existsBy: generates SELECT COUNT(*) > 0 WHERE email = ?
     * More efficient than findByEmail() when you only need to check existence.
     * Don't fetch the entire User object just to check if it exists.
     */
	
	boolean existsByEmail(String email);
	
	/**
     * Derived query with multiple conditions:
     * findBy + Email + And + IsLocked
     * → SELECT * FROM users WHERE email = ? AND is_locked = ?
     */
	
	Optional<User> findByEmailAndIsLocked (String email,Boolean isLocked);
	
	/**
     * Custom JPQL Query.
     *
     * @Query: When derived queries get complex, write JPQL explicitly.
     *
     * JPQL vs SQL:
     *   SQL:   SELECT * FROM users WHERE role = 'ROLE_ADMIN'
     *   JPQL:  SELECT u FROM User u WHERE u.role = :role
     *          ↑ uses CLASS name and FIELD names, not table/column names
     *
     * @Param("role"): binds method parameter to :role placeholder.
     *                 Prevents SQL injection — always use named params.
     *
     * nativeQuery = true: would use raw SQL instead of JPQL.
     *   Use only when JPQL can't express what you need.
     *   Ties you to a specific database dialect.
     */
	@Query("SELECT u FROM User u WHERE u.role=:role AND u.isEnabled=true")
	List<User> findActiveUserByRole(@Param("role")Role role);
}
