package vn.edu.reservationservice.reservation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.reservationservice.reservation.ReservationStatus;

import java.time.LocalDateTime;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long>,
        JpaSpecificationExecutor<ReservationEntity> {

    @Query("""
            select case when count(reservation) > 0 then true else false end
            from ReservationEntity reservation
            where reservation.roomId = :roomId
              and reservation.status = :status
              and :startTime < reservation.endTime
              and :endTime > reservation.startTime
            """)
    boolean existsOverlappingReservation(
            @Param("roomId") Long roomId,
            @Param("status") ReservationStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
