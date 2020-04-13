package advisor;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class Model {
    private final String REDIRECT_URI;
    private final String CLIENT_ID;
    private final String CLIENT_SECRET;

    public Model(String REDIRECT_URI, String CLIENT_ID, String CLIENT_SECRET) {
        this.REDIRECT_URI = REDIRECT_URI;
        this.CLIENT_ID = CLIENT_ID;
        this.CLIENT_SECRET = CLIENT_SECRET;
    }

    private String authorizationServerPath = "https://accounts.spotify.com";
    private String apiServerPath = "https://api.spotify.com";

    private HttpClient client = HttpClient.newHttpClient();
    private HttpServer server;
    private String authorizationCode;
    private String accessToken;
    private String refreshToken;

    private Map<Command, String> commandEndpoints = Map.of(
            Command.NEW, "/v1/browse/new-releases",
            Command.CATEGORIES, "/v1/browse/categories",
            Command.FEATURED, "/v1/browse/featured-playlists",
            Command.PLAYLISTS, "/v1/browse/categories/%s/playlists");

    private Map<String, String> categoryIds = new HashMap<>();


    public void setAuthorizationServerPath(String authorizationServerPath) {
        this.authorizationServerPath = authorizationServerPath;
    }

    public void setApiServerPath(String apiServerPath) {
        this.apiServerPath = apiServerPath;
    }

    String authLink() {
        return String.format("%s/authorize?client_id=%s&redirect_uri=%s&response_type=code",
                authorizationServerPath, CLIENT_ID, REDIRECT_URI);
    }

    void waitForCode() throws InterruptedException {
        while (authorizationCode == null) {
            Thread.sleep(10);
        }
    }

    void listenForAuth() throws IOException {
        setupServer();
        server.start();
    }

    void stopListening() {
        if (server != null) {
            server.stop(1);
        }
    }

    boolean requestAccessToken() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create(authorizationServerPath + "/api/token"))
                .POST(HttpRequest.BodyPublishers.ofString(String.format(
                        "grant_type=authorization_code" +
                                "&code=%s" +
                                "&redirect_uri=%s" +
                                "&client_id=%s" +
                                "&client_secret=%s",
                        authorizationCode, REDIRECT_URI, CLIENT_ID, CLIENT_SECRET)))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            var jo = JsonParser.parseString(response.body()).getAsJsonObject();
            accessToken = jo.get("access_token").getAsString();
            refreshToken = jo.get("refresh_token").getAsString();
            return true;
        }
        return false;
    }

    boolean isAuthorized() {
        return accessToken != null && refreshToken != null;
    }

    List<String> requestData(Command command) throws IOException, InterruptedException {
        URI uri;
        if (command == Command.PLAYLISTS) {
            if (!categoryIds.containsKey(command.arg)) {
                requestData(Command.CATEGORIES);
            }
            var id = categoryIds.get(command.arg);
            if (id == null) {
                return Collections.singletonList("Unknown category name");
            }
            uri = URI.create(apiServerPath + String.format(commandEndpoints.get(command), id));
        } else {
            uri = URI.create(apiServerPath + commandEndpoints.get(command));
        }
        var request = HttpRequest.newBuilder()
                .header("Authorization", "Bearer" + accessToken)
                .uri(uri)
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        var jo = JsonParser.parseString(response.body()).getAsJsonObject();

        if (response.statusCode() == 200) {
            var list = new ArrayList<String>();
            switch (command) {
                case PLAYLISTS:
                case FEATURED:
                    jo.get("playlists").getAsJsonObject()
                            .get("items").getAsJsonArray().forEach(playlistEl -> {

                        var playlist = playlistEl.getAsJsonObject();
                        var playlistName = playlist.get("name").getAsString();
                        var spotifyURL = playlist.get("external_urls").getAsJsonObject().get("spotify").getAsString();

                        var res = playlistName +
                                System.lineSeparator() +
                                spotifyURL +
                                System.lineSeparator();
                        list.add(res);

                    });
                    return list;
                case NEW:
                    jo.get("albums").getAsJsonObject()
                            .get("items").getAsJsonArray().forEach(albumEl -> {

                        var album = albumEl.getAsJsonObject();
                        var albumName = album.get("name").getAsString();

                        var joiner = new StringJoiner(", ");
                        album.get("artists").getAsJsonArray().forEach(artistEl -> {
                            var artistName = artistEl.getAsJsonObject().get("name").getAsString();
                            joiner.add(artistName);
                        });

                        var spotifyURL = album.get("external_urls").getAsJsonObject().get("spotify").getAsString();

                        String res = albumName +
                                System.lineSeparator() +
                                '[' + joiner.toString() + ']' +
                                System.lineSeparator() +
                                spotifyURL +
                                System.lineSeparator();
                        list.add(res);
                    });
                    return list;
                case CATEGORIES:
                default:
                    jo.get("categories").getAsJsonObject()
                            .get("items").getAsJsonArray().forEach(categoryEl -> {

                        var category = categoryEl.getAsJsonObject();
                        var categoryName = category.get("name").getAsString();
                        var categoryId = category.get("id").getAsString();

                        categoryIds.put(categoryName, categoryId);

                        list.add(categoryName);
                    });
                    return list;
            }
        } else {
            return Collections.singletonList(jo.get("error").getAsJsonObject().get("message").getAsString());
        }
    }

    private void setupServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            var query = exchange.getRequestURI().getQuery();

            String responseBody;
            if (query != null && query.contains("code=")) {
                responseBody = "Got the code. Return back to your program.";
                exchange.sendResponseHeaders(200, responseBody.length());
                authorizationCode = query.substring(5);
            } else {
                responseBody = "Not found authorization code. Try again.";
                exchange.sendResponseHeaders(200, responseBody.length());
            }
            exchange.getResponseBody().write(responseBody.getBytes());
            exchange.getResponseBody().close();
        });
    }
}
