package com.rus.rus.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "routines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Routine {

    @Id
    @Column(name = "rid")
    private Integer rid; // PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // FK to categorys

    @Column(name = "content")
    private String content; // 루틴 내용
}
