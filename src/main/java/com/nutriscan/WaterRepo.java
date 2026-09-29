package com.nutriscan;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WaterRepo extends JpaRepository<WaterLog, String> {
    List<WaterLog> findByUserIdAndDate(String userId, String date);
    void deleteByUserIdAndDate(String userId, String date);
    void deleteByUserId(String userId);
}
