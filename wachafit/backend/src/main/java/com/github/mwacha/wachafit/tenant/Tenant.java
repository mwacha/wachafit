package com.github.mwacha.wachafit.tenant;

import jakarta.persistence.*;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = UUID.class)
)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60, unique = true)
    private String slug;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false, columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    public UUID getId()              { return id; }
    public String getName()          { return name; }
    public void setName(String n)    { this.name = n; }
    public String getSlug()          { return slug; }
    public void setSlug(String s)    { this.slug = s; }
    public boolean isActive()        { return active; }
    public void setActive(boolean a) { this.active = a; }
    public Instant getCreatedAt()    { return createdAt; }
}
