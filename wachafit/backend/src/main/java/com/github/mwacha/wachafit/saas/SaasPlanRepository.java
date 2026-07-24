package com.github.mwacha.wachafit.saas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SaasPlanRepository extends JpaRepository<SaasPlan, UUID> {
    List<SaasPlan> findByActiveTrueOrderByPriceAsc();
}
