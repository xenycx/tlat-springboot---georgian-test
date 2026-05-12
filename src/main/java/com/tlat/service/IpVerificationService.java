package com.tlat.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;
import com.tlat.repository.RoomRepository;
import com.tlat.entity.Room;

@Service
public class IpVerificationService {
    
    @Autowired
    private RoomRepository roomRepository;
    
    public boolean verifyIpAddress(String roomNumber, HttpServletRequest request) {
        Room room = roomRepository.findByRoomNumber(roomNumber);
        if (room == null) {
            throw new RuntimeException("ოთახი ვერ მოიძებნა");
        }
            
        String clientIp = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(clientIp)) {
            clientIp = "127.0.0.1";
        }
        return room.getIpAddress().equals(clientIp);
    }
}