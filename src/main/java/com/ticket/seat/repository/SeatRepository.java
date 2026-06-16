package com.ticket.seat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ticket.seat.domain.Seat;

import jakarta.persistence.LockModeType;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long>{
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Seat s where s.id = :id")
	Optional<Seat> findWithLockById(@Param("id") Long id);

	List<Seat> findByScheduleId(Long scheduleId);
}
