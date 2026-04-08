package com.tlat.Dto;

import com.tlat.Entity.LearningResourceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceFormDto {

    @NotNull(message = "ლექცია აუცილებელია")
    private Long lectureId;

    @NotBlank(message = "სათაური აუცილებელია")
    private String title;

    private String description;

    @NotNull(message = "კატეგორია აუცილებელია")
    private LearningResourceCategory category = LearningResourceCategory.MATERIAL;

    private java.util.List<Long> targetGroupIds = new java.util.ArrayList<>();

    private boolean published;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime visibleFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime visibleUntil;
}
