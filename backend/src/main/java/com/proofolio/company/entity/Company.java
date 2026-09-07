package com.proofolio.company.entity;

import com.proofolio.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(length = 200)
    private String industry;

    @Column(length = 500)
    private String website;

    @Column(name = "talent_profile", columnDefinition = "text")
    private String talentProfile;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "core_values", columnDefinition = "text[]", nullable = false)
    private List<String> coreValues = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tech_stack", columnDefinition = "text[]", nullable = false)
    private List<String> techStack = new ArrayList<>();

    @Column(name = "hiring_process", columnDefinition = "text")
    private String hiringProcess;

    @Column(columnDefinition = "text")
    private String notes;

    public Company(UUID ownerId, String name) {
        super(ownerId);
        this.name = name;
    }
}
