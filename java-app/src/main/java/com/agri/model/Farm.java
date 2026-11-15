package com.agri.model;

/** A farm owned by a farmer. totalArea is in acres. */
public record Farm(int farmId, int farmerId, String location, double totalArea) {
}
