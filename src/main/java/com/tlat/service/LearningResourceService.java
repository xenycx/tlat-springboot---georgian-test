package com.tlat.service;

import com.tlat.Dto.LectureDto;
import com.tlat.Dto.ResourceFormDto;
import com.tlat.Entity.LearningResource;
import com.tlat.Entity.ResourceAuditLog;
import com.tlat.Entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface LearningResourceService {

    List<LectureDto> getSelectableLectures(User user);

    List<LearningResource> getManageResources(User user);

    List<LearningResource> getVisibleResourcesForStudent(User user);

    record SimpleGroupInfo(Long id, String code) {}
    List<SimpleGroupInfo> getGroupsForLecture(Long lectureId, User user);

    LearningResource getEditableResource(Long resourceId, User user);

    void createResource(ResourceFormDto form, MultipartFile file, User actor);

    void updateResource(Long resourceId, ResourceFormDto form, MultipartFile file, User actor);

    void setPublished(Long resourceId, boolean published, User actor);

    void deleteResource(Long resourceId, User actor);

    ResourceDownloadPayload getDownloadPayload(Long resourceId, User user);

    List<ResourceAuditLog> getAuditLogs(User user);

    record ResourceDownloadPayload(Path filePath, String originalFilename, String contentType) {
    }
}
