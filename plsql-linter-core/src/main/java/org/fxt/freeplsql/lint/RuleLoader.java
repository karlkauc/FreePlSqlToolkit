package org.fxt.freeplsql.lint;

import org.fxt.freeplsql.lint.yaml.YamlRuleLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public final class RuleLoader {

    private RuleLoader() {
    }

    /**
     * Loads built-in rules, optional YAML rules from {@code yamlConfig}, and any rules registered
     * via the {@link ServiceLoader} mechanism (META-INF/services/org.fxt.freeplsql.lint.LintRule).
     */
    public static List<LintRule> loadAll(Path yamlConfig, ClassLoader classLoader) throws IOException {
        var rules = new ArrayList<LintRule>(DefaultRules.all());
        if (yamlConfig != null) {
            rules.addAll(YamlRuleLoader.load(yamlConfig));
        }
        ServiceLoader<LintRule> spi = classLoader != null
                ? ServiceLoader.load(LintRule.class, classLoader)
                : ServiceLoader.load(LintRule.class);
        for (LintRule rule : spi) {
            rules.add(rule);
        }
        return List.copyOf(rules);
    }
}
