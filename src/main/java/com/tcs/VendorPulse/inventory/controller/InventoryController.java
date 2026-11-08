package com.tcs.VendorPulse.inventory.controller;

import com.tcs.VendorPulse.inventory.entity.InventorySnapshot;
import com.tcs.VendorPulse.inventory.service.InventoryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService s) { service = s; }

    @PostMapping("/sync")
    public InventorySnapshot sync(@RequestParam String materialCode, @RequestParam int available,
                                 @RequestParam int safety, @RequestParam double consumption) {
        return service.sync(materialCode, available, safety, consumption);
    }

    @GetMapping("/{materialCode}")
    public InventorySnapshot latest(@PathVariable String materialCode) {
        return service.latest(materialCode);
    }

    @GetMapping
    public List<InventorySnapshot> all() {
        return service.all();
    }
}
