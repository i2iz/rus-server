package com.rus.rus.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "titles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Title {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // int 타입의 PK이므로 auto_increment 설정
    @Column(name = "title_id", nullable = false)
    private Integer titleId; // PK: int 타입

    @Column(name = "value", length = 255, nullable = false) // 칭호 이름, varchar
    private String value;
}
