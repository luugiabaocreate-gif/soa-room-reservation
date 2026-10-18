package vn.edu.roomservice.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import vn.edu.roomservice.room.RoomStatus;
import vn.edu.roomservice.room.RoomType;

@Entity
@Table(
        name = "rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rooms_code",
                columnNames = "code"
        )
)
public class RoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String building;

    @Column(nullable = false)
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoomType type;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "total_computers", nullable = false)
    private Integer totalComputers;

    @Column(name = "working_computers", nullable = false)
    private Integer workingComputers;

    @Column(name = "broken_computers", nullable = false)
    private Integer brokenComputers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoomStatus status;

    protected RoomEntity() {
        // Required by JPA.
    }

    public RoomEntity(
            String code,
            String building,
            Integer floor,
            RoomType type,
            Integer capacity,
            Integer totalComputers,
            Integer workingComputers,
            Integer brokenComputers,
            RoomStatus status
    ) {
        update(
                code,
                building,
                floor,
                type,
                capacity,
                totalComputers,
                workingComputers,
                brokenComputers,
                status
        );
    }

    public void update(
            String code,
            String building,
            Integer floor,
            RoomType type,
            Integer capacity,
            Integer totalComputers,
            Integer workingComputers,
            Integer brokenComputers,
            RoomStatus status
    ) {
        this.code = code;
        this.building = building;
        this.floor = floor;
        this.type = type;
        this.capacity = capacity;
        this.totalComputers = totalComputers;
        this.workingComputers = workingComputers;
        this.brokenComputers = brokenComputers;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getBuilding() {
        return building;
    }

    public Integer getFloor() {
        return floor;
    }

    public RoomType getType() {
        return type;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public Integer getTotalComputers() {
        return totalComputers;
    }

    public Integer getWorkingComputers() {
        return workingComputers;
    }

    public Integer getBrokenComputers() {
        return brokenComputers;
    }

    public RoomStatus getStatus() {
        return status;
    }
}
