package datadog.trace.agent.tooling.bytebuddy.matcher;

import static datadog.trace.util.CollectionUtils.tryMakeImmutableSet;

import datadog.trace.api.cache.DDCache;
import datadog.trace.api.cache.DDCaches;
import datadog.trace.api.function.Function;
import datadog.trace.bootstrap.instrumentation.java.concurrent.ExcludeFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.matcher.ElementMatcher;

public class NameMatchers {

  /**
   * Matches a {@link NamedElement} for its exact name.
   *
   * @param name The expected name.
   * @param <T> The type of the matched object.
   * @return An element matcher for a named element's exact name.
   */
  public static <T extends NamedElement> ElementMatcher.Junction<T> named(String name) {
    return deduplicateNamed(name);
  }

  /**
   * Matches a {@link NamedElement} for its name not being in an exclusion set.
   *
   * @param type The type of exclusion to apply
   * @param <T> The type of the matched object.
   * @return An element matcher checking if an element's exact name is a member of a set.
   */
  public static <T extends NamedElement> ElementMatcher.Junction<T> notExcludedByName(
      ExcludeFilter.ExcludeType type) {
    return new NotExcluded<T>(type);
  }

  /**
   * Matches a {@link NamedElement} for its exact name's membership of a set.
   *
   * @param names The expected names.
   * @param <T> The type of the matched object.
   * @return An element matcher checking if an element's exact name is a member of a set.
   */
  public static <T extends NamedElement> ElementMatcher.Junction<T> namedOneOf(String... names) {
    return new OneOf<>(tryMakeImmutableSet(Arrays.asList(names)));
  }

  /**
   * Matches a {@link NamedElement} for its exact name's membership of a set.
   *
   * @param names The expected names.
   * @param <T> The type of the matched object.
   * @return An element matcher checking if an element's exact name is a member of a set.
   */
  public static <T extends NamedElement> ElementMatcher.Junction<T> namedOneOf(
      Collection<String> names) {
    return new OneOf<>(tryMakeImmutableSet(names));
  }

  /**
   * Matches a {@link NamedElement} for its exact name's absence from a set.
   *
   * @param names The expected names.
   * @param <T> The type of the matched object.
   * @return An element matcher checking if an element's exact name is absent from a set.
   */
  public static <T extends NamedElement> ElementMatcher.Junction<T> namedNoneOf(String... names) {
    return new NoneOf<>(tryMakeImmutableSet(Arrays.asList(names)));
  }

  /**
   * Matches a {@link NamedElement} for its name's prefix.
   *
   * @param prefix The expected name's prefix.
   * @param <T> The type of the matched object.
   * @return An element matcher for a named element's name's prefix.
   */
  public static <T extends NamedElement> ElementMatcher.Junction<T> nameStartsWith(String prefix) {
    return new StartsWith<>(prefix);
  }

  /**
   * Matches a {@link NamedElement} for its name's suffix.
   *
   * @param suffix The expected name's suffix.
   * @param <T> The type of the matched object.
   * @return An element matcher for a named element's name's suffix.
   */
  public static <T extends NamedElement> ElementMatcher.Junction<T> nameEndsWith(String suffix) {
    return new EndsWith<>(suffix);
  }

  @SuppressWarnings("rawtypes")
  private static final DDCache<String, Named> namedCache = DDCaches.newFixedSizeCache(512);

  @SuppressWarnings("rawtypes")
  private static final Function<String, Named> newNamedMatcher =
      new Function<String, Named>() {
        @Override
        public Named apply(String input) {
          return new Named(input);
        }
      };

  @SuppressWarnings("unchecked")
  static <T extends NamedElement> ElementMatcher.Junction<T> deduplicateNamed(String name) {
    return namedCache.computeIfAbsent(name, newNamedMatcher);
  }

  static class Named<T extends NamedElement> extends ElementMatcher.Junction.ForNonNullValues<T> {
    final String name;

    Named(String name) {
      this.name = name;
    }

    @Override
    protected boolean doMatch(NamedElement target) {
      return target.getActualName().equals(name);
    }
  }

  static class StartsWith<T extends NamedElement> extends Named<T> {
    StartsWith(String name) {
      super(name);
    }

    @Override
    protected boolean doMatch(NamedElement target) {
      return target.getActualName().startsWith(name);
    }
  }

  static class EndsWith<T extends NamedElement> extends Named<T> {
    EndsWith(String name) {
      super(name);
    }

    @Override
    protected boolean doMatch(NamedElement target) {
      return target.getActualName().endsWith(name);
    }
  }

  static class NotExcluded<T extends NamedElement> extends ElementMatcher.Junction.ForNonNullValues<T> {
    private final ExcludeFilter.ExcludeType excludeType;

    NotExcluded(ExcludeFilter.ExcludeType excludeType) {
      this.excludeType = excludeType;
    }

    @Override
    protected boolean doMatch(NamedElement target) {
      return !ExcludeFilter.exclude(excludeType, target.getActualName());
    }
  }

  static class OneOf<T extends NamedElement> extends ElementMatcher.Junction.ForNonNullValues<T> {
    private final Set<String> names;

    OneOf(Set<String> names) {
      this.names = names;
    }

    @Override
    protected boolean doMatch(NamedElement target) {
      return names.contains(target.getActualName());
    }
  }

  static class NoneOf<T extends NamedElement> extends OneOf<T> {
    NoneOf(Set<String> names) {
      super(names);
    }

    @Override
    protected boolean doMatch(NamedElement target) {
      return !super.doMatch(target);
    }
  }
}
