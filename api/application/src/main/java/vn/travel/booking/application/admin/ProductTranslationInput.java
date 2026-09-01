package vn.travel.booking.application.admin;

import java.util.List;

public record ProductTranslationInput(
        String slug,
        String title,
        String shortDescription,
        List<String> longDescription,
        List<String> whyChooseThis,
        String heroImageAlt,
        String status) {
}
