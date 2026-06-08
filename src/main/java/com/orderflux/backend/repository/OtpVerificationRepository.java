package com.orderflux.backend.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.orderflux.backend.model.OtpVerification;
import com.orderflux.backend.model.User;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long>{
	@Query(
			"""
			SELECT o FROM OtpVerification o
			WHERE o.user=:user
			AND o.isUsed=false
			AND o.expiryTime>:now
			ORDER BY o.createdAt DESC
			LIMIT 1
			"""
			)
	
	Optional<OtpVerification> findValidOtpByuser(
			@Param("user") User user,
			@Param("now") LocalDateTime now);
	
	@Modifying(clearAutomatically = true)
	@Query("""
	        UPDATE OtpVerification o
	        SET o.isUsed = true
	        WHERE o.user = :user
	        AND o.isUsed = false
	        """)
	void invalidateAllOtpsForUser(@Param("user") User user);
	
	boolean existsByUserAndIsUsedFalseAndExpiryTimeAfter(
			User user,
			LocalDateTime now
			);
}
