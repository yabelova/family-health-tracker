package com.yabelova.healthtracker.repository;

import com.yabelova.healthtracker.domain.User;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@SuppressWarnings("NullableProblems")
public interface UserRepository extends ListCrudRepository<User, Integer> {

    Optional<User> findByTelegramId(Long telegramId);
}
