package advisor;

import java.io.IOException;

public class Main {
    private static final String REDIRECT_URI = "http://localhost:8080";
    private static final String CLIENT_ID = "d2c1038a26d44699a4de666d67e3e0dd";
    private static final String CLIENT_SECRET = "540fc0de3f0c477c9cc7ea0d10d10526";

    public static void main(String[] args) throws IOException, InterruptedException {
        var model = new Model(REDIRECT_URI, CLIENT_ID, CLIENT_SECRET);
        var view = new View();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-access") && args.length > i + 1) {
                model.setAuthorizationServerPath(args[i + 1]);
            }
            if (args[i].equals("-resource") && args.length > i + 1) {
                model.setApiServerPath(args[i + 1]);
            }
            if (args[i].equals("-page") && args.length > i + 1) {
                view.setAmountOnPage(Integer.parseInt(args[i + 1]));
            }
        }

        var controller = new Controller(model, view);
        controller.mainMenu();
    }
}
