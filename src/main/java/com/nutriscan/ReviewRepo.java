package com.nutriscan;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepo extends JpaRepository<Review, String> {
    List<Review> findTop30ByOrderByCreatedAtDesc();
}
