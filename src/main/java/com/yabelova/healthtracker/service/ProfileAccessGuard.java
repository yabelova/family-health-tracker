package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Проверка доступа пользователя к профилю в момент операции с записью.
 * Гард защищает от записи/чтения через отозванный доступ (визард-маркер
 * может держать profileId, к которому связь уже удалена).
 */
@Component
@RequiredArgsConstructor
public class ProfileAccessGuard {

    private final ProfileRepository profileRepository;

    public void check(Long userId, Integer profileId) {
        if (!profileRepository.isLinked(userId, profileId)) {
            throw new RecordOperationException(RecordOperationException.Error.PROFILE_ACCESS_DENIED);
        }
    }
}
