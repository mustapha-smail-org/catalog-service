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
public class EventPricingEmbeddable {

    @Column(name = "price_type", length = 100)
    private String priceType;

    @Column(name = "price_detail", columnDefinition = "TEXT")
    private String priceDetail;

    @Column(name = "access_type", length = 100)
    private String accessType;

    @Column(name = "booking_url", length = 2_048)
    private String bookingUrl;

    @Column(name = "booking_link_text", length = 500)
    private String bookingLinkText;
}