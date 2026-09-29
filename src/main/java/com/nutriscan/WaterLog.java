package com.nutriscan;

import jakarta.persistence.*;

@Entity
public class WaterLog {
    @Id public String id;
    public String userId;
    @Column(name = "log_date") public String date;
    public int ml;
}
