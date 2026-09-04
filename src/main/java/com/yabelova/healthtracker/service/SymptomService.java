package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.SymptomLog;
import com.yabelova.healthtracker.domain.SymptomLogProperties;
import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.repository.SymptomLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SymptomService {

    private final SymptomLogRepository repository;
    private final ProfileAccessGuard accessGuard;

    public SymptomLog save(Integer profileId, Long createdBy, SymptomLogProperties properties) {
        accessGuard.check(createdBy, profileId);
        SymptomLog log = SymptomLog.builder()
                .profileId(profileId)
                .createdBy(createdBy)
                .createdAt(Instant.now())
                .properties(properties)
                .build();
        return repository.save(log);
    }

    public List<SymptomLog> listByProfile(Long userId, Integer profileId) {
        accessGuard.check(userId, profileId);
        return repository.findByProfileId(profileId);
    }

    public List<SymptomLog> listLastSevenDays(Long userId, Integer profileId) {
        accessGuard.check(userId, profileId);
        return repository.findByProfileIdAndCreatedAtAfter(profileId, Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS));
    }

    public void delete(Long userId, Integer profileId, Integer id) {
        accessGuard.check(userId, profileId);
        SymptomLog log = repository.findById(id)
                .orElseThrow(() -> new RecordOperationException(RecordOperationException.Error.RECORD_NOT_FOUND));
        if (!log.getProfileId().equals(profileId)) {
            throw new RecordOperationException(RecordOperationException.Error.RECORD_NOT_LINKED);
        }
        repository.deleteById(id);
    }
}
