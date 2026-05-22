package warehouse;

import org.springframework.web.client.RestTemplate;
import warehouse.model.ProductData;
import java.util.Random;

public class DataGenerator {
    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/api/product";
        Random rand = new Random();
        String[] categories = {"Getraenk", "Lebensmittel", "Waschmittel", "Tierfutter", "Reinigung", "Elektronik"};

        System.out.println("Sende 300 Produkte an die Middleware...");

        try {
            for (int i = 1; i <= 300; i++) {
                int warehouseId = rand.nextInt(5) + 1; // Lager 1 bis 5
                String cat = categories[rand.nextInt(categories.length)];

                ProductData p = new ProductData(
                        String.valueOf(warehouseId),
                        "PROD-" + i,
                        "Artikel-" + i,
                        cat,
                        Math.round(rand.nextDouble() * 1000.0 * 10.0) / 10.0 // Zufällige Menge bis 1000
                );

                restTemplate.postForObject(url, p, ProductData.class);

                if (i % 50 == 0) System.out.println(i + " Produkte erfolgreich übertragen...");
            }
            System.out.println("Alle 300 Vertiefungs-Daten sind in der MongoDB.");
        } catch (Exception e) {
            System.err.println("FEHLER: Middleware nicht erreichbar! Läuft die Application?");
        }
    }
}