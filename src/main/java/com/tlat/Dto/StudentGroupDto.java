package com.tlat.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentGroupDto {

    private Long id;

    @NotBlank(message = "ჯგუფის კოდი არ უნდა იყოს ცარიელი")
    private String code;
}
