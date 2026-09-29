package com.nutriscan;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Review {
    @Id public String id;
    public String name;
    public int rating;
    @Column(length = 500) public String text;
    public Instant createdAt = Instant.now();
}
