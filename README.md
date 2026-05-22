# Middleware Engineering "Document Oriented Middleware using MongoDB" - Taskdescription
GIT repository: [https://github.com/ThomasMicheler/DEZSYS_GK_WAREHOUSE_DOM.git](https://github.com/ThomasMicheler/DEZSYS_GK_WAREHOUSE_DOM.git)

## Einführung

Diese Übung soll helfen die Funktionsweise und Einsatzmöglichkeiten eines dokumentenorientierten dezentralen Systems mit Hilfe des Frameworks Spring Data MongoDB oder einem Framework Ihrer Wahl zu demonstrieren. Die Daten werden in dieser Übung in einem NoSQL Repository gespeichert und verarbeitet.

Es handelt sich um ein Lagerstandort Beispiel, wie in Aufgabe "GK8.1 Spring Data and ORM". Die Daten aller Lagerstandorte sollen in der Zentrale persistiert und in einer NoSQL Datenbank gespeichert werden. Von hier aus koennen die Daten fuer verschiedene Fragestellungen des Betriebes (Management, Einkauf, Vertrieb,...) abgefragt werden.

## 1.1 Ziele

Das Ziel dieser Übung ist die Implementierung einer dokumentenorientierten Middleware, die die Daten aller Warenlager zentral in einem entsprechenden Format ablegt.

## 1.2 Voraussetzungen

* Grundlagen zu JSON & REST
* Grundlagen Architektur von verteilten Systemen
* Grundlagen Spring Framework, Spring Boot oae.
* Grundlagen NoSQL
* Installation MongoDB
* Datenstruktur basierend auf der Aufgabenstellung "GK8.1 Spring Data and ORM"
* Umsetzung eines einfachen Web-Userinterfaces zur Anzeige von Daten

## 2. Wichtige Codeblöcke
### 2.1 Repository (`WarehouseRepository.java`)
Das Interface nutzt Spring Data, um Standard-Datenbankoperationen ohne manuellen SQL/NoSQL-Code bereitzustellen.
``` java
public interface WarehouseRepository extends MongoRepository<ProductData, String> {
    // Findet ein spezifisches Produkt anhand der Business-ID
    public ProductData findByProductID(String productID);
    // Listet alle Produkte eines bestimmten Lagers auf
    public List<ProductData> findByWarehouseID(String warehouseID);
}
```

### 2.2 REST-Controller (`WarehouseController.java`)
Hier wird die Schnittstelle für die Lagerstandorte definiert. Besonders wichtig sind die Endpunkte für Warehouse-Management und Einzelprodukte.
``` java
@RestController
@RequestMapping("/api")
public class WarehouseController {
    @Autowired
    private WarehouseRepository repository;

    // GET: Alle Produkte abrufen
    @GetMapping("/product")
    public List<ProductData> getAllProducts() {
        return repository.findAll();
    }

    // POST: Ein neues Produkt hinzufügen/aktualisieren
    @PostMapping("/product")
    public ProductData addProduct(@RequestBody ProductData product) {
        return repository.save(product);
    }

    // DELETE: Ein Produkt anhand der ID löschen
    @DeleteMapping("/product/{id}")
    public String deleteProduct(@PathVariable String id) {
        ProductData p = repository.findByProductID(id);
        if(p != null) {
            repository.delete(p);
            return "Product " + id + " deleted.";
        }
        return "Product not found.";
    }
}
```

### 2.3 Testdaten-Generator (`DateGenerator.java`)
Für die "Vertiefung" wurde ein Generator implementiert, der 300 Produkte zufällig auf 5 Lager verteilt.
``` java
// Beispielhafter Loop zur Generierung
for (int i = 1; i <= 300; i++) {
    int warehouseId = rand.nextInt(5) + 1;
    ProductData p = new ProductData(
        String.valueOf(warehouseId), 
        "PROD-" + i, "Artikel-" + i, 
        cat, 
        quantity
    );
    restTemplate.postForObject(url, p, ProductData.class);
}
```

### 2.4 Mongo Shell Operationen (CRUD)
Um die Daten direkt in der Datenbank zu verwalten, werden folgende Befehle genutzt:
``` shell
// 1. CREATE: Ein neues Produkt mit mehreren Lager-Subdokumenten anlegen
db.products.insertOne({
    name: "Macbook Air",
    category: "Hardware",
    price: 1200,
    warehouses: [
        { warehouse_id: "WH01", stock: 14 },
        { warehouse_id: "WH02", stock: 10 }
    ],
    tags: ["laptop", "office", "premium"]
});

// 2. READ: Ein spezifisches Produkt anhand des Namens finden
db.products.find({ name: "Macbook Air" }).pretty();

// 3. UPDATE: Den Lagerbestand in einem spezifischen Array-Element erhöhen
db.products.updateOne(
    { name: "Macbook Air", "warehouses.warehouse_id": "WH01" },
    { $inc: { "warehouses.$.stock": 5 } }
);

// 4. DELETE: Ein Dokument vollständig entfernen
db.products.deleteOne({ name: "Macbook Air" });

// 5. COMPLEX-READ: Aggregation über Kategorien (Vertiefung)
db.products.aggregate([
    { $unwind: "$warehouses" }, // Zerlegt das Array für die Berechnung
    { $group: {
            _id: "$category",
            totalStock: { $sum: "$warehouses.stock" }
        }}
]);
```

## Vertiefung Fragen
### Frage 1: Kritischer Lagerbestand (Meldebestand)
Szenario: Der Einkauf muss wissen, welche Produkte knapp werden, um rechtzeitig nachzubestellen.

Frage: Welche Produkte haben über alle Lagerstandorte hinweg einen Gesamtlagerbestand von weniger als 10 Stück?

MongoDB-Query (Aggregation Pipeline):
``` javascript
db.warehouse.aggregate([
  // 1. Das Produkt-Array in einzelne Dokumente aufteilen
  { $unwind: "$products" },
  
  // 2. Gruppieren nach Produkt-ID/Name und die Mengen summieren
  { $group: {
      _id: { productId: "$products.id", productName: "$products.name" },
      totalStock: { $sum: "$products.quantity" }
  }},
  
  // 3. Filtern nach Produkten mit einem Gesamtbestand unter 10
  { $match: { totalStock: { $lt: 10 } } }
])
```

### Frage 2: Filialübergreifende Produktsuche (Bestandsabfrage)
Szenario: Ein Kunde steht in Lager A und sucht ein bestimmtes Produkt, das dort ausverkauft ist. Der Vertrieb möchte wissen, in welchen anderen Lagern das Produkt noch vorrätig ist.

Frage: Wie hoch ist der Lagerbestand des Produkts mit der ID "PROD-123" aufgeteilt auf die einzelnen Lagerstandorte?

MongoDB-Query:
``` javascript
db.warehouse.aggregate([
  // 1. Nur Dokumente durchsuchen, die das Produkt überhaupt enthalten
  { $match: { "products.id": "PROD-123" } },
  
  // 2. Array auflösen
  { $unwind: "$products" },
  
  // 3. Nur das gesuchte Produkt herausfiltern
  { $match: { "products.id": "PROD-123" } },
  
  // 4. Schickes Ausgabeformat: Lager-Name und die jeweilige Menge anzeigen
  { $project: {
      _id: 0,
      warehouseId: "$id",
      warehouseName: "$name",
      productName: "$products.name",
      stock: "$products.quantity"
  }}
])
```

### Frage 3: Lagerkapazität & Sortimentsbreite pro Standort
Szenario: Das Management benötigt eine Übersicht zur Auslastung und Produktvielfalt der einzelnen Warenhäuser, um logistische Entscheidungen zu treffen.

Frage: Wie viele verschiedene Produkte (Sortimentsbreite) und wie viele Artikel insgesamt (Gesamtwert/Menge) führt jedes einzelne Lager?

MongoDB-Query:
``` javascript
db.warehouse.aggregate([
  // 1. Array auflösen, um mit den Mengen rechnen zu können
  { $unwind: "$products" },
  
  // 2. Nach Lager gruppieren: Anzahl der Dokumente zählen (= verschiedene Produkte) 
  //    und die Mengen aufsummieren (= Gesamtanzahl aller Artikel)
  { $group: {
      _id: { warehouseId: "$id", warehouseName: "$name" },
      uniqueProductsCount: { $sum: 1 },
      totalArticlesCount: { $sum: "$products.quantity" }
  }},
  
  // 3. Sortieren nach dem Lager mit den meisten Artikeln
  { $sort: { totalArticlesCount: -1 } }
])
```

## 3. Fragestellung für Protokoll

+ Nennen Sie 4 Vorteile eines NoSQL Repository im Gegensatz zu einem relationalen DBMS

Flexibles Schema: Dokumente können unterschiedliche Strukturen haben; Änderungen am Datenmodell erfordern keine aufwendigen Schema-Migrationen (Migrationsskripte).

Hohe Skalierbarkeit: NoSQL-Datenbanken sind von Grund auf für die horizontale Skalierbarkeit (Sharding über mehrere Server) ausgelegt.

Bessere Performance bei unstrukturierten Daten: Schnelles Lesen und Schreiben von JSON-Objekten, da keine teuren JOIN-Operationen über mehrere Tabellen nötig sind.

Naturnahe Datenspeicherung: Daten werden so im Dokument gespeichert, wie sie in der Applikation verwendet werden (z. B. als verschachteltes Objekt/Array), was das Object-Relational Mapping (ORM) vereinfacht.
+ Nennen Sie 4 Nachteile eines NoSQL Repository im Gegensatz zu einem relationalen DBMS

Eingeschränkte ACID-Garantien: Viele NoSQL-Systeme opfern strikte Konsistenz (ACID) zugunsten von Verfügbarkeit und Geschwindigkeit (Eventual Consistency).

Keine standardisierten JOINs: Verknüpfungen über mehrere Collections/Tabellen hinweg sind performancetechnisch teuer oder müssen auf Applikationsseite gelöst werden.

Keine einheitliche Abfragesprache: Es gibt kein universelles SQL; jedes NoSQL-System (MongoDB, Cassandra, Neo4j) hat seine eigene, proprietäre Query-Language.

Datenredundanz: Um JOINs zu vermeiden, werden Daten oft dupliziert. Das erhöht den Speicherbedarf und das Risiko von Inkonsistenzen bei Aktualisierungen.
+ Welche Schwierigkeiten ergeben sich bei der Zusammenführung der Daten?

Inkonsistente Datenformate: Wenn verschiedene Lager unterschiedliche JSON-Strukturen oder Datentypen (z. B. String vs. Integer bei IDs) an die Zentrale senden.

Gleichzeitige Schreibzugriffe (Race Conditions): Wenn zwei Lager zeitgleich den Bestand desselben Produkts aktualisieren, kann es ohne striktes Sperrsystem zu Datenverlust kommen ("Last-Write-Wins"-Problem).

ID-Konflikte (Schlüssel-Kollisionen): Wenn dezentrale Systeme eigene IDs generieren, die in der Zentrale nicht mehr eindeutig sind.

Fehlende Transaktionssicherheit: Ein Fehler beim Übertragen eines Teils der Daten lässt sich schwerer per "Rollback" rückgängig machen als in einer SQL-Datenbank.
+ Welche Arten von NoSQL Datenbanken gibt es?


+ Nennen Sie einen Vertreter für jede Art?

  1. Dokumentenorientierte Datenbanken (z.B. MongoDB)
  2. Key-Value Stores (z.B. Redis)
  3. Spaltenorientierte Datenbanken (z.B. Cassandra)
  4. Graphdatenbanken (z.B. Neo4j, ArangoDB)
+ Beschreiben Sie die Abkürzungen CA, CP und AP in Bezug auf das CAP Theorem
  
    Das CAP-Theorem besagt, dass ein verteiltes System maximal zwei der folgenden drei Eigenschaften gleichzeitig garantieren kann:

    * C (Consistency / Konsistenz): Alle Knoten sehen zu jeder Zeit dieselben Daten (Sofortige Gleichheit nach einem Schreibvorgang).

    * A (Availability / Verfügbarkeit): Jede Anfrage erhält eine Antwort (Erfolg oder Fehler), ohne Garantie, dass es die allerneuesten Daten sind.

    * P (Partition Tolerance / Partitionstoleranz): Das System arbeitet weiter, selbst wenn die Kommunikation zwischen den Servern unterbrochen ist (Netzwerk-Splittung).
    
  Kombinationen:

    * CP: Das System blockiert Zugriffe, bis die Daten überall synchron sind. Sicherheit geht vor Verfügbarkeit.

    * AP: Das System antwortet sofort mit den Daten, die es gerade hat, selbst wenn sie veraltet sind. Verfügbarkeit geht vor Konsistenz.

    * CA: In der Praxis bei verteilten Systemen unmöglich, da Netzwerkfehler (P) immer passieren können. Existiert nur in Single-Server-Systemen.

+ Mit welchem Befehl koennen Sie den Lagerstand eines Produktes aller Lagerstandorte anzeigen.
```
db.productData.find({ "productID": "PROD-100" })
```
+ Mit welchem Befehl koennen Sie den Lagerstand eines Produktes eines bestimmten Lagerstandortes anzeigen.
```
db.productData.find({ "warehouseID": "3", "productID": "PROD-100" })
```

## 4. Links und Dokumente
* [Was bedeutet NoSQL](https://www.oracle.com/at/database/nosql/what-is-nosql)
* [Accessing Data with MongoDB](https://spring.io/guides/gs/accessing-data-mongodb/)
* [MongoDB Installation](https://docs.mongodb.com/manual/administration/install-community/)
* [mongo Shell Quick Reference](https://docs.mongodb.com/manual/reference/mongo-shell/)
* [mongo Shell Query Reference](https://www.mongodb.com/docs/manual/tutorial/query-embedded-documents/)
* [Grundlagen Spring Framework](https://spring.io/)
* [Spring Boot](https://spring.io/guides/gs/spring-boot/)
* [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb)
* [Spring RESTful Web Service](https://spring.io/guides/gs/rest-service/#use-maven)
* NoSQL Introduction
  - [NoSQL on w3resource](https://www.w3resource.com/mongodb/nosql.php)  
  - [Introduction to NoSQL Database](https://www.edureka.co/blog/introduction-to-nosql-database/)  
  - [NoSQL im Überblick](https://www.heise.de/ct/artikel/NoSQL-im-Ueberblick-1012483.html)  
  - [Introduction to NoSQL Databases on YouTube ](https://www.youtube.com/watch?v=2yQ9TGFpDuM)  

