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

    @Convert(converter = StringListConverter.class)
    @Column(length = 2000)
    public List<String> tips = new ArrayList<>();
}
