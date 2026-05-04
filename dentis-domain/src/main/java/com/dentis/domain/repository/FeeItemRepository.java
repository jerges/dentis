package com.dentis.domain.repository;

import com.dentis.domain.entity.FeeItem;
import com.dentis.domain.enums.FeeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeeItemRepository extends JpaRepository<FeeItem, String> {

    Page<FeeItem> findByActiveTrueAndCategory(FeeCategory category, Pageable pageable);

    List<FeeItem> findByActiveTrue();

    @Query("SELECT f FROM FeeItem f WHERE f.active = true AND " +
           "(LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(f.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<FeeItem> searchFeeItems(@Param("search") String search, Pageable pageable);

    boolean existsByCode(String code);
}
