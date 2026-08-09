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
public class EventLocationEmbeddable {

    @Column(name = "venue_name", length = 500)
    private String name;

    @Column(name = "street", length = 500)
    private String street;

    @Column(name = "zipcode", length = 20)
    private String zipcode;

    @Column(name = "city", length = 150)
    private String city;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;
}