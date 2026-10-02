package jugs;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkiverse.roq.frontmatter.runtime.model.DocumentPage;
import io.quarkiverse.roq.frontmatter.runtime.model.Paginator;
import io.quarkiverse.roq.frontmatter.runtime.model.RoqCollection;
import io.quarkus.qute.TemplateExtension;

/**
 * Template helpers for the jugs collection.
 *
 * <p>Roq collections have no built-in sort, and the scan order is not guaranteed alphabetical,
 * so we sort by a front-matter field here. The paginator's page count and URLs are derived from
 * the collection size (order-independent), so pairing it with {@link #paged} is safe.
 */
@TemplateExtension
public class JugExtensions {

    private static String field(DocumentPage page, String key) {
        Object value = page.data(key);
        return value == null ? "" : value.toString();
    }

    /**
     * Liquid {@code sort:} semantics — used by the map ({@code founded_date}) and search ({@code name}) pages.
     * Case-SENSITIVE (Ruby's byte-wise {@code <=>}, so "JUG"/"JVM" sort before "Java"), empty values first,
     * with the source filename as a stable tie-break. Jekyll's collection order is by filename, so an empty
     * {@code founded_date} falls back to filename order — exactly as the live map renders it.
     */
    static List<DocumentPage> sortBy(RoqCollection collection, String key) {
        return collection.stream()
                .sorted(Comparator.comparing((DocumentPage page) -> field(page, key))
                        .thenComparing(DocumentPage::baseFileName))
                .toList();
    }

    /**
     * The slice of documents for the current paginator page. Ordered by name CASE-INSENSITIVELY to match
     * jekyll-paginate-v2 (which differs from Liquid's case-sensitive {@code sort:} used elsewhere).
     */
    static List<DocumentPage> paged(RoqCollection collection, Paginator paginator) {
        int from = (paginator.currentIndex() - 1) * paginator.limit();
        return collection.stream()
                .sorted(Comparator.comparing((DocumentPage page) -> field(page, "name"), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(DocumentPage::baseFileName))
                .skip(from).limit(paginator.limit()).toList();
    }

    /** Groups JUGs by normalized country region in a stable display order. */
    static List<RegionGroup> groupByRegion(RoqCollection collection) {
        Map<String, Map<String, List<DocumentPage>>> grouped = new LinkedHashMap<>();
        collection.forEach(page -> {
            String region = RegionClassifier.regionForOverrideOrCountry(field(page, "region"), field(page, "country"));
            String country = RegionClassifier.normalizeCountry(field(page, "country"));
            if (country.isEmpty()) {
                country = "Unknown";
            }
            grouped.computeIfAbsent(region, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(country, ignored -> new java.util.ArrayList<>())
                    .add(page);
        });

        return grouped.keySet().stream()
                .sorted(Comparator.comparingInt(RegionClassifier::regionIndex)
                        .thenComparing(String.CASE_INSENSITIVE_ORDER))
                .map(region -> new RegionGroup(region, grouped.get(region).keySet().stream()
                        .sorted(Comparator.comparingInt((String country) -> grouped.get(region).get(country).size()).reversed()
                                .thenComparing(String.CASE_INSENSITIVE_ORDER))
                        .map(country -> new CountryGroup(country, RegionClassifier.regionAnchor(region), grouped.get(region).get(country).stream()
                                .sorted(Comparator.comparing((DocumentPage page) -> field(page, "name"), String.CASE_INSENSITIVE_ORDER)
                                        .thenComparing(DocumentPage::baseFileName))
                                .toList()))
                        .toList()))
                .toList();
    }

    /** Sorts region cards by JUG count, with the stable region order as a tie-breaker. */
    static List<RegionGroup> sortRegionsByCount(List<RegionGroup> regions) {
        return regions.stream()
                .sorted(Comparator.comparingInt(RegionGroup::getCount).reversed()
                        .thenComparingInt(region -> RegionClassifier.regionIndex(region.getName()))
                        .thenComparing(RegionGroup::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** First component of the "location" field (used as-is by the map, matching the original template). */
    static String lng(DocumentPage page) {
        return coord(page, 0);
    }

    /** Second component of the "location" field. */
    static String lat(DocumentPage page) {
        return coord(page, 1);
    }

    private static String coord(DocumentPage page, int index) {
        // Mirror Jekyll's `location | strip | split: ","`: strip the whole value, split, keep each part
        // verbatim (the element after the comma retains its leading space, matching the live output byte-for-byte).
        String[] parts = field(page, "location").strip().split(",");
        return index < parts.length ? parts[index] : "";
    }
}
