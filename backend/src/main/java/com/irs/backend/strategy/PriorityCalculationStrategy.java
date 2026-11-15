package com.irs.backend.strategy;

import com.irs.backend.model.enums.*;

public interface PriorityCalculationStrategy {

    Priority calculate(Impact impact, Urgency urgency);

}