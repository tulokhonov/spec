package com.example.spec.service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "t1")
@Getter
@Setter
public class Person {

    @Id
    private Long id;

    private String name;

    private int age;

    private BigDecimal score;
}
