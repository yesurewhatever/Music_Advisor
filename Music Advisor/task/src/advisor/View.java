package advisor;

import java.util.List;

public class View {
    private List<String> content;
    private int amountOnPage = 5;
    private int currentInd = 0;

    public void setContent(List<String> content) {
        this.content = content;
    }

    public void setAmountOnPage(int amountOnPage) {
        this.amountOnPage = amountOnPage;
    }

    void update() {
        for (int i = currentInd; i < currentInd + amountOnPage && i < content.size(); i++) {
            System.out.println(content.get(i));
        }
        int pagesTotal = (int) Math.ceil(content.size() * 1.0 / amountOnPage);
        int currentPage = currentInd / amountOnPage + 1;
        System.out.printf("---PAGE %d OF %d---%s",
                currentPage, pagesTotal, System.lineSeparator());
    }

    void next() {
        if (currentInd + amountOnPage < content.size()) {
            currentInd += amountOnPage;
            update();
        } else {
            System.out.println("No more pages.");
        }
    }

    void prev() {
        if (currentInd - amountOnPage >= 0) {
            currentInd -= amountOnPage;
            update();
        } else {
            System.out.println("No more pages.");
        }
    }
}
