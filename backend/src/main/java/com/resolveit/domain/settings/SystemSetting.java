package com.resolveit.domain.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A key/value configuration entry (strategies, SLA minutes, warning percent). */
@Entity
@Table(name = "settings")
public class SystemSetting {
    @Id
    @Column(name = "setting_key", length = 80)
    private String key;
    @Column(name = "setting_value", nullable = false, length = 200)
    private String value;

    protected SystemSetting() { }

    public SystemSetting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public void change(String newValue) { this.value = newValue; }

    public String getKey() { return key; }
    public String getValue() { return value; }
}
