package com.tlat.service.Impl;

import org.springframework.stereotype.Service;

import com.tlat.dto.RoomDto;
import com.tlat.entity.Room;
import com.tlat.repository.RoomRepository;
import com.tlat.service.RoomService;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Objects;
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
        Long nonNullRoomId = Objects.requireNonNull(roomId, "roomId is required");
        Optional<Room> roomOptional = roomRepository.findById(nonNullRoomId);
        if(roomOptional.isPresent()){
            return mapToRoomDto(roomOptional.get());
        }
        return null;
    }

    @Override
    public void editRoom(RoomDto updatedRoomDto, Long roomId) {
        Long nonNullRoomId = Objects.requireNonNull(roomId, "roomId is required");
        Room existingRoom = roomRepository.findById(nonNullRoomId)
                .orElseThrow(() -> new EntityNotFoundException("ოთახი ვერ მოიძებნა"));

        // შეამოწმეთ IP მისამართის დუბლირება
        Room roomWithSameIp = roomRepository.findByIpAddress(updatedRoomDto.getIpAddress());
        if (roomWithSameIp != null && !roomWithSameIp.getId().equals(nonNullRoomId)) {
            throw new IllegalArgumentException("IP მისამართი უკვე არსებობს სხვა ოთახისთვის.");
        }

        existingRoom.setRoomNumber(updatedRoomDto.getRoomNumber());
        existingRoom.setIpAddress(updatedRoomDto.getIpAddress());
        roomRepository.save(existingRoom);
    }

    @Override
    public void deleteRoomById(Long roomId) {
        roomRepository.deleteById(Objects.requireNonNull(roomId, "roomId is required"));
    }

    private RoomDto mapToRoomDto(Room room){
        RoomDto roomDto = new RoomDto();
        roomDto.setId(room.getId());
        roomDto.setRoomNumber(room.getRoomNumber());
        roomDto.setIpAddress(room.getIpAddress());
        return roomDto;
    }
}