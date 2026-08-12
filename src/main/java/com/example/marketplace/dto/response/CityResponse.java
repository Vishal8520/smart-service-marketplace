package com.example.marketplace.dto.response;

public class CityResponse {

    private Long id;
    private String name;
    private String state;
    private boolean active;

    public CityResponse() {
    }

    public CityResponse(Long id, String name, String state, boolean active) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.active = active;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String state;
        private boolean active;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public CityResponse build() {
            return new CityResponse(id, name, state, active);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
