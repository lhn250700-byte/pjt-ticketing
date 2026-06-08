package com.ticket.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ticket.reservation.dto.MakeReservationRequest;
import com.ticket.reservation.repository.ReservationRepository;
import com.ticket.reservation.service.ReservationService;
import com.ticket.seat.domain.Seat;
import com.ticket.seat.repository.SeatRepository;

@SpringBootTest
public class ReservationServiceTest {
	@Autowired
	private ReservationService reservationService;
	@Autowired
	private SeatRepository seatRepository;
	@Autowired
	private ReservationRepository reservationRepository;
	
	@Test
	void 동시_100명_예매시_1명만_성공() throws InterruptedException {
		int threadCount = 100;
		Long targetScheduleId = 1L;
		Long targetSeatId = 1L;
		ExecutorService executorService = Executors.newFixedThreadPool(32);
		CountDownLatch latch = new CountDownLatch(threadCount);
		
		for (int i = 0; i < threadCount; i++) {
			Long userId = (long) i + 1;
			MakeReservationRequest request = new MakeReservationRequest(userId, targetScheduleId, targetSeatId);
			executorService.submit(() -> {
				try {
					reservationService.makeReservation(request);
				} catch (Exception e) {
					System.out.println("예매 실패(정상): " + e.getMessage());
				} finally {
					latch.countDown();
				}
			});
		}
		
		latch.await();
		executorService.shutdown();
		
		Seat seat = seatRepository.findWithLockById(targetSeatId)
	            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Seat Id 입니다."));
		assertTrue(seat.getIsReserved(), "좌석은 최종적으로 예매된 상태(true)여야 합니다.");
		
		long reservationCount = reservationRepository.count();
		assertEquals(1, reservationCount, "동시 요청은 100건이었지만 예매 내역은 딱 1개만 생성되어야 합니다.");
	}
}
