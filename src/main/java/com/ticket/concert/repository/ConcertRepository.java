package com.ticket.concert.repository;

import com.ticket.concert.domain.Concert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, Long>{
    // 페이징 GET concerts/
    // 1페이지 조회용
    List<Concert> findTop11ByOrderByIdDesc();
    // 2페이지 이후 조회용
    List<Concert> findTop11ByIdLessThanOrderByIdDesc(Long concertId);
}
