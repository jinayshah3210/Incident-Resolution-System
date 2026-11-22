package com.resolveit.domain.incident;

/** Incident priority bands, P1 being the most urgent. */
public enum Priority {
    P1("Critical"), P2("High"), P3("Medium"), P4("Low");

    private final String label;

    Priority(String label) { this.label = label; }

    public String getLabel() { return label; }

    /** One level more urgent (P1 stays P1). Used when a correlated major incident is suspected. */
    public Priority higher() { return this == P1 ? P1 : values()[ordinal() - 1]; }

    public boolean isAtLeast(Priority other) { return ordinal() <= other.ordinal(); }
}
