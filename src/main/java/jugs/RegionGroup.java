package jugs;

import java.util.List;

/** A deterministic, renderable group of country groups assigned to one region. */
public final class RegionGroup {


    private final String name;
    private final String anchor;
    private final List<CountryGroup> countries;

    public RegionGroup(String name, List<CountryGroup> countries) {
        this.name = name;
        this.anchor = RegionClassifier.regionAnchor(name);
        this.countries = List.copyOf(countries);
    }

    public String getName() {
        return name;
    }

    public String getAnchor() {
        return anchor;
    }

    public List<CountryGroup> getCountries() {
        return countries;
    }

    public int getCount() {
        return countries.stream().mapToInt(CountryGroup::getCount).sum();
    }

}
