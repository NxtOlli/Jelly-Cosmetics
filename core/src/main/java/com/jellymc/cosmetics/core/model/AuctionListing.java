package com.jellymc.cosmetics.core.model;

import java.util.Date;
import java.util.UUID;

public class AuctionListing {
    private int id;
    private UUID sellerUuid;
    private String cosmeticId;
    private double price;
    private Date listedAt;
    private Date expiresAt;
    private String status;
    private String cosmeticType;

    public AuctionListing() {
    }

    public AuctionListing(int id, UUID sellerUuid, String cosmeticId, double price,
                          Date listedAt, Date expiresAt, String status, String cosmeticType) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.cosmeticId = cosmeticId;
        this.price = price;
        this.listedAt = listedAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.cosmeticType = cosmeticType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public void setSellerUuid(UUID sellerUuid) {
        this.sellerUuid = sellerUuid;
    }

    public String getCosmeticId() {
        return cosmeticId;
    }

    public void setCosmeticId(String cosmeticId) {
        this.cosmeticId = cosmeticId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Date getListedAt() {
        return listedAt;
    }

    public void setListedAt(Date listedAt) {
        this.listedAt = listedAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCosmeticType() {
        return cosmeticType;
    }

    public void setCosmeticType(String cosmeticType) {
        this.cosmeticType = cosmeticType;
    }

    public boolean isActive() {
        return "ACTIVE".equals(status) && expiresAt.after(new Date());
    }

    public boolean isExpired() {
        return expiresAt.before(new Date());
    }

    public long getRemainingTimeMillis() {
        return Math.max(0, expiresAt.getTime() - System.currentTimeMillis());
    }

    public String getRemainingTimeFormatted() {
        long remainingMillis = getRemainingTimeMillis();
        if (remainingMillis <= 0) {
            return "Expired";
        }

        long seconds = remainingMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}
