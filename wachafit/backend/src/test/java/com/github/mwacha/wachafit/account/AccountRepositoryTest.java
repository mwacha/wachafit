package com.github.mwacha.wachafit.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired AccountRepository repo;

    @Test
    void savesAndFindsByEmail() {
        Account a = new Account();
        a.setName("Maria Admin");
        a.setEmail("maria@teste.com");
        a.setPasswordHash("hash");
        repo.save(a);

        var found = repo.findByEmail("maria@teste.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Maria Admin");
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void findByEmailReturnsEmpty_whenNotFound() {
        assertThat(repo.findByEmail("nao-existe@teste.com")).isEmpty();
    }
}
