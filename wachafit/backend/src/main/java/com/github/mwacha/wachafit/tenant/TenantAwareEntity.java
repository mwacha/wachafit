package com.github.mwacha.wachafit.tenant;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.util.UUID;

@MappedSuperclass
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantAwareEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @PrePersist
    protected void prePersist() {
        if (this.tenantId == null) {
            UUID ctx = TenantContext.get();
            if (ctx == null) throw new IllegalStateException(
                "TenantContext não definido ao persistir " + getClass().getSimpleName());
            this.tenantId = ctx;
        }
    }

    public UUID getTenantId()            { return tenantId; }
    public void setTenantId(UUID id)     { this.tenantId = id; }
}
