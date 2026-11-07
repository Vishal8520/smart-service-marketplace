package com.example.marketplace.service;

import com.example.marketplace.dto.request.CityRequest;
import com.example.marketplace.dto.response.CityResponse;
import com.example.marketplace.entity.City;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.CityRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CityService {

    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    @Cacheable("activeCities")
    @Transactional(readOnly = true)
    public List<CityResponse> getActiveCities() {
        return cityRepository.findAllByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Cacheable("allCities")
    @Transactional(readOnly = true)
    public List<CityResponse> getAllCities() {
        return cityRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = {"activeCities", "allCities"}, allEntries = true)
    @Transactional
    public CityResponse createCity(CityRequest request) {
        if (cityRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("City already exists: " + request.getName());
        }

        City city = City.builder()
                .name(request.getName().trim())
                .state(request.getState().trim())
                .active(true)
                .build();

        City saved = cityRepository.save(city);
        return mapToResponse(saved);
    }

    @CacheEvict(value = {"activeCities", "allCities"}, allEntries = true)
    @Transactional
    public CityResponse toggleCityActive(Long id) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City", "id", id));

        city.setActive(!city.isActive());
        City saved = cityRepository.save(city);
        return mapToResponse(saved);
    }

    public CityResponse mapToResponse(City city) {
        return CityResponse.builder()
                .id(city.getId())
                .name(city.getName())
                .state(city.getState())
                .active(city.isActive())
                .build();
    }
}
