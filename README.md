# Welcome
Welcome to the **Worldwide list of Java User Groups** (JUGs) !

This website it is based in 100% opensource technologies, and it is dynamically rendered based on the data located in the different folders saved in this repository.

We strongly invite you to improve this website by submitting a PR to include any of the JUGs we have available around the world or by providing new functionalities for the end users of this website.

# Setup
This website is based on [Jekyll](https://jekyllrb.com). To run it you need to set up a Ruby (minimum 2.4.1) environment.

To set up a Ruby environment we recommend:
* [rbenv](https://github.com/rbenv/rbenv) (Linux and MacOS)
* [RubyInstaller](https://rubyinstaller.org/) or [Chocolatey](https://chocolatey.org/) (Windows)

After you have Ruby installed, install all the required dependencies running:
```
bundle install
```

# Running in local
Once you have the project with all the dependencies installed, you can launch it in local with the following command:
```
bundle exec jekyll serve
```
This will start a jekyll server on `http://127.0.0.1:4000` Once it is loaded, you can open it with your favourite browser.

The content you can see, it will be updated every time you made changes in your local files.

# JUGS Map & Directory
Visit [the main page](index.html)

# Adding a JUG
For adding a new JUG you only need to submit a PR that should include a new `.md` file saved in the `_jugs` folder that should contain all the mandatory information (_name, website_ and _location_ for locating in the map). Check the [the main page](index.html) to see some properties.

For locating your JUG properly in the map, it is recommended opening [the map](map.html) with your browser, go to its console and click in the place you desire. You should see the coordinates you can use for storing it in your `.md` file. Alternatively you can use [latlong.net](https://www.latlong.net/) or any other latitude and longitude finder. 

#  Communication Channels
Feel free to reach out using one of the following channels:
  
* [Official mailing list](https://jugs.groups.io/g/jug-leaders) ( jug-leaders@jugs.groups.io )
* [Slack](https://jugleaders.slack.com)

# Other
* [Q&A](qa.md)
* Newsletter
* Track speakers
* Code of conduct
