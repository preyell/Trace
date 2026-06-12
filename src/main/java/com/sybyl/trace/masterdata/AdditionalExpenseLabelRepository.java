package com.sybyl.trace.masterdata;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdditionalExpenseLabelRepository extends JpaRepository<AdditionalExpenseLabel, Long> {
  List<AdditionalExpenseLabel> findAllByActiveTrueOrderByNameAsc();
  boolean existsByNameIgnoreCase(String name);
  Optional<AdditionalExpenseLabel> findByNameIgnoreCase(String name);
  
  @Query("select l from AdditionalExpenseLabel l order by l.name asc")
  List<AdditionalExpenseLabel> findAllForAdmin();        // includes inactive

  @Query("select l from AdditionalExpenseLabel l where l.active=true order by l.name asc")
  List<AdditionalExpenseLabel> findAllActive(); 
  
//Add this method for pagination and searching
 @Query("SELECT l FROM AdditionalExpenseLabel l " +
        "WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "OR LOWER(l.description) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "ORDER BY l.name ASC")
 Page<AdditionalExpenseLabel> search(@Param("q") String q, Pageable pageable);
}
