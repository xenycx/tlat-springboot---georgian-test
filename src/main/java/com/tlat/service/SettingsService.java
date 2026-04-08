package com.tlat.service;

import com.tlat.Entity.AppSetting;
import com.tlat.Repository.AppSettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettingsService {

    public static final String KEY_LECTURER_MAX_FILE_SIZE_MB = "lecturer.max.file.size.mb";

    private final AppSettingRepository repository;

    @Value("${resource.lecturer.max-size-bytes:104857600}")
    private long defaultLecturerMaxSizeBytes;

    public SettingsService(AppSettingRepository repository) {
        this.repository = repository;
    }

    /** Seed defaults on startup so the table always has known keys. */
    @PostConstruct
    @Transactional
    public void seedDefaults() {
        upsertIfAbsent(KEY_LECTURER_MAX_FILE_SIZE_MB,
                String.valueOf(defaultLecturerMaxSizeBytes / (1024 * 1024)),
                "ლექტორის მიერ ატვირთული ფაილის მაქსიმალური ზომა MB-ში");
    }

    public String get(String key, String defaultValue) {
        return repository.findById(key)
                .map(AppSetting::getValue)
                .orElse(defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Returns the lecturer max file size in bytes (reads from DB). */
    public long getLecturerMaxFileSizeBytes() {
        long mb = getLong(KEY_LECTURER_MAX_FILE_SIZE_MB, defaultLecturerMaxSizeBytes / (1024 * 1024));
        return mb * 1024L * 1024L;
    }

    @Transactional
    public void update(String key, String value) {
        AppSetting setting = repository.findById(key)
                .orElseThrow(() -> new IllegalArgumentException("Unknown setting key: " + key));
        setting.setValue(value);
        repository.save(setting);
    }

    public List<AppSetting> findAll() {
        return repository.findAll();
    }

    private void upsertIfAbsent(String key, String value, String description) {
        if (!repository.existsById(key)) {
            repository.save(new AppSetting(key, value, description));
        }
    }
}
