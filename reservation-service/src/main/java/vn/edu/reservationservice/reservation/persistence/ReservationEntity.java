package vn.edu.reservationservice.reservation.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import vn.edu.reservationservice.reservation.ReservationStatus;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reservations",
        indexes = {
                @Index(
                        name = "idx_reservations_room_status_time",
                        columnList = "room_id,status,start_time,end_time"
                ),
                @Index(
                        name = "idx_reservations_requester_id",
                        columnList = "requester_id"
                )
        }
)
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * External reference to a Room owned by Room Service. It is intentionally
     * not a foreign key to room_db.
     */
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "room_code", nullable = false, length = 50)
    private String roomCode;

    @Column(name = "requester_id", nullable = false, length = 100)
    private String requesterId;

    @Column(name = "requester_name", nullable = false, length = 150)
    private String requesterName;

    @Column(nullable = false, length = 300)
    private String purpose;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "attendee_count", nullable = false)
    private Integer attendeeCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ReservationEntity() {
        // Required by JPA.
    }

    public ReservationEntity(
            Long roomId,
            String roomCode,
            String requesterId,
            String requesterName,
            String purpose,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer attendeeCount,
            ReservationStatus status
    ) {
        this.roomId = roomId;
        this.roomCode = roomCode;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.purpose = purpose;
        this.startTime = startTime;
        this.endTime = endTime;
        this.attendeeCount = attendeeCount;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public String getPurpose() {
        return purpose;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Integer getAttendeeCount() {
        return attendeeCount;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
