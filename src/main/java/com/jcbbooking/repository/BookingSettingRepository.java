package com.jcbbooking.repository;

import com.jcbbooking.model.BookingSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingSettingRepository extends JpaRepository<BookingSetting, Long> {
    Optional<BookingSetting> findBySettingKey(String settingKey);
}
