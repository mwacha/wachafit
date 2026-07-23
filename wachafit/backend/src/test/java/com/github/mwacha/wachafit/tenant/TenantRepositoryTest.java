package com.github.mwacha.wachafit.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TenantRepositoryTest {

    @Autowired TenantRepository repo;

    @Test
    void savesAndFindsbySlug() {
        Tenant t = new Tenant();
        t.setName("Academia Teste");
        t.setSlug("academia-teste");
        repo.save(t);

        var found = repo.findBySlug("academia-teste");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Academia Teste");
    }

    @Test
    void findBySlugReturnsEmpty_whenNotFound() {
        assertThat(repo.findBySlug("nao-existe")).isEmpty();
    }
}
