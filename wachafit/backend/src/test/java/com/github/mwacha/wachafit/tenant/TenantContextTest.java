package com.github.mwacha.wachafit.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void cleanup() { TenantContext.clear(); }

    @Test
    void setAndGet_returnsSameTenantId() {
        UUID id = UUID.randomUUID();
        TenantContext.set(id);
        assertThat(TenantContext.get()).isEqualTo(id);
    }

    @Test
    void clear_removesValue() {
        TenantContext.set(UUID.randomUUID());
        TenantContext.clear();
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void defaultValueIsNull() {
        assertThat(TenantContext.get()).isNull();
    }
}
