package com.example.marketplace.service;

import com.example.marketplace.entity.Category;
import com.example.marketplace.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Plumbing")
                .description("Plumbing and pipe repairs")
                .build();
    }

    @Test
    @DisplayName("getAllCategories returns cached list of categories")
    void getAllCategories_returnsList() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<Category> result = categoryService.getAllCategories();

        assertEquals(1, result.size());
        assertEquals("Plumbing", result.get(0).getName());
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("createCategory saves and evicts cache")
    void createCategory_success() {
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        Category result = categoryService.createCategory(category);

        assertNotNull(result);
        assertEquals("Plumbing", result.getName());
        verify(categoryRepository, times(1)).save(category);
    }
}
