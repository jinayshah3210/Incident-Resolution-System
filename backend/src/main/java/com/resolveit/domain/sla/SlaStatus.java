package com.resolveit.domain.sla;

import com.resolveit.exception.ValidationException;
import java.util.Locale;

/** Overall SLA health of an incident, used for badges and the SLA search filter. */
public enum SlaStatus {
    OK, AT_RISK, VIOLATED;

    public static SlaStatus parse(String raw) {
        String value = raw.strip().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return SlaStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw ValidationException.field("slaStatus", "SLA status must be ok, at-risk or violated.");
        }
    }
}
