package com.sybyl.trace.masterdata;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdditionalExpenseLabelRepository extends JpaRepository<AdditionalExpenseLabel, Long> {
  List<AdditionalExpenseLabel> findAllByActiveTrueOrderByNameAsc();
  boolean existsByNameIgnoreCase(String name);
  Optional<AdditionalExpenseLabel> findByNameIgnoreCase(String name);
  
  @Query("select l from AdditionalExpenseLabel l order by l.name asc")
  List<AdditionalExpenseLabel> findAllForAdmin();        // includes inactive

  @Query("select l from AdditionalExpenseLabel l where l.active=true order by l.name asc")
  List<AdditionalExpenseLabel> findAllActive(); 
}
