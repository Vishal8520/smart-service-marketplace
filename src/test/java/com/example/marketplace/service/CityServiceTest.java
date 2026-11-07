package com.example.marketplace.service;

import com.example.marketplace.dto.request.CityRequest;
import com.example.marketplace.dto.response.CityResponse;
import com.example.marketplace.entity.City;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.CityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CityServiceTest {

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private CityService cityService;

    private City activeCity;
    private City inactiveCity;

    @BeforeEach
    void setUp() {
        activeCity = City.builder()
                .id(1L)
                .name("Bengaluru")
                .state("Karnataka")
                .active(true)
                .build();

        inactiveCity = City.builder()
                .id(2L)
                .name("Mysuru")
                .state("Karnataka")
                .active(false)
                .build();
    }

    @Test
    @DisplayName("getActiveCities returns only active cities")
    void getActiveCities_returnsActiveOnly() {
        when(cityRepository.findAllByActiveTrue()).thenReturn(List.of(activeCity));

        List<CityResponse> result = cityService.getActiveCities();

        assertEquals(1, result.size());
        assertEquals("Bengaluru", result.get(0).getName());
        verify(cityRepository, times(1)).findAllByActiveTrue();
    }

    @Test
    @DisplayName("getAllCities returns all cities regardless of active status")
    void getAllCities_returnsAll() {
        when(cityRepository.findAll()).thenReturn(List.of(activeCity, inactiveCity));

        List<CityResponse> result = cityService.getAllCities();

        assertEquals(2, result.size());
        verify(cityRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("createCity creates and returns new city when name is unique")
    void createCity_success() {
        CityRequest request = new CityRequest();
        request.setName("Hyderabad");
        request.setState("Telangana");

        when(cityRepository.existsByNameIgnoreCase("Hyderabad")).thenReturn(false);
        when(cityRepository.save(any(City.class))).thenAnswer(invocation -> {
            City c = invocation.getArgument(0);
            return City.builder()
                    .id(3L)
                    .name(c.getName())
                    .state(c.getState())
                    .active(c.isActive())
                    .build();
        });

        CityResponse response = cityService.createCity(request);

        assertNotNull(response);
        assertEquals("Hyderabad", response.getName());
        assertEquals("Telangana", response.getState());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("createCity throws exception when city name already exists")
    void createCity_duplicateName_throwsException() {
        CityRequest request = new CityRequest();
        request.setName("Bengaluru");
        request.setState("Karnataka");

        when(cityRepository.existsByNameIgnoreCase("Bengaluru")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> cityService.createCity(request));
        verify(cityRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleCityActive toggles active status successfully")
    void toggleCityActive_success() {
        when(cityRepository.findById(1L)).thenReturn(Optional.of(activeCity));
        when(cityRepository.save(any(City.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CityResponse response = cityService.toggleCityActive(1L);

        assertFalse(response.isActive());
        verify(cityRepository, times(1)).save(activeCity);
    }

    @Test
    @DisplayName("toggleCityActive throws ResourceNotFoundException if city not found")
    void toggleCityActive_notFound_throwsException() {
        when(cityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cityService.toggleCityActive(99L));
    }
}
