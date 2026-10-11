package jugs;

import java.util.Comparator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkiverse.roq.frontmatter.runtime.model.DocumentPage;
import io.quarkiverse.roq.frontmatter.runtime.model.Paginator;
import io.quarkiverse.roq.frontmatter.runtime.model.RoqCollection;
import io.quarkus.qute.TemplateExtension;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Template helpers for the jugs collection.
 *
 * <p>Roq collections have no built-in sort, and the scan order is not guaranteed alphabetical,
 * so we sort by a front-matter field here. The paginator's page count and URLs are derived from
 * the collection size (order-independent), so pairing it with {@link #paged} is safe.
 */
@TemplateExtension
public class JugExtensions {

    private static final DateTimeFormatter EVENT_LABEL_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final Map<Path, Optional<EventRecord>> NEXT_EVENT_CACHE = new ConcurrentHashMap<>();
    private static final Map<Path, List<EventDetails>> EVENT_HISTORY_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, String> COUNTRY_CODES = Map.ofEntries(
            Map.entry("Albania", "AL"), Map.entry("Angola", "AO"), Map.entry("Argentina", "AR"),
            Map.entry("Armenia", "AM"), Map.entry("Australia", "AU"), Map.entry("Austria", "AT"),
            Map.entry("Bangladesh", "BD"), Map.entry("Belarus", "BY"), Map.entry("Belgium", "BE"),
            Map.entry("Bolivia", "BO"), Map.entry("Brazil", "BR"), Map.entry("Bulgaria", "BG"),
            Map.entry("Canada", "CA"), Map.entry("Chile", "CL"), Map.entry("China", "CN"),
            Map.entry("Colombia", "CO"), Map.entry("Costa Rica", "CR"), Map.entry("Croatia", "HR"),
            Map.entry("Cyprus", "CY"), Map.entry("Czechia", "CZ"), Map.entry("Denmark", "DK"),
            Map.entry("Dominican Republic", "DO"), Map.entry("Ecuador", "EC"), Map.entry("El Salvador", "SV"),
            Map.entry("Finland", "FI"), Map.entry("France", "FR"), Map.entry("Germany", "DE"),
            Map.entry("Ghana", "GH"), Map.entry("Greece", "GR"), Map.entry("Guatemala", "GT"),
            Map.entry("Hong Kong", "HK"), Map.entry("Hungary", "HU"), Map.entry("Iceland", "IS"),
            Map.entry("India", "IN"), Map.entry("Iran", "IR"), Map.entry("Ireland", "IE"),
            Map.entry("Israel", "IL"), Map.entry("Italy", "IT"), Map.entry("Côte d'Ivoire", "CI"),
            Map.entry("Japan", "JP"), Map.entry("Kazakhstan", "KZ"), Map.entry("Kenya", "KE"),
            Map.entry("Latvia", "LV"), Map.entry("Lebanon", "LB"), Map.entry("Lithuania", "LT"),
            Map.entry("Luxembourg", "LU"), Map.entry("Malaysia", "MY"), Map.entry("Mexico", "MX"),
            Map.entry("Morocco", "MA"), Map.entry("Netherlands", "NL"), Map.entry("New Zealand", "NZ"),
            Map.entry("Nicaragua", "NI"), Map.entry("Nigeria", "NG"), Map.entry("North Macedonia", "MK"),
            Map.entry("Norway", "NO"), Map.entry("Pakistan", "PK"), Map.entry("Panama", "PA"),
            Map.entry("Peru", "PE"), Map.entry("Philippines", "PH"), Map.entry("Poland", "PL"),
            Map.entry("Portugal", "PT"), Map.entry("Romania", "RO"), Map.entry("Russia", "RU"),
            Map.entry("Saudi Arabia", "SA"), Map.entry("Serbia", "RS"), Map.entry("Singapore", "SG"),
            Map.entry("South Africa", "ZA"), Map.entry("Spain", "ES"), Map.entry("Sudan", "SD"),
            Map.entry("Sweden", "SE"), Map.entry("Switzerland", "CH"), Map.entry("Taiwan", "TW"),
            Map.entry("Tunisia", "TN"), Map.entry("Turkey", "TR"), Map.entry("Ukraine", "UA"),
            Map.entry("United Arab Emirates", "AE"), Map.entry("United Kingdom", "GB"),
            Map.entry("United States", "US"), Map.entry("Worldwide", "UN"));

    private static String field(DocumentPage page, String key) {
        Object value = page.data(key);
        return value == null ? "" : value.toString();
    }

    /** Return only Markdown JUG records; colocated JSON event histories are collection members too. */
    static RoqCollection jugRecords(RoqCollection collection) {
        return new RoqCollection(collection.collection(), collection.stream()
                .filter(page -> "md".equalsIgnoreCase(page.source().extension()))
                .toList());
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

    /** Resolves stored social handles to safe HTTPS links while preserving stored URLs. */
    static String linkUrl(DocumentPage page, String key) {
        String value = field(page, key).strip();
        if (value.isEmpty()) {
            return "";
        }
        if (value.startsWith("https://") || value.startsWith("http://")) {
            return value;
        }
        String handle = value.startsWith("@") ? value.substring(1).strip() : value;
        if (handle.isEmpty()) {
            return "";
        }
        return switch (key) {
            case "twitter" -> "https://twitter.com/" + handle;
            case "bluesky" -> "https://bsky.app/profile/" + handle;
            case "facebook" -> "https://www.facebook.com/" + handle;
            case "github" -> "https://github.com/" + handle;
            case "instagram" -> "https://www.instagram.com/" + handle;
            case "linkedin" -> "https://www.linkedin.com/" + (handle.contains("/") ? handle : "company/" + handle);
            case "telegram" -> "https://t.me/" + handle;
            case "twitch" -> "https://www.twitch.tv/" + handle;
            case "youtube" -> "https://www.youtube.com/@" + handle;
            case "kktix" -> handle.contains(".kktix.cc") ? "https://" + handle + (handle.endsWith("/") ? "" : "/") : "https://" + handle + ".kktix.cc/";
            case "mastodon" -> mastodonUrl(handle);
            default -> "";
        };
    }

    private static String mastodonUrl(String value) {
        int separator = value.lastIndexOf('@');
        return separator > 0 && separator < value.length() - 1
                ? "https://" + value.substring(separator + 1) + "/@" + value.substring(0, separator)
                : "";
    }

    /** Returns the Unicode country flag for a normalized country value. */
    static String countryFlag(DocumentPage page) {
        String code = COUNTRY_CODES.get(RegionClassifier.normalizeCountry(field(page, "country")));
        if (code == null) {
            return "🌍";
        }
        if (code.equals("UN")) {
            return "🌐";
        }
        return code.chars()
                .mapToObj(letter -> String.valueOf(Character.toChars(0x1F1E6 + letter - 'A')))
                .reduce("", String::concat);
    }

    /** Link to the complete Markdown record in the repository. */
    static String sourceUrl(DocumentPage page) {
        String filename = page.baseFileName();
        if (!filename.endsWith(".md")) {
            filename += ".md";
        }
        return "https://github.com/World-Wide-JUGs/GlobalWWJugs/blob/master/content/jugs/" + filename;
    }

    /** Returns the next historical event at render time, rather than a persisted next_event field. */
    static String nextEventLabel(DocumentPage page) {
        return nextEvent(page).map(event -> "NEXT EVENT · " + EVENT_LABEL_DATE.format(event.start())).orElse("");
    }

    /** Returns the source event URL for the next event, when one is available. */
    static String nextEventUrl(DocumentPage page) {
        return nextEvent(page).map(EventRecord::url).orElse("");
    }

    static boolean hasNextEvent(DocumentPage page) {
        return nextEvent(page).isPresent();
    }

    /** Returns the complete persisted event history, newest event first. */
    static List<EventDetails> eventHistory(DocumentPage page) {
        return EVENT_HISTORY_CACHE.computeIfAbsent(eventFile(page), JugExtensions::readEventHistory);
    }

    static boolean hasEventHistory(DocumentPage page) {
        return !eventHistory(page).isEmpty();
    }

    private static Optional<EventRecord> nextEvent(DocumentPage page) {
        return NEXT_EVENT_CACHE.computeIfAbsent(eventFile(page), JugExtensions::readNextEvent);
    }

    private static Path eventFile(DocumentPage page) {
        String configuredEventList = field(page, "event_list").strip();
        return configuredEventList.isEmpty()
                ? Path.of(page.sourcePath()).toAbsolutePath().normalize()
                        .resolveSibling(page.baseFileName() + "_events.json")
                : Path.of(configuredEventList).toAbsolutePath().normalize();
    }

    private static Optional<EventRecord> readNextEvent(Path eventFile) {
        if (!Files.isRegularFile(eventFile)) {
            return Optional.empty();
        }
        try {
            JsonArray events = new JsonObject(Files.readString(eventFile)).getJsonArray("events", new JsonArray());
            Instant now = Instant.now();
            return events.stream()
                    .map(value -> eventRecord((JsonObject) value))
                    .flatMap(Optional::stream)
                    .filter(event -> event.start().toInstant().isAfter(now))
                    .min(java.util.Comparator.comparing(event -> event.start().toInstant()));
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<EventRecord> eventRecord(JsonObject event) {
        String start = event.getString("start", "");
        if (start.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new EventRecord(OffsetDateTime.parse(start), event.getString("url", "")));
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    private static List<EventDetails> readEventHistory(Path eventFile) {
        if (!Files.isRegularFile(eventFile)) {
            return List.of();
        }
        try {
            JsonArray events = new JsonObject(Files.readString(eventFile)).getJsonArray("events", new JsonArray());
            List<EventDetails> history = new ArrayList<>();
            for (Object value : events) {
                JsonObject event = (JsonObject) value;
                eventRecord(event).ifPresent(record -> history.add(new EventDetails(
                        EVENT_LABEL_DATE.format(record.start()),
                        event.getString("title", "Untitled event"),
                        event.getString("location", ""),
                        record.url())));
            }
            return history.reversed();
        } catch (IOException | RuntimeException ignored) {
            return List.of();
        }
    }

    private record EventRecord(OffsetDateTime start, String url) {
    }

    public static final class EventDetails {
        private final String date;
        private final String title;
        private final String location;
        private final String url;

        private EventDetails(String date, String title, String location, String url) {
            this.date = date;
            this.title = title;
            this.location = location;
            this.url = url;
        }

        public String getDate() { return date; }
        public String getTitle() { return title; }
        public String getLocation() { return location; }
        public String getUrl() { return url; }
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
