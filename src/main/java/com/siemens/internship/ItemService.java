package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Service for managing Items with asynchronous processing.
 */
@Service
public class ItemService {

    @Autowired
    public ItemRepository itemRepository;

    private final List<Item> processedItems = new CopyOnWriteArrayList<>();

    @Autowired
    @Qualifier("taskExecutor")
    public Executor taskExecutor;

    /**
     * Configuration for the custom task executor.
     */
    @Configuration
    public static class AsyncConfig {
        @Bean(name = "taskExecutor")
        public Executor taskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(10);
            executor.setMaxPoolSize(20);
            executor.setQueueCapacity(100);
            executor.setThreadNamePrefix("ItemProcessor-");
            executor.initialize();
            return executor;
        }
    }

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    /**
     * Asynchronously processes all items by updating their status to "PROCESSED".
     * Uses CompletableFuture to ensure all items are processed before returning.
     * @return CompletableFuture containing the list of processed items.
     */
    @Async("taskExecutor")
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();
        processedItems.clear();


        if (itemIds == null || itemIds.isEmpty()) {
            return CompletableFuture.completedFuture(processedItems);
        }

    
        List<CompletableFuture<Void>> futures = itemIds.stream().map(id -> CompletableFuture.runAsync(() -> {
            try {
                Optional<Item> itemOpt = itemRepository.findById(id);
                if (itemOpt.isPresent()) {
                    Item item = itemOpt.get();
                    item.setStatus("PROCESSED");
                    itemRepository.save(item);
                    processedItems.add(item);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to process item with ID " + id + ": " + e.getMessage(), e);
            }
        }, taskExecutor)).collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> processedItems)
                .exceptionally(throwable -> {
                    throw new RuntimeException("Error during async processing: " + throwable.getMessage(), throwable);
                });
    }
}