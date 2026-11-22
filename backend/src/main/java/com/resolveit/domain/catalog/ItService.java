package com.resolveit.domain.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** A business IT service with a business criticality (1-4). Incidents keep the service name as history. */
@Entity
@Table(name = "services")
public class ItService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 120)
    private String name;
    @Column(nullable = false, length = 600)
    private String description;
    @Column(nullable = false, length = 60)
    private String category;
    @Column(nullable = false)
    private int criticality;
    @Column(length = 500)
    private String keywords;
    @Column(nullable = false)
    private boolean active = true;

    protected ItService() { }

    public ItService(String name, String description, String category, int criticality, String keywords) {
        update(name, description, category, criticality, keywords);
    }

    public void update(String name, String description, String category, int criticality, String keywords) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.criticality = criticality;
        this.keywords = keywords == null ? "" : keywords.strip().toLowerCase(Locale.ROOT);
    }

    public void archive() { active = false; }
    public void reactivate() { active = true; }

    public List<String> keywordList() {
        if (keywords == null || keywords.isBlank()) return List.of();
        return Arrays.stream(keywords.split(",")).map(String::strip).filter(k -> !k.isEmpty()).toList();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public int getCriticality() { return criticality; }
    public String getKeywords() { return keywords; }
    public boolean isActive() { return active; }
}
