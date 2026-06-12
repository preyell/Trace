package com.sybyl.trace.notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sybyl.trace.user.AppUser;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findTop50ByRecipientOrderByCreatedAtDesc(AppUser recipient);

	long countByRecipientAndReadFlagFalse(AppUser recipient);

	void deleteByRecipient(AppUser recipient);

	Page<Notification> findByRecipientOrderByCreatedAtDesc(AppUser recipient, Pageable pageable);

	Page<Notification> findByRecipientAndReadFlagFalseOrderByCreatedAtDesc(AppUser recipient, Pageable pageable);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           delete from Notification n
           where n.readFlag = true
             and n.createdAt < :cutoff
           """)
    int deleteOldReadBefore(@Param("cutoff") Instant cutoff);
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
	       update Notification n
	          set n.readFlag = true
	        where n.recipient = :recipient
	          and n.readFlag = false
	       """)
	int markAllAsReadByRecipient(@Param("recipient") AppUser recipient);
	
	
	Optional<Notification> findByIdAndRecipient(Long id, AppUser recipient);
}