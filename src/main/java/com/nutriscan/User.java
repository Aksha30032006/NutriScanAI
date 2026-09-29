package com.nutriscan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id public String id;
    public String name;
    @Column(unique = true) public String email;
    @JsonIgnore public String password;
    public String goal = "Weight loss";
    public Integer calTarget = 2000;
    public Integer proteinTarget = 70;
    public Double waterTarget = 2.5;
    public Double height;
    public Double weight;
    public Integer age;
    public Instant createdAt = Instant.now();
}
