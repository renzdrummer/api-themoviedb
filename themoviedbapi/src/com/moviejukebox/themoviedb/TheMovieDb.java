/*
 *      Copyright (c) 2004-2010 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list 
 *  
 *      Web: http://code.google.com/p/moviejukebox/
 *  
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *  
 *      For any reuse or distribution, you must make clear to others the 
 *      license terms of this work.  
 */
package com.moviejukebox.themoviedb;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.moviejukebox.themoviedb.model.MovieDB;
import com.moviejukebox.themoviedb.model.Person;
import com.moviejukebox.themoviedb.tools.DOMHelper;
import com.moviejukebox.themoviedb.tools.DOMParser;
import com.moviejukebox.themoviedb.tools.LogFormatter;
import com.moviejukebox.themoviedb.tools.WebBrowser;

/**
 * This is the main class for the API to connect to TheMovieDb.org The implementation is for v2.1 
 * of the API as detailed here http://api.themoviedb.org/2.1/docs/
 * 
 * @author Stuart.Boston
 * @version 1.3
 */
public class TheMovieDb {

    private String apiKey;
    private static Logger logger;
    private static LogFormatter tmdbFormatter = new LogFormatter();
    private static ConsoleHandler tmdbConsoleHandler = new ConsoleHandler();
    private static final String apiSite = "http://api.themoviedb.org/2.1/";
    private static final String defaultLanguage = "en-US";
    private static final String MOVIE_SEARCH = "Movie.search";
    private static final String MOVIE_BROWSE = "Movie.browse";
    private static final String MOVIE_IMDB_LOOKUP = "Movie.imdbLookup";
    private static final String MOVIE_GET_INFO = "Movie.getInfo";
    private static final String MOVIE_GET_IMAGES = "Movie.getImages";
    private static final String PERSON_GET_VERSION = "Person.getVersion";
    private static final String PERSON_GET_INFO = "Person.getInfo";
    private static final String PERSON_SEARCH = "Person.search";

    public TheMovieDb(String apiKey) {
        setLogger(Logger.getLogger("TheMovieDB"));
        setApiKey(apiKey);
    }

    public TheMovieDb(String apiKey, Logger logger) {
        setLogger(logger);
        setApiKey(apiKey);
    }

    public void setProxy(String host, String port, String username, String password) {
        WebBrowser.setProxyHost(host);
        WebBrowser.setProxyPort(port);
        WebBrowser.setProxyUsername(username);
        WebBrowser.setProxyPassword(password);
    }

    public void setTimeout(int webTimeoutConnect, int webTimeoutRead) {
        WebBrowser.setWebTimeoutConnect(webTimeoutConnect);
        WebBrowser.setWebTimeoutRead(webTimeoutRead);
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        TheMovieDb.logger = logger;
        tmdbConsoleHandler.setFormatter(tmdbFormatter);
        tmdbConsoleHandler.setLevel(Level.FINE);
        logger.addHandler(tmdbConsoleHandler);
        logger.setUseParentHandlers(true);
        logger.setLevel(Level.ALL);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        tmdbFormatter.addApiKey(apiKey);
    }

    /**
     * Build the search URL from the search prefix and movie title.
     * This will change between v2.0 and v2.1 of the API
     * 
     * @param prefix        The search prefix before the movie title
     * @param language      The two digit language code. E.g. en=English            
     * @param searchTerm    The search key to use, e.g. movie title or IMDb ID
     * @return              The search URL
     */
    private String buildSearchUrl(String prefix, String searchTerm, String language) {
        String searchUrl = apiSite + prefix + "/" + language + "/xml/" + apiKey;
        if (prefix.equals(MOVIE_BROWSE)) {
            searchUrl += "?";
        } else {
            searchUrl += "/";
        }
        searchUrl += searchTerm;
        logger.finest("Search URL: " + searchUrl);
        return searchUrl;
    }

    /**
     * Searches the database using the movie title passed
     * 
     * @param movieTitle    The title to search for
     * @param language      The two digit language code. E.g. en=English
     * @return              A movie bean with the data extracted
     */
    public List<MovieDB> moviedbSearch(String movieTitle, String language) {
        MovieDB movie = null;
        List<MovieDB> movieList = new ArrayList<MovieDB>();

        // If the title is null, then exit
        if (!isValidString(movieTitle)) {
            return movieList;
        }

        Document doc = null;

        try {
            String searchUrl = buildSearchUrl(MOVIE_SEARCH, URLEncoder.encode(movieTitle, "UTF-8"), language);
            doc = DOMHelper.getEventDocFromUrl(searchUrl);
            NodeList nlMovies = doc.getElementsByTagName("movie");
            if (nlMovies == null) {
                return movieList;
            }

            for (int loop = 0; loop < nlMovies.getLength(); loop++) {
                Node nMovie = nlMovies.item(loop);
                if (nMovie.getNodeType() == Node.ELEMENT_NODE) {
                    Element eMovie = (Element) nMovie;
                    movie = DOMParser.parseMovieInfo(eMovie);
                    if (movie != null) {
                        movieList.add(movie);
                    }
                }
            }
        } catch (Exception error) {
            logger.severe("TheMovieDb Error: " + error.getMessage());
        }
        return movieList;
    }

    /**
     * Browse the database using the default parameters.
     * http://api.themoviedb.org/2.1/methods/Movie.browse
     *
     * @param orderBy either <code>rating</code>,
     * <code>release</code> or <code>title</code>
     * @param order how results are ordered. Either <code>asc</code> or
     * <code>desc</code>
     * @param language the two digit language code. E.g. en=English
     * @return a list of MovieDB objects
     */
    public List<MovieDB> moviedbBrowse(String orderBy, String order, String language) {
        return this.moviedbBrowse(orderBy, order, new HashMap<String, String>(), language);
    }

    /**
     * Browse the database using optional parameters.
     * http://api.themoviedb.org/2.1/methods/Movie.browse
     *
     * @param orderBy either <code>rating</code>,
     * <code>release</code> or <code>title</code>
     * @param order how results are ordered. Either <code>asc</code> or
     * <code>desc</code>
     * @param parameters a Map of optional parameters. See the complete list
     * in the url above.
     * @param language the two digit language code. E.g. en=English
     * @return a list of MovieDB objects
     */
    public List<MovieDB> moviedbBrowse(String orderBy, String order,
            Map<String, String> parameters, String language) {

        List<String> validParameters = new ArrayList<String>();
        validParameters.add("per_page");
        validParameters.add("page");
        validParameters.add("query");
        validParameters.add("min_votes");
        validParameters.add("rating_min");
        validParameters.add("rating_max");
        validParameters.add("genres");
        validParameters.add("genres_selector");
        validParameters.add("release_min");
        validParameters.add("release_max");
        validParameters.add("year");
        validParameters.add("certifications");
        validParameters.add("companies");
        validParameters.add("countries");

        String url = "order_by=" + orderBy + "&order=" + order;
        for (String key : validParameters) {
            if (parameters.containsKey(key)) {
                url += "&" + key + "=" + parameters.get(key);
            }
        }
        logger.finest("Browse URL : " + url);

        MovieDB movie = null;
        List<MovieDB> movieList = new ArrayList<MovieDB>();
        Document doc = null;

        try {
            String searchUrl = buildSearchUrl(MOVIE_BROWSE, url, language);
            doc = DOMHelper.getEventDocFromUrl(searchUrl);
            NodeList nlMovies = doc.getElementsByTagName("movie");
            if (nlMovies == null) {
                return movieList;
            }

            for (int loop = 0; loop < nlMovies.getLength(); loop++) {
                Node nMovie = nlMovies.item(loop);
                if (nMovie.getNodeType() == Node.ELEMENT_NODE) {
                    Element eMovie = (Element) nMovie;
                    movie = DOMParser.parseMovieInfo(eMovie);
                    if (movie != null) {
                        movieList.add(movie);
                    }
                }
            }
        } catch (Exception error) {
            logger.severe("TheMovieDb Error: " + error.getMessage());
        }
        return movieList;
    }

    /**
     * Searches the database using the IMDb reference
     * 
     * @param imdbID    IMDb reference, must include the "tt" at the start
     * @param language  The two digit language code. E.g. en=English            
     * @return          A movie bean with the data extracted
     */
    public MovieDB moviedbImdbLookup(String imdbID, String language) {
        MovieDB movie = new MovieDB();

        // If the imdbID is null, then exit
        if (!isValidString(imdbID)) {
            return movie;
        }

        Document doc = null;

        try {
            String searchUrl = buildSearchUrl(MOVIE_IMDB_LOOKUP, imdbID, language);

            doc = DOMHelper.getEventDocFromUrl(searchUrl);
            NodeList nlMovies = doc.getElementsByTagName("movie");
            if (nlMovies == null) {
                return movie;
            }

            for (int loop = 0; loop < nlMovies.getLength(); loop++) {
                Node nMovie = nlMovies.item(loop);
                if (nMovie.getNodeType() == Node.ELEMENT_NODE) {
                    Element eMovie = (Element) nMovie;
                    movie = DOMParser.parseMovieInfo(eMovie);
                }
            }
        } catch (Exception error) {
            logger.severe("TheMovieDb Error: " + error.getMessage());
        }
        return movie;
    }

    /**
     * Passes a null MovieDB object to the full function
     * 
     * @param tmdbID    TheMovieDB ID of the movie to get the information for
     * @param language  The two digit language code. E.g. en=English            
     * @return          A movie bean with all of the information
     */
    public MovieDB moviedbGetInfo(String tmdbID, String language) {
        MovieDB movie = null;
        movie = moviedbGetInfo(tmdbID, movie, language);
        return movie;
    }

    /**
     * Gets all the information for a given TheMovieDb ID
     * 
     * @param movie
     *            An existing MovieDB object to populate with the data
     * @param tmdbID
     *            The Movie Db ID for the movie to get information for
     * @param language
     *            The two digit language code. E.g. en=English            
     * @return A movie bean with all of the information
     */
    public MovieDB moviedbGetInfo(String tmdbID, MovieDB movie, String language) {
        // If the tmdbID is invalid, then exit
        if (!isValidString(tmdbID)) {
            return movie;
        }

        Document doc = null;

        try {
            String searchUrl = buildSearchUrl(MOVIE_GET_INFO, tmdbID, language);

            doc = DOMHelper.getEventDocFromUrl(searchUrl);
            if (doc == null && !language.equalsIgnoreCase(defaultLanguage)) {
                logger.fine("Trying to get the default version");
                Thread.dumpStack();
                searchUrl = buildSearchUrl(MOVIE_GET_INFO, tmdbID, defaultLanguage);
            }

            if (doc == null) {
                return movie;
            }

            NodeList nlMovies = doc.getElementsByTagName("movie");
            if (nlMovies == null) {
                return movie;
            }

            for (int loop = 0; loop < nlMovies.getLength(); loop++) {
                Node nMovie = nlMovies.item(loop);
                if (nMovie.getNodeType() == Node.ELEMENT_NODE) {
                    Element eMovie = (Element) nMovie;
                    movie = DOMParser.parseMovieInfo(eMovie);
                }
            }
        } catch (Exception error) {
            logger.severe("Error: " + error.getMessage());
        }
        return movie;
    }

    public MovieDB moviedbGetImages(String searchTerm, String language) {
        MovieDB movie = null;
        movie = moviedbGetInfo(searchTerm, movie, language);
        return movie;
    }

    /**
     * Get all the image information from TheMovieDb.
     * @param searchTerm    Can be either the IMDb ID or TMDb ID
     * @param movie
     * @param language
     * @return
     */
    public MovieDB moviedbGetImages(String searchTerm, MovieDB movie, String language) {
        // If the searchTerm is null, then exit
        if (isValidString(searchTerm)) {
            return movie;
        }

        Document doc = null;

        try {
            String searchUrl = buildSearchUrl(MOVIE_GET_IMAGES, searchTerm, language);

            doc = DOMHelper.getEventDocFromUrl(searchUrl);
            NodeList nlMovies = doc.getElementsByTagName("movie");
            if (nlMovies == null) {
                return movie;
            }

            for (int loop = 0; loop < nlMovies.getLength(); loop++) {
                Node nMovie = nlMovies.item(loop);
                if (nMovie.getNodeType() == Node.ELEMENT_NODE) {
                    Element eMovie = (Element) nMovie;
                    movie = DOMParser.parseMovieInfo(eMovie);
                }
            }

        } catch (Exception error) {
            logger.severe("TheMovieDb Error: " + error.getMessage());
        }

        return movie;
    }

    /**
     * The Person.search method is used to search for an actor, actress or production member.
     * http://api.themoviedb.org/2.1/methods/Person.search
     * 
     * @param personName
     * @param language
     * @return
     */
    public Person personSearch(String personName, String language) {
        Person person = new Person();
        if (!isValidString(personName)) {
            return person;
        }

        Document doc = null;

        try {
            String searchUrl = buildSearchUrl(PERSON_SEARCH, personName, language);
            doc = DOMHelper.getEventDocFromUrl(searchUrl);
            person = DOMParser.parsePersonInfo(doc);
        } catch (Exception error) {
            logger.severe("ERROR: " + error.getMessage());
        }

        return person;
    }

    /**
     * The Person.getInfo method is used to retrieve the full filmography, known movies, 
     * images and things like birthplace for a specific person in the TMDb database.
     * 
     * @param personID
     * @param language
     * @return
     */
    public Person personGetInfo(String personID, String language) {
        Person person = new Person();
        if (!isValidString(personID)) {
            return person;
        }

        Document doc = null;

        try {
            String searchUrl = buildSearchUrl(PERSON_GET_INFO, personID, language);
            doc = DOMHelper.getEventDocFromUrl(searchUrl);
            person = DOMParser.parsePersonInfo(doc);
        } catch (Exception error) {
            logger.severe("ERROR: " + error.getMessage());
        }

        return person;
    }

    /**
     * The Person.getVersion method is used to retrieve the last modified time along with 
     * the current version number of the called object(s). This is useful if you've already 
     * called the object sometime in the past and simply want to do a quick check for updates.
     * 
     * @param personID
     * @param language
     * @return
     */
    public Person personGetVersion(String personID, String language) {
        Person person = new Person();
        if (!isValidString(personID)) {
            return person;
        }

        Document doc = null;

        try {
            String searchUrl = buildSearchUrl(PERSON_GET_VERSION, personID, language);
            doc = DOMHelper.getEventDocFromUrl(searchUrl);
            person = DOMParser.parsePersonGetVersion(doc);
        } catch (Exception error) {
            logger.severe("ERROR: " + error.getMessage());
        }

        return person;
    }

    /**
     * Check the string passed to see if it contains a value.
     * @param testString The string to test
     * @return False if the string is empty, null or UNKNOWN, True otherwise
     */
    private static boolean isValidString(String testString) {
        if (testString == null) {
            return false;
        }

        if (testString.equalsIgnoreCase(MovieDB.UNKNOWN)) {
            return false;
        }

        if (testString.trim().equals("")) {
            return false;
        }

        return true;
    }

    /**
     * Search a list of movies and return the one that matches the title & year
     * @param movieList The list of movies to search
     * @param title     The title to search for
     * @param year      The year of the title to search for
     * @return          The matching movie
     */
    public static MovieDB findMovie(Collection<MovieDB> movieList, String title, String year) {
        if (movieList == null || movieList.isEmpty()) {
            return null;
        }

        for (MovieDB moviedb : movieList) {
            if (compareMovies(moviedb, title, year)) {
                return moviedb;
            }
        }

        return null;
    }

    /**
     * Compare the MovieDB object with a title & year
     * @param moviedb   The moviedb object to compare too
     * @param title     The title of the movie to compare
     * @param year      The year of the movie to compare
     * @return          True if there is a match, False otherwise.
     */
    public static boolean compareMovies(MovieDB moviedb, String title, String year) {
        if (moviedb == null) {
            return false;
        }

        if (!isValidString(title)) {
            return false;
        }

        if (isValidString(year)) {
            if (isValidString(moviedb.getReleaseDate())) {
                // Compare with year
                String movieYear = moviedb.getReleaseDate().substring(0, 4);
                if (moviedb.getTitle().equalsIgnoreCase(title) && movieYear.equals(year)) {
                    return true;
                }
            }
        } else {
            // Compare without year
            if (moviedb.getTitle().equalsIgnoreCase(title)) {
                return true;
            }
        }
        return false;
    }
}
