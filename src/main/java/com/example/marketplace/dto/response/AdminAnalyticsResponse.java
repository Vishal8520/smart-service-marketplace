package com.example.marketplace.dto.response;

import java.math.BigDecimal;

public class AdminAnalyticsResponse {

    private long totalUsers;
    private long totalProviders;
    private long totalCustomers;
    private long totalServices;
    private long totalBookings;
    private long pendingBookings;
    private long completedBookings;
    private long cancelledBookings;
    private BigDecimal totalRevenue;
    private double averagePlatformRating;

    public AdminAnalyticsResponse() {
    }

    public AdminAnalyticsResponse(long totalUsers, long totalProviders, long totalCustomers, long totalServices,
            long totalBookings, long pendingBookings, long completedBookings, long cancelledBookings,
            BigDecimal totalRevenue, double averagePlatformRating) {
        this.totalUsers = totalUsers;
        this.totalProviders = totalProviders;
        this.totalCustomers = totalCustomers;
        this.totalServices = totalServices;
        this.totalBookings = totalBookings;
        this.pendingBookings = pendingBookings;
        this.completedBookings = completedBookings;
        this.cancelledBookings = cancelledBookings;
        this.totalRevenue = totalRevenue;
        this.averagePlatformRating = averagePlatformRating;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalUsers;
        private long totalProviders;
        private long totalCustomers;
        private long totalServices;
        private long totalBookings;
        private long pendingBookings;
        private long completedBookings;
        private long cancelledBookings;
        private BigDecimal totalRevenue;
        private double averagePlatformRating;

        public Builder totalUsers(long totalUsers) {
            this.totalUsers = totalUsers;
            return this;
        }

        public Builder totalProviders(long totalProviders) {
            this.totalProviders = totalProviders;
            return this;
        }

        public Builder totalCustomers(long totalCustomers) {
            this.totalCustomers = totalCustomers;
            return this;
        }

        public Builder totalServices(long totalServices) {
            this.totalServices = totalServices;
            return this;
        }

        public Builder totalBookings(long totalBookings) {
            this.totalBookings = totalBookings;
            return this;
        }

        public Builder pendingBookings(long pendingBookings) {
            this.pendingBookings = pendingBookings;
            return this;
        }

        public Builder completedBookings(long completedBookings) {
            this.completedBookings = completedBookings;
            return this;
        }

        public Builder cancelledBookings(long cancelledBookings) {
            this.cancelledBookings = cancelledBookings;
            return this;
        }

        public Builder totalRevenue(BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
            return this;
        }

        public Builder averagePlatformRating(double averagePlatformRating) {
            this.averagePlatformRating = averagePlatformRating;
            return this;
        }

        public AdminAnalyticsResponse build() {
            return new AdminAnalyticsResponse(totalUsers, totalProviders, totalCustomers, totalServices, totalBookings,
                    pendingBookings, completedBookings, cancelledBookings, totalRevenue, averagePlatformRating);
        }
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalProviders() {
        return totalProviders;
    }

    public void setTotalProviders(long totalProviders) {
        this.totalProviders = totalProviders;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalServices() {
        return totalServices;
    }

    public void setTotalServices(long totalServices) {
        this.totalServices = totalServices;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public long getPendingBookings() {
        return pendingBookings;
    }

    public void setPendingBookings(long pendingBookings) {
        this.pendingBookings = pendingBookings;
    }

    public long getCompletedBookings() {
        return completedBookings;
    }

    public void setCompletedBookings(long completedBookings) {
        this.completedBookings = completedBookings;
    }

    public long getCancelledBookings() {
        return cancelledBookings;
    }

    public void setCancelledBookings(long cancelledBookings) {
        this.cancelledBookings = cancelledBookings;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public double getAveragePlatformRating() {
        return averagePlatformRating;
    }

    public void setAveragePlatformRating(double averagePlatformRating) {
        this.averagePlatformRating = averagePlatformRating;
    }
}
