# Welcome
Welcome to the [**Worldwide list of Java User Groups** (JUGs)](https://world-wide-jugs.github.io/GlobalWWJugs/) !

This website it is based in 100% opensource technologies, and it is dynamically rendered based on the data located in the different folders saved in this repository.

We strongly invite you to improve this website by submitting a PR to include any of the JUGs we have available around the world or by providing new functionalities for the end users of this website.

# Setup
This website is based on [Roq](https://docs.quarkiverse.io/quarkus-roq/dev/index.html), the [Quarkus](https://quarkus.io) static site generator. To build it you need a Java 21 (or later) environment. Maven does not need to be installed separately — the project ships with the Maven Wrapper (`./mvnw`).

# Running in local
Once you have Java 21 available, you can launch the site locally in dev mode with:
```
./mvnw quarkus:dev
```
This will start a development server on `http://localhost:8080`. Once it is loaded, you can open it with your favourite browser:
 - http://localhost:8080/GlobalWWJugs/

The content you can see, it will be updated every time you make changes in your local files.

# Building the static site
To generate the static website (the same output that is published to GitHub Pages) run:
```
./mvnw package -Dquarkus.roq.generator.batch=true
java -jar target/quarkus-app/quarkus-run.jar
```
The generated site is written to `target/roq`.

# JUGS Map, Regions & Directory
Visit [the main page](https://world-wide-jugs.github.io/GlobalWWJugs/), browse the [map](https://world-wide-jugs.github.io/GlobalWWJugs/map.html), or view the [regional directory](https://world-wide-jugs.github.io/GlobalWWJugs/regions.html).

The regional directory normalizes common country aliases such as `USA`, `UK`, and `The Netherlands`, then assigns each JUG to a deterministic geographic region. The canonical country values are stored directly in the JUG frontmatter. The country-to-region mapping is maintained in `src/main/resources/jugs/country-regions.properties`; new country values must be added there when new JUG records are introduced. Unknown or worldwide entries are retained under `Other / Unassigned` rather than being omitted.

## Country and region data

- Use the canonical country name from `country-regions.properties`; aliases such as `USA`, `UK`, `UAE`, `DO`, `Ivory Coast`, `Macedonia`, `Czech Republic`, and `The Netherlands` are normalized to their canonical values.
- A JUG needs `name`, `country`, `website`, and `location`. `region` is optional and should only be used for an explicit exception; otherwise the region is derived from `country`.
- Regions are displayed in this order: Africa, Asia, Europe, Middle East, North America, Oceania, South America, and Other / Unassigned.
- Country cards are grouped within regions and ordered by JUG count. Keep country values consistent so grouping and counts remain correct.

# Adding a JUG
For adding a new JUG, submit a PR with a new `.md` file in `content/jugs` containing the required fields (`name`, `country`, `website`, and `location`). Check the existing files in `content/jugs` for optional properties such as social links, calendar, meetup, and `region` overrides.

For locating your JUG properly in the map, it is recommended opening [the map](https://world-wide-jugs.github.io/GlobalWWJugs/map.html) with your browser, go to its console and click in the place you desire. You should see the coordinates you can use for storing it in your `.md` file. Alternatively you can use [latlong.net](https://www.latlong.net/) or any other latitude and longitude finder.

#  Communication Channels
Feel free to reach out using one of the following channels:
  
* [Official mailing list](https://jugs.groups.io/g/jug-leaders) ( jug-leaders@jugs.groups.io )
* [Slack](https://jugleaders.slack.com)

# Other
* [Q&A](qa.md)
* Newsletter
* Track speakers
* Code of conduct
