package io.github.kota65535.gradle.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * <p>A {@link Plugin} which adds some project visualization report tasks to a project.</p>
 *
 * @see org.gradle.api.plugins.ProjectReportsPlugin
 * @see <a
 * href="https://github.com/gradle/gradle/blob/master/subprojects/diagnostics/src/main/java/org/gradle/api/plugins/ProjectReportsPlugin.java">ProjectReportsPlugin.java</a>
 */
public class AsdfParsePlugin implements Plugin<Project> {

  public static final String EXTENSION_NAME = "asdfParse";
  public static final String TOOL_VERSIONS = ".tool-versions";

  // cf. https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
  public static final String DEFAULT_PATTERN = "^(?<majorVersion>0|[1-9]\\d*)\\.(?<minorVersion>0|[1-9]\\d*)\\.(?<patchVersion>0|[1-9]\\d*)(?:-(?<preRelease>(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+(?<buildMetadata>[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";

  @Override
  public void apply(final Project project) {
    File versionsFile = getToolVersionsFile(project.getProjectDir(), project.getRootDir());
    if (versionsFile == null) {
      throw new GradleException(".tool-versions file not found in this project.");
    }
    Map<String, String> versions = getVersions(versionsFile);
    setExtraProperties(versions, Collections.emptyMap(), project);

    Consumer<Map<String, String>> onPatternsSet = (Map<String, String> patterns) -> setExtraProperties(versions, patterns, project);
    project.getExtensions().create(EXTENSION_NAME, AsdfParsePluginExtension.class, onPatternsSet);
  }

  private File getToolVersionsFile(File dir, File rootDir) {
    File f = new File(dir, TOOL_VERSIONS);
    if (f.exists()) {
      return f;
    } else if (dir.equals(rootDir)) {
      return null;
    } else {
      return getToolVersionsFile(dir.getParentFile(), rootDir);
    }
  }

  private Map<String, String> getVersions(File file) {
    Map<String, String> ret = new HashMap<>();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }
        String[] tokens = line.strip().split(" ");
        if (tokens.length != 2) {
          throw new GradleException("invalid format of line '%s'".formatted(line));
        }
        ret.put(tokens[0], tokens[1]);
      }
    } catch (FileNotFoundException e) {
      throw new GradleException("file not found", e);
    } catch (IOException e) {
      throw new GradleException("file cannot read", e);
    }
    return ret;
  }

  private void setExtraProperties(Map<String, String> versions, Map<String, String> patterns, Project project) {
    Map<String, String> variables = new HashMap<>();
    for (Entry<String, String> v : versions.entrySet()) {
      String patternStr = patterns.entrySet().stream()
          .filter(e -> e.getKey().equals(v.getKey()))
          .map(Entry::getValue)
          .findFirst()
          .orElse(Optional.ofNullable(patterns.get("*")).orElse(DEFAULT_PATTERN));
      List<String> groupNames = getGroupNames(patternStr);
      Pattern pattern = Pattern.compile(patternStr);
      Matcher matcher = pattern.matcher(v.getValue());
      if (!matcher.find()) {
        project.getLogger().info("No pattern matches. tool: {}, version: {}, pattern: {}", v.getKey(), v.getValue(), patternStr);
        continue;
      }
      variables.put(v.getKey() + "Version", matcher.group());
      for (String gn : groupNames) {
        String name = v.getKey() + capitalize(gn);
        String value = matcher.group(gn);
        if (value != null) {
          variables.put(name, value);
        }
      }
    }
    variables.entrySet().stream()
        .sorted(Entry.comparingByKey())
        .filter(v -> project.getExtensions().findByName(v.getKey()) == null)
        .forEach(v -> {
          project.getLogger().info("{} -> {}", v.getKey(), v.getValue());
          project.getExtensions().add(v.getKey(), v.getValue());
        });
  }

  private List<String> getGroupNames(String regex) {
    Pattern pattern = Pattern.compile("<([a-zA-Z][a-zA-Z0-9]*)>");
    Matcher matcher = pattern.matcher(regex);
    List<String> ret = new ArrayList<>();
    while (matcher.find()) {
      ret.add(matcher.group(1));
    }
    return ret;
  }

  private String capitalize(String str) {
    if (str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }
}
