package com.agri.model;

/** A farmer who owns one or more farms. */
public record Farmer(int farmerId, String name, String contactNumber, String address) {
}
