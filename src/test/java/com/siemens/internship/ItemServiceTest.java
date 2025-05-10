package com.siemens.internship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ItemServiceTest {

    @InjectMocks
    private ItemService itemService;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    @org.springframework.beans.factory.annotation.Qualifier("taskExecutor")
    private Executor taskExecutor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        taskExecutor = Executors.newFixedThreadPool(2);
        itemService = new ItemService();
        itemService.itemRepository = itemRepository;
        itemService.taskExecutor = taskExecutor;
    }

    @Test
    void testFindAll() {
        List<Item> items = Arrays.asList(new Item(1L, "Item1", "Desc1", "NEW", "test@example.com"));
        when(itemRepository.findAll()).thenReturn(items);

        List<Item> result = itemService.findAll();
        assertEquals(items, result);
    }

    @Test
    void testFindAll_Empty() {
        when(itemRepository.findAll()).thenReturn(Collections.emptyList());

        List<Item> result = itemService.findAll();
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindById_Found() {
        Item item = new Item(1L, "Item1", "Desc1", "NEW", "test@example.com");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Optional<Item> result = itemService.findById(1L);
        assertTrue(result.isPresent());
        assertEquals(item, result.get());
    }

    @Test
    void testFindById_NotFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<Item> result = itemService.findById(1L);
        assertFalse(result.isPresent());
    }

    @Test
    void testSave() {
        Item item = new Item(1L, "Item1", "Desc1", "NEW", "test@example.com");
        when(itemRepository.save(item)).thenReturn(item);

        Item result = itemService.save(item);
        assertEquals(item, result);
    }

    @Test
    void testSave_NullInput() {
        assertThrows(IllegalArgumentException.class, () -> itemService.save(null));
    }

    @Test
    void testDeleteById() {
        doNothing().when(itemRepository).deleteById(1L);

        itemService.deleteById(1L);
        verify(itemRepository).deleteById(1L);
    }

    @Test
    void testDeleteById_NonExistent() {
        doThrow(new RuntimeException("Item not found")).when(itemRepository).deleteById(2L);

        assertThrows(RuntimeException.class, () -> itemService.deleteById(2L));
    }

    @Test
    void testProcessItemsAsync_Success() throws Exception {
        List<Long> ids = Arrays.asList(1L);
        Item item = new Item(1L, "Item1", "Desc1", "NEW", "test@example.com");
        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> processedItems = future.get();

        assertEquals(1, processedItems.size());
        assertEquals("PROCESSED", processedItems.get(0).getStatus());
    }

    @Test
    void testProcessItemsAsync_Failure() {
        List<Long> ids = Arrays.asList(1L);
        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Error during async processing: java.lang.RuntimeException: Failed to process item with ID 1: Database error", 
                        exception.getCause().getMessage());
    }

    @Test
    void testProcessItemsAsync_NullIds() throws Exception {
        when(itemRepository.findAllIds()).thenReturn(null);

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> processedItems = future.get();

        assertTrue(processedItems.isEmpty());
    }

    @Test
    void testProcessItemsAsync_EmptyIds() throws Exception {
        when(itemRepository.findAllIds()).thenReturn(Collections.emptyList());

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> processedItems = future.get();

        assertTrue(processedItems.isEmpty());
    }

    @Test
    void testProcessItemsAsync_ItemNotFound() throws Exception {
        List<Long> ids = Arrays.asList(1L);
        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> processedItems = future.get();

        assertTrue(processedItems.isEmpty());
    }
}