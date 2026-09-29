package com.nutriscan;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
public class Scan {
    @Id public String id;
    public String userId;
    public String name;
    public String emoji;
    public int calories;
    public int protein;
    public int carbs;
    public int fat;
    public String image;
    public Instant createdAt;
    public boolean ai;
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(length = 500)
    public List<String> tips = new ArrayList<>();
}
