package com.tlat.service;

import com.tlat.Dto.RoomDto;

import java.util.List;

public interface RoomService {
    void saveRoom(RoomDto roomDto);
    List<RoomDto> findAllRooms();
    RoomDto findRoomById(Long roomId);
    void editRoom(RoomDto updatedRoomDto, Long roomId);
    void deleteRoomById(Long roomId);
}


