package vn.edu.roomservice.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import vn.edu.roomservice.room.RoomStatus;
import vn.edu.roomservice.room.RoomType;

import java.util.List;

@Component
public class RoomDataInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomDataInitializer.class);

    private final RoomRepository roomRepository;

    public RoomDataInitializer(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (roomRepository.count() > 0) {
            return;
        }

        roomRepository.saveAll(List.of(
                new RoomEntity(
                        "A101",
                        "A",
                        1,
                        RoomType.THEORY,
                        60,
                        0,
                        0,
                        0,
                        RoomStatus.AVAILABLE
                ),
                new RoomEntity(
                        "A301",
                        "A",
                        3,
                        RoomType.LAB,
                        45,
                        45,
                        42,
                        3,
                        RoomStatus.AVAILABLE
                ),
                new RoomEntity(
                        "B201",
                        "B",
                        2,
                        RoomType.THEORY,
                        50,
                        0,
                        0,
                        0,
                        RoomStatus.MAINTENANCE
                ),
                new RoomEntity(
                        "B305",
                        "B",
                        3,
                        RoomType.LAB,
                        40,
                        40,
                        36,
                        4,
                        RoomStatus.LOCKED
                ),
                new RoomEntity(
                        "C102",
                        "C",
                        1,
                        RoomType.LAB,
                        30,
                        30,
                        0,
                        30,
                        RoomStatus.OUT_OF_SERVICE
                )
        ));

        LOGGER.info("Seeded Room database with five local development rooms");
    }
}
