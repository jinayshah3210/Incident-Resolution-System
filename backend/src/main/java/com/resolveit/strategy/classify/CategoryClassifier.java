package com.resolveit.strategy.classify;

import com.resolveit.domain.catalog.Category;
import com.resolveit.strategy.NamedStrategy;
import java.util.Collection;

/** Strategy pattern: suggests an incident category from its title and description. */
public interface CategoryClassifier extends NamedStrategy {
    ClassificationResult classify(String title, String description, Collection<Category> categories);
}
