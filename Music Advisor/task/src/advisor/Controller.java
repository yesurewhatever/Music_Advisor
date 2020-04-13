package advisor;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Scanner;

public class Controller {
    @NotNull
    private Model model;
    @NotNull
    private View view;

    public Controller(@NotNull Model model, @NotNull View view) {
        this.model = model;
        this.view = view;
    }

    void mainMenu() throws IOException, InterruptedException {
        try (var scan = new Scanner(System.in)) {
            while (true) {
                var input = scan.nextLine().strip();

                var command = Command.getCommand(input);

                if (command == Command.AUTH) {
                    if (!model.isAuthorized()) {
                        auth();
                    } else {
                        System.out.println("Already authorized");
                    }
                }
                if (command == Command.EXIT) {
                    System.out.println("---GOODBYE!---");
                    return;
                }
                if (command == Command.UNKNOWN) {
                    System.out.println("Unknown command, try again");
                    continue;
                }
                if (model.isAuthorized()) {
                    switch (command) {
                        case NEW:
                        case FEATURED:
                        case PLAYLISTS:
                        case CATEGORIES:
                            view.setContent(model.requestData(command));
                            view.update();
                            break;
                        case NEXT:
                            view.next();
                            break;
                        case PREV:
                            view.prev();
                            break;
                    }
                } else {
                    System.out.println("Please, provide access for application.");
                }
            }
        }
    }

    private void auth() throws IOException, InterruptedException {
        model.listenForAuth();

        System.out.println("use this link to request the access code:");
        System.out.println(model.authLink());
        System.out.println();
        System.out.println("waiting for code...");
        model.waitForCode();
        System.out.println("code received");

        model.stopListening();

        System.out.println("Making http request for access_token...");
        var success = model.requestAccessToken();

        System.out.println(success ? "Success!" : "Failed");
    }
}
