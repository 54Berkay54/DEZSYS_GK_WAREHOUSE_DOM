package warehouse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import warehouse.model.ProductData;
import warehouse.repository.WarehouseRepository;
import java.util.List;

@RestController
@RequestMapping("/api")
public class WarehouseController {

    @Autowired
    private WarehouseRepository repository;


    @PostMapping("/warehouse")
    public String addWarehouse(@RequestBody List<ProductData> products) {
        repository.saveAll(products);
        return "Warehouse data added.";
    }

    @GetMapping("/warehouse")
    public List<ProductData> getAllWarehouses() {
        return repository.findAll();
    }

    @GetMapping("/warehouse/{id}")
    public List<ProductData> getWarehouseById(@PathVariable String id) {
        return repository.findByWarehouseID(id);
    }

    @DeleteMapping("/warehouse/{id}")
    public String deleteWarehouse(@PathVariable String id) {
        List<ProductData> list = repository.findByWarehouseID(id);
        repository.deleteAll(list);
        return "Warehouse " + id + " deleted.";
    }


    @PostMapping("/product")
    public ProductData addProduct(@RequestBody ProductData product) {
        return repository.save(product);
    }

    @GetMapping("/product")
    public List<ProductData> getAllProducts() {
        return repository.findAll();
    }

    @GetMapping("/product/{id}")
    public ProductData getProduct(@PathVariable String id) {
        return repository.findByProductID(id);
    }

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