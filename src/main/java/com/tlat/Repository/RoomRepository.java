package com.tlat.Repository;

import com.tlat.Entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    
    // Spring Data JPA ავტომატურად ქმნის SQL: SELECT * FROM rooms WHERE room_number = ?
    Room findByRoomNumber(String roomNumber);
    
    // Spring Data JPA ავტომატურად ქმნის SQL: SELECT * FROM rooms WHERE ip_address = ?
    Room findByIpAddress(String ipAddress);
}

