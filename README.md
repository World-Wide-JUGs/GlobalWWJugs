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

# JUGS Map & Directory
Visit [the main page](https://world-wide-jugs.github.io/GlobalWWJugs/)

# Adding a JUG
For adding a new JUG you only need to submit a PR that should include a new `.md` file saved in the `content/jugs` folder that should contain all the mandatory information (_name, website_ and _location_ for locating in the map). Check the existing files in `content/jugs` to see the available properties.

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
