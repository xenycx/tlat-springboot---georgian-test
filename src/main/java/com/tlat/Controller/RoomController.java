package com.tlat.Controller;

import com.tlat.Dto.RoomDto;
import com.tlat.Entity.Room;
import com.tlat.service.RoomService;
import com.tlat.Repository.RoomRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Controller
@RequestMapping("/rooms")
public class RoomController {

    private RoomService roomService;
    private RoomRepository roomRepository;

    public RoomController(RoomService roomService, RoomRepository roomRepository) {
        this.roomService = roomService;
        this.roomRepository = roomRepository;
    }

    @GetMapping
    public String listRooms(Model model) {
        List<RoomDto> rooms = roomService.findAllRooms();
        model.addAttribute("rooms", rooms);
        return "room/list";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/add")
    public String showAddRoomForm(Model model) {
        RoomDto room = new RoomDto();
        model.addAttribute("room", room);
        return "room/add";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public String addRoom(@Valid @ModelAttribute("room") RoomDto roomDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("room", roomDto);
            return "room/add";
        }
        roomService.saveRoom(roomDto);
        return "redirect:/rooms";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/edit/{id}")
    public String showEditRoomForm(@PathVariable Long id, Model model) {
        RoomDto room = roomService.findRoomById(id);
        model.addAttribute("room", room);
        return "room/edit";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/edit/{id}")
    public String editRoom(@Valid @ModelAttribute("room") RoomDto roomDto, BindingResult result, @PathVariable Long id, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("room", roomDto);
            return "room/edit";
        }
        try {
            roomService.editRoom(roomDto, id);
        } catch (IllegalArgumentException e) {
            result.rejectValue("ipAddress", "error.room", e.getMessage());
            model.addAttribute("room", roomDto);
            return "room/edit";
        }
        return "redirect:/rooms";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/delete/{id}")
    public String deleteRoom(@PathVariable Long id) {
        roomService.deleteRoomById(id);
        return "redirect:/rooms";
    }

    @GetMapping("/ip")
        @ResponseBody
        public ResponseEntity<String> getLocalIpAddress(HttpServletRequest request) {
            String ipAddress = extractIpAddress(request);
            return ResponseEntity.ok(ipAddress);
        }

        private String extractIpAddress(HttpServletRequest request) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isEmpty()) {
                // Get first IP in case of multiple proxies
                return forwardedFor.split(",")[0].trim();
            }
            
            // Check other common headers
            String[] headerNames = {
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_REAL_IP"
            };
            
            for (String headerName : headerNames) {
                String header = request.getHeader(headerName);
                if (header != null && !header.isEmpty() && !"unknown".equalsIgnoreCase(header)) {
                    return header;
                }
            }
            
            // Fallback to remote address
            String remoteAddr = request.getRemoteAddr();
            // Handle localhost IPv6
            if ("0:0:0:0:0:0:0:1".equals(remoteAddr)) {
                remoteAddr = "127.0.0.1";
            }
            
            return remoteAddr;
        }

        @GetMapping("/find-by-ip")
        @ResponseBody
        public ResponseEntity<Room> findRoomByIp(@RequestParam String ip) {
            Room room = roomRepository.findByIpAddress(ip);
            if (room != null) {
                return ResponseEntity.ok(room);
            }
            return ResponseEntity.notFound().build();
        }
}