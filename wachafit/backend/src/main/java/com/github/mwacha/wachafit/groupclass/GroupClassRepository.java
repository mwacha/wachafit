package com.github.mwacha.wachafit.groupclass;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface GroupClassRepository extends JpaRepository<GroupClass, UUID> {
    List<GroupClass> findByActive(boolean active);
}
