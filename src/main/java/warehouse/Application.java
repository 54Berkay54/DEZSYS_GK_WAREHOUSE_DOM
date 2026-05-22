package warehouse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import warehouse.model.ProductData;
import warehouse.repository.WarehouseRepository;

@SpringBootApplication
public class Application implements CommandLineRunner {

    @Autowired
    private WarehouseRepository repository;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        // Initialize product data repository
        repository.deleteAll();

        // 10 Produkte, 3 Kategorien (Getraenk, Waschmittel, Lebensmittel)
        repository.save(new ProductData("1","00-101","Orangensaft","Getraenk", 2500));
        repository.save(new ProductData("1","00-102","Apfelsaft","Getraenk", 3420));
        repository.save(new ProductData("1","00-103","Cola","Getraenk", 1200));

        repository.save(new ProductData("1","01-201","Ariel Color","Waschmittel", 478));
        repository.save(new ProductData("1","01-202","Persil Tabs","Waschmittel", 300));
        repository.save(new ProductData("1","01-203","Weichspueler","Waschmittel", 150));

        repository.save(new ProductData("1","02-301","Pasta 500g","Lebensmittel", 1324));
        repository.save(new ProductData("1","02-302","Reis 1kg","Lebensmittel", 800));
        repository.save(new ProductData("1","02-303","Tomatensauce","Lebensmittel", 600));
        repository.save(new ProductData("1","02-304","Olivenoel","Lebensmittel", 200));

        System.out.println("--- Grundlagen Daten geladen (10 Produkte/3 Kategorien) ---");

    }

}