package com.tlat.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomDto {
    private Long id;

    @NotBlank(message = "ოთახის ნომერი არ უნდა იყოს ცარიელი")
    private String roomNumber;

    @NotBlank(message = "IP მისამართი არ უნდა იყოს ცარიელი")
    @Pattern(regexp = "^\\d{1,3}(\\.\\d{1,3}){3}$", message = "IP მისამართის ფორმატი არასწორია")
    private String ipAddress;
}

