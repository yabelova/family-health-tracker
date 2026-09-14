package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.SymptomLog;
import com.yabelova.healthtracker.domain.SymptomLogProperties;
import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.repository.SymptomLogRepository;
import com.yabelova.healthtracker.util.TimeZones;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SymptomService {

    private final SymptomLogRepository repository;
    private final ProfileAccessGuard accessGuard;

    public SymptomLog save(Integer profileId, Integer createdBy, SymptomLogProperties properties) {
        accessGuard.check(createdBy, profileId);
        SymptomLog log = SymptomLog.builder()
                .profileId(profileId)
                .createdBy(createdBy)
                .createdAt(Instant.now())
                .properties(properties)
                .build();
        return repository.save(log);
    }

    public List<SymptomLog> listByProfile(Integer userId, Integer profileId) {
        accessGuard.check(userId, profileId);
        return repository.findByProfileId(profileId);
    }

    public List<SymptomLog> listLastSevenDays(Integer userId, Integer profileId) {
        accessGuard.check(userId, profileId);
        return repository.findByProfileIdAndSymptomTimeSince(profileId,
                LocalDate.now(TimeZones.DEFAULT).minusDays(7).atStartOfDay());
    }

    public void delete(Integer userId, Integer profileId, Integer id) {
        accessGuard.check(userId, profileId);
        SymptomLog log = repository.findById(id)
                .orElseThrow(() -> new RecordOperationException(RecordOperationException.Error.RECORD_NOT_FOUND));
        if (!log.getProfileId().equals(profileId)) {
            throw new RecordOperationException(RecordOperationException.Error.RECORD_NOT_LINKED);
        }
        repository.deleteById(id);
    }
}
