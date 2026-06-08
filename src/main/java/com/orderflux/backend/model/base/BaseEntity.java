package com.orderflux.backend.model.base;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * BaseEntity — Audit fields inherited by ALL entities.
 *
 * @MappedSuperclass:
 *   This class is NOT an entity itself (no table created for it).
 *   Its fields ARE mapped to columns in the child entity's table.
 *   Think of it as a "mixin" for JPA.
 *
 * @EntityListeners(AuditingEntityListener.class):
 *   Registers a Hibernate listener that automatically populates
 *   @CreatedDate and @LastModifiedDate fields before insert/update.
 *   Requires @EnableJpaAuditing on a @Configuration class.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * @CreatedDate: Hibernate sets this ONCE on INSERT.
     *               Never updated after that.
     *
     * updatable = false: Even if someone tries to change it
     *                    via JPA, the SQL UPDATE ignores this column.
     * nullable = false:  This column must always have a value.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * @LastModifiedDate: Hibernate updates this on every UPDATE.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}