package com.yiliiii.project.my_photography_project.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    // "mappedBy = tags" 告诉 JPA：
    // "Photo 实体那边有一个 'tags' 字段, 去那里找 @ManyToMany 的配置"
    @ManyToMany(mappedBy = "tags")
    private Set<Photo> photos = new HashSet<>();

    // --- 构造函数, Getter, Setter ---
    public Tag() {}
    public Tag(String name) { this.name = name; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<Photo> getPhotos() { return photos; }
    public void setPhotos(Set<Photo> photos) { this.photos = photos; }
}