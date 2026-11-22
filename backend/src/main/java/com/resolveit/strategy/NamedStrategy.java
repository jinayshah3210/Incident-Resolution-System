package com.resolveit.strategy;

/** Common contract of every runtime-selectable strategy: a stable key stored in the settings table. */
public interface NamedStrategy {
    String key();
    String displayName();
}
