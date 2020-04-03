package advisor;

import java.util.Locale;

public enum Command {
    AUTH, NEW, FEATURED, CATEGORIES, PLAYLISTS, NEXT, PREV, EXIT, UNKNOWN;
    public String arg;

    static Command getCommand(String input) {
        if (input.contains(" ")) {
            if (input.substring(0, input.indexOf(' ')).equalsIgnoreCase("playlists")) {
                PLAYLISTS.arg = input.substring(input.indexOf(' ') + 1);
                return PLAYLISTS;
            }
        }

        switch (input.toLowerCase(Locale.US)) {
            case "auth":
                return AUTH;
            case "featured":
                return FEATURED;
            case "new":
                return NEW;
            case "categories":
                return CATEGORIES;
            case "next":
                return NEXT;
            case "prev":
                return PREV;
            case "exit":
                return EXIT;
            default:
                return UNKNOWN;
        }
    }
}
