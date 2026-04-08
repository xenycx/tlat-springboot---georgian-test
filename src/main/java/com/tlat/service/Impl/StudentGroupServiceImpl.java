package com.tlat.service.Impl;

import com.tlat.Dto.StudentGroupDto;
import com.tlat.Entity.StudentGroup;
import com.tlat.Repository.LectureRepository;
import com.tlat.Repository.StudentGroupRepository;
import com.tlat.Repository.UserRepository;
import com.tlat.service.StudentGroupService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentGroupServiceImpl implements StudentGroupService {

    private final StudentGroupRepository studentGroupRepository;
    private final UserRepository userRepository;
    private final LectureRepository lectureRepository;

    public StudentGroupServiceImpl(StudentGroupRepository studentGroupRepository,
                                   UserRepository userRepository,
                                   LectureRepository lectureRepository) {
        this.studentGroupRepository = studentGroupRepository;
        this.userRepository = userRepository;
        this.lectureRepository = lectureRepository;
    }

    @Override
    public void saveGroup(StudentGroupDto groupDto) {
        StudentGroup existing = studentGroupRepository.findByCode(groupDto.getCode());
        if (existing != null) {
            throw new IllegalArgumentException("ჯგუფი ამ კოდით უკვე არსებობს.");
        }
        StudentGroup group = new StudentGroup();
        group.setCode(groupDto.getCode());
        studentGroupRepository.save(group);
    }

    @Override
    public List<StudentGroupDto> findAllGroups() {
        return studentGroupRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public StudentGroupDto findGroupById(Long groupId) {
        Optional<StudentGroup> groupOptional = studentGroupRepository.findById(Objects.requireNonNull(groupId, "groupId is required"));
        return groupOptional.map(this::mapToDto).orElse(null);
    }

    @Override
    public void editGroup(StudentGroupDto groupDto, Long groupId) {
        Long nonNullGroupId = Objects.requireNonNull(groupId, "groupId is required");
        StudentGroup existingGroup = studentGroupRepository.findById(nonNullGroupId)
                .orElseThrow(() -> new EntityNotFoundException("ჯგუფი ვერ მოიძებნა"));

        StudentGroup groupWithSameCode = studentGroupRepository.findByCode(groupDto.getCode());
        if (groupWithSameCode != null && !groupWithSameCode.getId().equals(nonNullGroupId)) {
            throw new IllegalArgumentException("ჯგუფი ამ კოდით უკვე არსებობს.");
        }

        existingGroup.setCode(groupDto.getCode());
        studentGroupRepository.save(existingGroup);
    }

    @Override
    public void deleteGroupById(Long groupId) {
        Long nonNullGroupId = Objects.requireNonNull(groupId, "groupId is required");
        if (userRepository.countByStudentGroup_Id(nonNullGroupId) > 0 || lectureRepository.countByGroups_Id(nonNullGroupId) > 0) {
            throw new IllegalArgumentException("ჯგუფის წაშლა ვერ მოხერხდა: ჯგუფი მიბმულია სტუდენტებზე ან ლექციებზე.");
        }
        studentGroupRepository.deleteById(nonNullGroupId);
    }

    private StudentGroupDto mapToDto(StudentGroup group) {
        return new StudentGroupDto(group.getId(), group.getCode());
    }
}
