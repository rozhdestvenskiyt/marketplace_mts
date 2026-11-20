package ru.avito.avito.search;

public record ListingIndexEvent(Long listingId, ListingDocument document) {

    public static ListingIndexEvent upsert(ListingDocument document) {
        return new ListingIndexEvent(document.id(), document);
    }

    public static ListingIndexEvent delete(long listingId) {
        return new ListingIndexEvent(listingId, null);
    }
}
