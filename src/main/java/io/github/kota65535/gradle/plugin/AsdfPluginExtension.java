package io.github.kota65535.gradle.plugin;

import java.util.Map;
import java.util.function.Consumer;

public class AsdfPluginExtension {

  private final Consumer<Map<String, String>> onPatternsSet;
  private Map<String, String> patterns;

  public AsdfPluginExtension(Consumer<Map<String, String>> onPatternsSet) {
    this.onPatternsSet = onPatternsSet;
  }

  public Map<String, String> getPatterns() {
    return patterns;
  }

  public void setPatterns(Map<String, String> patterns) {
    this.patterns = patterns;
    onPatternsSet.accept(patterns);
  }
}
