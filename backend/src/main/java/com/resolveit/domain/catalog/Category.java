package com.resolveit.domain.catalog;

import com.resolveit.exception.ValidationException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * An incident category stored as data, not as an enum, so administrators can add categories
 * and classifier keywords at runtime without code changes.
 */
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 60)
    private String name;
    @Column(length = 300)
    private String description;
    @Column(nullable = false)
    private boolean active = true;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "category_keywords", joinColumns = @JoinColumn(name = "category_id"))
    private Set<CategoryKeyword> keywords = new HashSet<>();

    protected Category() { }

    public Category(String name, String description) {
        rename(name);
        this.description = description;
    }

    public void rename(String newName) {
        String clean = newName == null ? "" : newName.strip();
        if (clean.length() < 3 || clean.length() > 60) throw ValidationException.field("name", "Category name must have 3-60 characters.");
        this.name = clean;
    }

    public void describe(String newDescription) { this.description = newDescription == null ? null : newDescription.strip(); }

    public void deactivate() { active = false; }
    public void activate() { active = true; }

    /** Replaces the keyword dictionary; weights are clamped to 1-5. */
    public void replaceKeywords(Map<String, Integer> weights) {
        Set<CategoryKeyword> next = new HashSet<>();
        weights.forEach((word, weight) -> {
            if (word != null && !word.isBlank()) next.add(new CategoryKeyword(word, weight == null ? 1 : weight));
        });
        keywords.clear();
        keywords.addAll(next);
    }

    public Map<String, Integer> keywordWeights() {
        Map<String, Integer> result = new LinkedHashMap<>();
        keywords.stream().sorted((a, b) -> a.getKeyword().compareTo(b.getKeyword()))
                .forEach(k -> result.put(k.getKeyword(), k.getWeight()));
        return result;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public Set<CategoryKeyword> getKeywords() { return Collections.unmodifiableSet(keywords); }
}
