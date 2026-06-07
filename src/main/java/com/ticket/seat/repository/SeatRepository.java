package com.ticket.seat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ticket.seat.domain.Seat;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long>{

}
