package com.rus.rus.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "routine_collections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoutineCollection {

    @Id
    @Column(name = "collection_id")
    private Integer collectionId; // PK

    @Column(name = "title")
    private String title; // 제목

    @Column(name = "sub_title")
    private String subTitle; // 소제목

    @Column(name = "guide")
    private String guide;

}
