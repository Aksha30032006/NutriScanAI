package com.nutriscan;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ScanRepo extends JpaRepository<Scan, String> {
    List<Scan> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Scan> findByIdAndUserId(String id, String userId);
    void deleteByIdAndUserId(String id, String userId);
    void deleteByUserId(String userId);
}
