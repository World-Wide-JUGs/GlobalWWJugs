package jugs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

final class RegionClassifier {

    private static final String FALLBACK_REGION = "Other / Unassigned";
    private static final List<String> REGION_ORDER = List.of(
            "Africa",
            "Asia",
            "Europe",
            "Middle East",
            "North America",
            "Oceania",
            "South America",
            FALLBACK_REGION);
    private static final Map<String, String> COUNTRY_ALIASES = Map.of(
            "Czech Republic", "Czechia",
            "DO", "Dominican Republic",
            "Ivory Coast", "Côte d'Ivoire",
            "Macedonia", "North Macedonia",
            "The Netherlands", "Netherlands",
            "UAE", "United Arab Emirates",
            "UK", "United Kingdom",
            "USA", "United States");
    private static final Map<String, String> COUNTRY_REGIONS = loadCountryRegions();

    private RegionClassifier() {
    }

    static String normalizeCountry(String country) {
        String normalized = country == null ? "" : country.strip();
        return COUNTRY_ALIASES.getOrDefault(normalized, normalized);
    }

    static String regionForCountry(String country) {
        return COUNTRY_REGIONS.getOrDefault(normalizeCountry(country), FALLBACK_REGION);
    }

    static String regionForOverrideOrCountry(String regionOverride, String country) {
        String normalizedOverride = regionOverride == null ? "" : regionOverride.strip();
        return normalizedOverride.isEmpty() ? regionForCountry(country) : normalizedOverride;
    }

    static List<String> regionOrder() {
        return REGION_ORDER;
    }

    static int regionIndex(String region) {
        int index = REGION_ORDER.indexOf(region);
        return index < 0 ? REGION_ORDER.size() : index;
    }

    static String regionAnchor(String region) {
        return region.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private static Map<String, String> loadCountryRegions() {
        Properties properties = new Properties();
        try (InputStream stream = RegionClassifier.class.getResourceAsStream("/jugs/country-regions.properties")) {
            if (stream == null) {
                throw new IllegalStateException("Missing /jugs/country-regions.properties");
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load country-to-region mapping", e);
        }
        return properties.stringPropertyNames().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(key -> key, properties::getProperty));
    }
}
