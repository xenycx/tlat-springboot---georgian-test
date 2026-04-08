package com.tlat.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto
{
    private Long id;
    
    private String avatarPath;

    private Long groupId;

    private String groupCode;
    
    private transient MultipartFile avatarFile;

    @NotEmpty(message = "ზედმეტსახელი არ უნდა იყოს შეუვსებელი")
    private String username;

    @NotBlank(message = "სახელი არ უნდა იყოს შეუვსებელი")
    @Size(min = 2, max = 50, message = "სახელი უნდა შედგებოდეს არანაკლებ 2 ასოსგან და არაუმეტეს 50 ასოსგან")
    private String firstName;

    @NotEmpty(message = "გვარი არ უნდა იყოს შეუვსებელი")
    private String lastName;

    @NotEmpty(message = "მეილი არ უნდა იყოს შეუვსებელი")
    @Email
    private String email;

    @NotNull(message = "პაროლი არ უნდა იყოს შეუვსებელი")
    private String password;

    @Positive(message = "ასაკი უნდა იყოს დადებითი რიცხვი")
    @Max(value = 200, message = "ასაკი არ უნდა აცდეს 200-ს")
    private Integer age;

    private String phone;

    @NotEmpty(message="სქესი არ უნდა იყოს შეუვსებელი")
    @Pattern(regexp = "^(Male|Female)$", message = "სქესი უნდა იყოს ან კაცი ან ქალი")
    private String gender;

    @NotBlank(message = "მისამართი არ უნდა იყოს შეუვსებელი")
    @Size(min = 5, max = 100, message = "მისამართი უნდა შედგებოდეს არანაკლებ 5 ასოსგან და არაუმეტეს 100 ასოსგან")
    private String address;

	
	private String role;
	
	// Computed property to get full name (for compatibility with User entity)
	public String getName() {
	    return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
	}
}
