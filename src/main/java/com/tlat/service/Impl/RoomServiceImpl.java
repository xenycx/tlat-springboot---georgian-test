package com.tlat.service.Impl;

import org.springframework.stereotype.Service;

import com.tlat.Dto.RoomDto;
import com.tlat.Entity.Room;
import com.tlat.Repository.RoomRepository;
import com.tlat.service.RoomService;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {

    private RoomRepository roomRepository;

    public RoomServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public void saveRoom(RoomDto roomDto) {
        Room room = new Room();
        room.setRoomNumber(roomDto.getRoomNumber());
        room.setIpAddress(roomDto.getIpAddress());
        roomRepository.save(room);
    }

    @Override
    public List<RoomDto> findAllRooms() {
        List<Room> rooms = roomRepository.findAll();
        return rooms.stream()
                .map(this::mapToRoomDto)
                .collect(Collectors.toList());
    }

    @Override
    public RoomDto findRoomById(Long roomId) {
        Optional<Room> roomOptional = roomRepository.findById(roomId);
        if(roomOptional.isPresent()){
            return mapToRoomDto(roomOptional.get());
        }
        return null;
    }

    @Override
    public void editRoom(RoomDto updatedRoomDto, Long roomId) {
        Room existingRoom = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("ოთახი ვერ მოიძებნა"));

        // შეამოწმეთ IP მისამართის დუბლირება
        Room roomWithSameIp = roomRepository.findByIpAddress(updatedRoomDto.getIpAddress());
        if (roomWithSameIp != null && !roomWithSameIp.getId().equals(roomId)) {
            throw new IllegalArgumentException("IP მისამართი უკვე არსებობს სხვა ოთახისთვის.");
        }

        existingRoom.setRoomNumber(updatedRoomDto.getRoomNumber());
        existingRoom.setIpAddress(updatedRoomDto.getIpAddress());
        roomRepository.save(existingRoom);
    }

    @Override
    public void deleteRoomById(Long roomId) {
        roomRepository.deleteById(roomId);
    }

    private RoomDto mapToRoomDto(Room room){
        RoomDto roomDto = new RoomDto();
        roomDto.setId(room.getId());
        roomDto.setRoomNumber(room.getRoomNumber());
        roomDto.setIpAddress(room.getIpAddress());
        return roomDto;
    }
}