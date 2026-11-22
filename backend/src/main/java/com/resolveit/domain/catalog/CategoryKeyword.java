package com.resolveit.domain.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;
import java.util.Objects;

/** A weighted classifier keyword belonging to a category (value object). */
@Embeddable
public class CategoryKeyword {
    @Column(name = "keyword", nullable = false, length = 60)
    private String keyword;
    @Column(name = "weight", nullable = false)
    private int weight;

    protected CategoryKeyword() { }

    public CategoryKeyword(String keyword, int weight) {
        this.keyword = keyword.strip().toLowerCase(Locale.ROOT);
        this.weight = Math.max(1, Math.min(5, weight));
    }

    public String getKeyword() { return keyword; }
    public int getWeight() { return weight; }

    @Override public boolean equals(Object other) {
        return other instanceof CategoryKeyword k && Objects.equals(keyword, k.keyword);
    }

    @Override public int hashCode() { return Objects.hashCode(keyword); }
}
