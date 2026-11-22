package com.resolveit.strategy;

import com.resolveit.exception.ValidationException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Strategy pattern registry: maps the setting value chosen by the administrator to a strategy bean.
 * Adding a new strategy only needs a new Spring bean; no if/else chains change.
 */
public class StrategyRegistry<T extends NamedStrategy> {
    private final String kind;
    private final Map<String, T> strategies = new LinkedHashMap<>();

    public StrategyRegistry(String kind, Collection<? extends T> beans) {
        this.kind = kind;
        for (T bean : beans) {
            T previous = strategies.put(bean.key().toUpperCase(Locale.ROOT), bean);
            if (previous != null) throw new IllegalStateException("Duplicate " + kind + " strategy key " + bean.key());
        }
    }

    public T get(String key) {
        T strategy = key == null ? null : strategies.get(key.strip().toUpperCase(Locale.ROOT));
        if (strategy == null) throw ValidationException.field(kind, "Unknown " + kind + " strategy: " + key);
        return strategy;
    }

    public boolean supports(String key) { return key != null && strategies.containsKey(key.strip().toUpperCase(Locale.ROOT)); }

    public List<String> keys() { return List.copyOf(strategies.keySet()); }

    public Map<String, String> describe() {
        Map<String, String> result = new LinkedHashMap<>();
        strategies.forEach((key, strategy) -> result.put(key, strategy.displayName()));
        return result;
    }
}
