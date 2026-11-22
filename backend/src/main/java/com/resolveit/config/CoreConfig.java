package com.resolveit.config;

import com.resolveit.strategy.StrategyRegistry;
import com.resolveit.strategy.assignment.AssignmentStrategy;
import com.resolveit.strategy.classify.CategoryClassifier;
import com.resolveit.strategy.priority.PriorityStrategy;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the clock and the three strategy registries (Strategy pattern, selected at runtime from settings). */
@Configuration
public class CoreConfig {
    @Bean
    public SwitchableClock clock() { return new SwitchableClock(); }

    @Bean
    public StrategyRegistry<PriorityStrategy> priorityStrategies(List<PriorityStrategy> strategies) {
        return new StrategyRegistry<>("priority", strategies);
    }

    @Bean
    public StrategyRegistry<AssignmentStrategy> assignmentStrategies(List<AssignmentStrategy> strategies) {
        return new StrategyRegistry<>("assignment", strategies);
    }

    @Bean
    public StrategyRegistry<CategoryClassifier> classifiers(List<CategoryClassifier> classifiers) {
        return new StrategyRegistry<>("classifier", classifiers);
    }
}
