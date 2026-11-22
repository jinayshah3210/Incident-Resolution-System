package com.resolveit.strategy.priority;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Derives the affected-users band when the employee does not choose one:
 * 1 = just me, 2 = a few people, 3 = a team or department, 4 = the whole organisation or customers.
 */
@Component
public class AffectedUsersAnalyzer extends KeywordScaleAnalyzer {
    @Override
    protected Map<Integer, List<String>> defineRules() {
        return Map.of(
                4, List.of("all users", "everyone", "entire", "company", "organisation", "organization", "whole office",
                        "customers", "all staff", "company-wide"),
                3, List.of("department", "team", "multiple users", "several users", "whole floor", "users are", "many users",
                        "users cannot", "users can't"),
                2, List.of("some users", "a few", "colleagues", "two of us", "few users", "couple of"));
    }
}
