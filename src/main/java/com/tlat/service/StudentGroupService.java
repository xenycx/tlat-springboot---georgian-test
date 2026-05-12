package com.tlat.service;

import com.tlat.dto.StudentGroupDto;

import java.util.List;

public interface StudentGroupService {
    void saveGroup(StudentGroupDto groupDto);
    List<StudentGroupDto> findAllGroups();
    StudentGroupDto findGroupById(Long groupId);
    void editGroup(StudentGroupDto groupDto, Long groupId);
    void deleteGroupById(Long groupId);
}
