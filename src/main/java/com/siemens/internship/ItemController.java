package com.siemens.internship;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * This is the controller for handling items. It has CRUD stuff and async stuff.
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * This gets all the items from the database.
     * @return A list of items with HTTP 200 OK.
     */
    @GetMapping
    public ResponseEntity<List<Item>> fetchAll() {
        List<Item> items = itemService.findAll();
        return ResponseEntity.ok(items);
    }

    /**
     * This creates a new item.
     * @param item The item to be created.
     * @param result This checks if the item is valid.
     * @return The created item with HTTP 201 CREATED, or 400 BAD_REQUEST if something is wrong.
     */
    @PostMapping
    public ResponseEntity<Item> addItem(@Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }
        Item savedItem = itemService.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedItem);
    }

    /**
     * This gets an item by its ID.
     * @param id The ID of the item.
     * @return The item with HTTP 200 OK, or 404 NOT_FOUND if it doesn't exist.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Item> fetchById(@PathVariable Long id) {
        return itemService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * This updates an item if it exists.
     * @param id The ID of the item to update.
     * @param updatedItem The new data for the item.
     * @return The updated item with HTTP 200 OK, or 404 NOT_FOUND if it doesn't exist.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Item> modifyItem(@PathVariable Long id, @RequestBody Item updatedItem) {
        Optional<Item> existing = itemService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        updatedItem.setId(id);
        Item result = itemService.save(updatedItem);
        return ResponseEntity.ok(result);
    }

    /**
     * This deletes an item by its ID.
     * @param id The ID of the item to delete.
     * @return HTTP 204 NO_CONTENT if it works, or 404 NOT_FOUND if the item isn't there.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeItem(@PathVariable Long id) {
        if (!itemService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        itemService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * This processes all the items asynchronously.
     * @return A list of processed items with HTTP 200 OK, or 500 INTERNAL_SERVER_ERROR if something breaks.
     */
    @GetMapping("/process")
    public CompletableFuture<ResponseEntity<List<Item>>> processAsync() {
        return itemService.processItemsAsync()
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
}
