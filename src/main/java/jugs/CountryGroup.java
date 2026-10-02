package jugs;

import java.util.List;

import io.quarkiverse.roq.frontmatter.runtime.model.DocumentPage;

/** A deterministic, renderable group of JUG pages assigned to one country. */
public final class CountryGroup {

    private final String name;
    private final String anchor;
    private final List<DocumentPage> jugs;

    public CountryGroup(String name, String regionAnchor, List<DocumentPage> jugs) {
        this.name = name;
        this.anchor = regionAnchor + "-" + anchorFor(name);
        this.jugs = List.copyOf(jugs);
    }

    public String getName() {
        return name;
    }

    public String getAnchor() {
        return anchor;
    }

    public List<DocumentPage> getJugs() {
        return jugs;
    }

    public int getCount() {
        return jugs.size();
    }

    private static String anchorFor(String country) {
        return country.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
