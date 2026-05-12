package com.tlat.service.Impl;

import com.tlat.dto.LectureDto;
import com.tlat.dto.ResourceFormDto;
import com.tlat.entity.LearningResource;
import com.tlat.entity.LearningResourcePublishStatus;
import com.tlat.entity.Lecture;
import com.tlat.entity.ResourceAuditAction;
import com.tlat.entity.ResourceAuditLog;
import com.tlat.entity.User;
import com.tlat.entity.StudentGroup;
import com.tlat.repository.StudentGroupRepository;
import org.hibernate.Hibernate;
import java.util.stream.Collectors;
import com.tlat.repository.LectureRepository;
import com.tlat.repository.LearningResourceRepository;
import com.tlat.repository.ResourceAuditLogRepository;
import com.tlat.service.LearningResourceService;
import com.tlat.service.LearningResourceStorageService;
import com.tlat.service.LectureService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class LearningResourceServiceImpl implements LearningResourceService {

    private final LearningResourceRepository learningResourceRepository;
    private final ResourceAuditLogRepository resourceAuditLogRepository;
    private final LectureRepository lectureRepository;
    private final LectureService lectureService;
    private final LearningResourceStorageService storageService;
    private final StudentGroupRepository studentGroupRepository;

    @Autowired
    public LearningResourceServiceImpl(LearningResourceRepository learningResourceRepository,
                                       ResourceAuditLogRepository resourceAuditLogRepository,
                                       LectureRepository lectureRepository,
                                       LectureService lectureService,
                                       LearningResourceStorageService storageService,
                                       StudentGroupRepository studentGroupRepository) {
        this.learningResourceRepository = learningResourceRepository;
        this.resourceAuditLogRepository = resourceAuditLogRepository;
        this.lectureRepository = lectureRepository;
        this.lectureService = lectureService;
        this.storageService = storageService;
        this.studentGroupRepository = studentGroupRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LectureDto> getSelectableLectures(User user) {
        if (isAdmin(user)) {
            return lectureService.findAllLectures();
        }
        if (isLecturer(user)) {
            return lectureService.findLecturesByLecturerId(user.getId());
        }
        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningResource> getManageResources(User user) {
        if (isAdmin(user)) {
            return learningResourceRepository.findAllByOrderByCreatedAtDesc();
        }
        if (isLecturer(user)) {
            return learningResourceRepository.findDistinctByLecture_Lecturers_IdOrderByCreatedAtDesc(user.getId());
        }
        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningResource> getVisibleResourcesForStudent(User user) {
        if (!isStudent(user) || user.getStudentGroup() == null) {
            return new ArrayList<>();
        }
        Long groupId = user.getStudentGroup().getId();
        return learningResourceRepository.findVisibleForGroup(
                groupId,
                LearningResourcePublishStatus.PUBLISHED,
                LocalDateTime.now()).stream()
                .filter(r -> r.getTargetGroups().isEmpty() ||
                             r.getTargetGroups().stream().anyMatch(tg -> tg.getId().equals(groupId)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimpleGroupInfo> getGroupsForLecture(Long lectureId, User user) {
        Lecture lecture = getLectureAndValidateOwnership(lectureId, user);
        return lecture.getGroups().stream()
                .map(g -> new SimpleGroupInfo(g.getId(), g.getCode()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LearningResource getEditableResource(Long resourceId, User user) {
        LearningResource resource = getResourceById(resourceId);
        if (!canManageResource(user, resource)) {
            throw new IllegalArgumentException("თქვენ არ გაქვთ ამ ფაილის რედაქტირების უფლება");
        }
        Hibernate.initialize(resource.getTargetGroups());
        return resource;
    }

    @Override
    @Transactional
    public void createResource(ResourceFormDto form, MultipartFile file, User actor) {
        Lecture lecture = getLectureAndValidateOwnership(form.getLectureId(), actor);
        validateVisibility(form.getVisibleFrom(), form.getVisibleUntil());

        try {
            LearningResourceStorageService.StoredFile storedFile = storageService.save(file, actor, isAdmin(actor));

            LearningResource resource = new LearningResource();
            resource.setLecture(lecture);
            resource.setUploadedBy(actor);
            resource.setTitle(form.getTitle().trim());
            
            if (form.getTargetGroupIds() != null && !form.getTargetGroupIds().isEmpty()) {
                resource.setTargetGroups(studentGroupRepository.findAllById(form.getTargetGroupIds()));
            }
            resource.setDescription(form.getDescription());
            resource.setCategory(form.getCategory());
            resource.setPublishStatus(form.isPublished() ? LearningResourcePublishStatus.PUBLISHED : LearningResourcePublishStatus.DRAFT);
            resource.setVisibleFrom(form.getVisibleFrom());
            resource.setVisibleUntil(form.getVisibleUntil());
            resource.setOriginalFilename(storedFile.originalFilename());
            resource.setStoredFilename(storedFile.storedFilename());
            resource.setFileSize(storedFile.fileSize());

            LearningResource saved = learningResourceRepository.save(resource);
            logAudit(saved, actor, ResourceAuditAction.CREATE, "ფაილი აიტვირთა");
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateResource(Long resourceId, ResourceFormDto form, MultipartFile file, User actor) {
        LearningResource resource = getResourceById(resourceId);
        if (!canManageResource(actor, resource)) {
            throw new IllegalArgumentException("თქვენ არ გაქვთ ამ ფაილის რედაქტირების უფლება");
        }

        Lecture lecture = getLectureAndValidateOwnership(form.getLectureId(), actor);
        validateVisibility(form.getVisibleFrom(), form.getVisibleUntil());

        resource.setLecture(lecture);
        resource.setTitle(form.getTitle().trim());
        if (form.getTargetGroupIds() != null && !form.getTargetGroupIds().isEmpty()) {
            resource.setTargetGroups(studentGroupRepository.findAllById(form.getTargetGroupIds()));
        } else {
            resource.getTargetGroups().clear();
        }
        resource.setDescription(form.getDescription());
        resource.setCategory(form.getCategory());
        resource.setPublishStatus(form.isPublished() ? LearningResourcePublishStatus.PUBLISHED : LearningResourcePublishStatus.DRAFT);
        resource.setVisibleFrom(form.getVisibleFrom());
        resource.setVisibleUntil(form.getVisibleUntil());

        if (file != null && !file.isEmpty()) {
            String oldFile = resource.getStoredFilename();
            try {
                LearningResourceStorageService.StoredFile storedFile = storageService.save(file, actor, isAdmin(actor));
                resource.setOriginalFilename(storedFile.originalFilename());
                resource.setStoredFilename(storedFile.storedFilename());
                resource.setFileSize(storedFile.fileSize());
                storageService.delete(oldFile);
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        LearningResource saved = learningResourceRepository.save(resource);
        logAudit(saved, actor, ResourceAuditAction.UPDATE, "ფაილის მონაცემები განახლდა");
    }

    @Override
    @Transactional
    public void setPublished(Long resourceId, boolean published, User actor) {
        LearningResource resource = getResourceById(resourceId);
        if (!canManageResource(actor, resource)) {
            throw new IllegalArgumentException("თქვენ არ გაქვთ ამ ფაილის სტატუსის შეცვლის უფლება");
        }

        resource.setPublishStatus(published ? LearningResourcePublishStatus.PUBLISHED : LearningResourcePublishStatus.DRAFT);
        LearningResource saved = learningResourceRepository.save(resource);
        logAudit(saved, actor, published ? ResourceAuditAction.PUBLISH : ResourceAuditAction.UNPUBLISH,
                published ? "ფაილი გამოქვეყნდა" : "ფაილი გახადა Draft");
    }

    @Override
    @Transactional
    public void deleteResource(Long resourceId, User actor) {
        LearningResource resource = getResourceById(resourceId);
        if (!canDeleteResource(actor, resource)) {
            throw new IllegalArgumentException("თქვენ არ გაქვთ ამ ფაილის წაშლის უფლება");
        }

        logAudit(resource, actor, ResourceAuditAction.DELETE, "ფაილი წაიშალა");
        learningResourceRepository.delete(Objects.requireNonNull(resource));
        storageService.delete(resource.getStoredFilename());
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceDownloadPayload getDownloadPayload(Long resourceId, User user) {
        LearningResource resource = getResourceById(resourceId);
        if (!canViewResource(user, resource)) {
            throw new IllegalArgumentException("თქვენ არ გაქვთ ამ ფაილის ნახვის უფლება");
        }

        Path path = storageService.getPath(resource.getStoredFilename());
        if (!Files.exists(path)) {
            throw new EntityNotFoundException("ფაილი ვერ მოიძებნა საცავში");
        }

        String contentType = storageService.probeContentType(path);
        return new ResourceDownloadPayload(path, resource.getOriginalFilename(), contentType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceAuditLog> getAuditLogs(User user) {
        if (!isAdmin(user)) {
            return new ArrayList<>();
        }
        return resourceAuditLogRepository.findTop200ByOrderByCreatedAtDesc();
    }

    private Lecture getLectureAndValidateOwnership(Long lectureId, User actor) {
        Lecture lecture = lectureRepository.findById(Objects.requireNonNull(lectureId))
                .orElseThrow(() -> new EntityNotFoundException("ლექცია ვერ მოიძებნა"));

        if (isAdmin(actor)) {
            return lecture;
        }

        if (!isLecturer(actor) || lecture.getLecturers().stream().noneMatch(l -> l.getId().equals(actor.getId()))) {
            throw new IllegalArgumentException("თქვენ არ გაქვთ ამ ლექციაზე ფაილების მართვის უფლება");
        }

        return lecture;
    }

    private LearningResource getResourceById(Long resourceId) {
        return learningResourceRepository.findById(Objects.requireNonNull(resourceId))
                .orElseThrow(() -> new EntityNotFoundException("ფაილი ვერ მოიძებნა"));
    }

    private boolean canManageResource(User user, LearningResource resource) {
        return isAdmin(user) || (isLecturer(user) && resource.getLecture().getLecturers().stream().anyMatch(l -> l.getId().equals(user.getId())));
    }

    private boolean canDeleteResource(User user, LearningResource resource) {
        return isAdmin(user) || canManageResource(user, resource);
    }

    private boolean canViewResource(User user, LearningResource resource) {
        if (isAdmin(user)) {
            return true;
        }

        if (isLecturer(user)) {
            return resource.getLecture().getLecturers().stream().anyMatch(l -> l.getId().equals(user.getId()));
        }

        if (!isStudent(user) || user.getStudentGroup() == null) {
            return false;
        }

        if (resource.getPublishStatus() != LearningResourcePublishStatus.PUBLISHED) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (resource.getVisibleFrom() != null && resource.getVisibleFrom().isAfter(now)) {
            return false;
        }
        if (resource.getVisibleUntil() != null && resource.getVisibleUntil().isBefore(now)) {
            return false;
        }

        boolean lectureMatches = resource.getLecture().getGroups().stream()
                .anyMatch(group -> group.getId().equals(user.getStudentGroup().getId()));
                
        if (!lectureMatches) return false;
        if (resource.getTargetGroups().isEmpty()) return true;
        
        return resource.getTargetGroups().stream()
                .anyMatch(tg -> tg.getId().equals(user.getStudentGroup().getId()));
    }

    private void validateVisibility(LocalDateTime visibleFrom, LocalDateTime visibleUntil) {
        if (visibleFrom != null && visibleUntil != null && visibleUntil.isBefore(visibleFrom)) {
            throw new IllegalArgumentException("ხილვადობის დასრულების დრო ვერ იქნება დაწყების დროზე ადრე");
        }
    }

    private void logAudit(LearningResource resource, User actor, ResourceAuditAction action, String details) {
        ResourceAuditLog log = new ResourceAuditLog();
        log.setResourceId(resource.getId());
        log.setResourceTitle(resource.getTitle());
        log.setResourceFilename(resource.getOriginalFilename());
        log.setActor(actor);
        log.setAction(action);
        log.setDetails(details);
        resourceAuditLogRepository.save(log);
    }

    private boolean isAdmin(User user) {
        return hasRole(user, "ROLE_ADMIN");
    }

    private boolean isLecturer(User user) {
        return hasRole(user, "ROLE_LECTURER");
    }

    private boolean isStudent(User user) {
        return hasRole(user, "ROLE_STUDENT");
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals(roleName));
    }
}
