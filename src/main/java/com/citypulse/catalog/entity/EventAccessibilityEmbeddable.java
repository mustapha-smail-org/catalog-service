package com.citypulse.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class EventAccessibilityEmbeddable {

    @Column(name = "wheelchair_accessible")
    private Boolean wheelchairAccessible;

    @Column(name = "blind_accessible")
    private Boolean blindAccessible;

    @Column(name = "deaf_accessible")
    private Boolean deafAccessible;

    @Column(name = "sign_language", length = 500)
    private String signLanguage;

    @Column(name = "mental_accessibility", length = 500)
    private String mentalAccessibility;
}