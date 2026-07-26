package com.github.mwacha.wachafit.auth;

import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.account.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PasswordResetTokenRepositoryTest {

    @Autowired PasswordResetTokenRepository repo;
    @Autowired AccountRepository accountRepository;

    @Test
    void savesAndFindsByToken() {
        Account account = new Account();
        account.setName("Pessoa Teste");
        account.setEmail("reset" + UUID.randomUUID() + "@teste.com");
        account.setPasswordHash("hash");
        account = accountRepository.save(account);

        PasswordResetToken t = new PasswordResetToken();
        t.setAccount(account);
        t.setToken(UUID.randomUUID().toString());
        t.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        repo.save(t);

        var found = repo.findByToken(t.getToken());
        assertThat(found).isPresent();
        assertThat(found.get().getAccount().getId()).isEqualTo(account.getId());
        assertThat(found.get().isUsed()).isFalse();
    }
}
