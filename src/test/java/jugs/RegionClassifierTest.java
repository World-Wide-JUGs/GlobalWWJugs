package jugs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class RegionClassifierTest {

    @Test
    void normalizesLegacyCountryAliases() {
        assertEquals("United States", RegionClassifier.normalizeCountry("USA"));
        assertEquals("United Kingdom", RegionClassifier.normalizeCountry("UK"));
        assertEquals("Netherlands", RegionClassifier.normalizeCountry("The Netherlands"));
        assertEquals("Czechia", RegionClassifier.normalizeCountry("Czech Republic"));
        assertEquals("Dominican Republic", RegionClassifier.normalizeCountry("DO"));
        assertEquals("North Macedonia", RegionClassifier.normalizeCountry("Macedonia"));
        assertEquals("United Arab Emirates", RegionClassifier.normalizeCountry("UAE"));
        assertEquals("Côte d'Ivoire", RegionClassifier.normalizeCountry("Ivory Coast"));
    }

    @Test
    void classifiesCountriesIntoStableRegions() {
        assertEquals("North America", RegionClassifier.regionForCountry("USA"));
        assertEquals("North America", RegionClassifier.regionForCountry("Canada"));
        assertEquals("North America", RegionClassifier.regionForCountry("Mexico"));
        assertEquals("Central America", RegionClassifier.regionForCountry("Costa Rica"));
        assertEquals("Caribbean", RegionClassifier.regionForCountry("Dominican Republic"));
        assertEquals("Europe", RegionClassifier.regionForCountry("Germany"));
        assertEquals("Middle East", RegionClassifier.regionForCountry("United Arab Emirates"));
        assertEquals("Other / Unassigned", RegionClassifier.regionForCountry("Worldwide"));
        assertEquals("Other / Unassigned", RegionClassifier.regionForCountry("Unknown Country"));
    }

    @Test
    void exposesDeterministicRegionOrder() {
        assertEquals(List.of(
                "Africa",
                "Asia",
                "Europe",
                "Middle East",
                "North America",
                "Central America",
                "Caribbean",
                "Oceania",
                "South America",
                "Other / Unassigned"), RegionClassifier.regionOrder());
    }

    @Test
    void preservesExplicitRegionOverrides() {
        assertEquals("Europe", RegionClassifier.regionForOverrideOrCountry("Europe", "United States"));
        assertEquals("North America", RegionClassifier.regionForOverrideOrCountry("", "USA"));
    }

    @Test
    void createsStableRegionGroupMetadata() {
        CountryGroup country = new CountryGroup("Worldwide", "other-unassigned", List.of());
        RegionGroup group = new RegionGroup("Other / Unassigned", List.of(country));

        assertEquals("other-unassigned", group.getAnchor());
        assertEquals(0, group.getCount());
        assertEquals("other-unassigned-worldwide", country.getAnchor());
        assertEquals(1, group.getCountries().size());
    }
}
