package ru.avito.avito.search;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.avito.avito.dto.ListingResponse;
import ru.avito.avito.dto.ListingSearchResponse;
import ru.avito.avito.exception.ApiException;

@Service
public class ListingSearchService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_QUERY_LENGTH = 200;

    private final ElasticsearchOperations elasticsearchOperations;

    public ListingSearchService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public ListingSearchResponse search(
            String text,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long sellerId,
            String sort,
            int page,
            int size) {
        validate(text, minPrice, maxPrice, sellerId, page, size);
        String queryText = StringUtils.hasText(text) ? text.trim() : null;
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        if (queryText == null) {
            boolQuery.must(query -> query.matchAll(matchAll -> matchAll));
        } else {
            boolQuery.must(query -> query.multiMatch(multiMatch -> multiMatch
                    .query(queryText)
                    .fields("title^6", "title.en^4", "description^2", "description.en")
                    .fuzziness("AUTO")
                    .prefixLength(1)));
        }
        if (sellerId != null) {
            boolQuery.filter(query -> query.term(term -> term
                    .field("sellerId")
                    .value(value -> value.longValue(sellerId))));
        }
        if (minPrice != null || maxPrice != null) {
            boolQuery.filter(query -> query.range(range -> range.number(number -> {
                number.field("price");
                if (minPrice != null) {
                    number.gte(minPrice.doubleValue());
                }
                if (maxPrice != null) {
                    number.lte(maxPrice.doubleValue());
                }
                return number;
            })));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(builder -> builder.bool(boolQuery.build())))
                .withPageable(PageRequest.of(page, size))
                .withSort(sortOptions -> sortOptions.field(field -> field
                        .field(sortField(sort, queryText))
                        .order(sortOrder(sort))))
                .withSort(sortOptions -> sortOptions.field(field -> field
                        .field("_id")
                        .order(SortOrder.Desc)))
                .build();
        SearchHits<ListingDocument> hits = elasticsearchOperations.search(query, ListingDocument.class);
        List<ListingResponse> listings = hits.getSearchHits().stream()
                .map(this::toResponse)
                .toList();
        long total = hits.getTotalHits();
        int totalPages = (int) Math.ceil((double) total / size);
        return new ListingSearchResponse(listings, total, page, size, totalPages);
    }

    private void validate(
            String text,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long sellerId,
            int page,
            int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE
                || (text != null && text.length() > MAX_QUERY_LENGTH)
                || (minPrice != null && minPrice.signum() < 0)
                || (maxPrice != null && maxPrice.signum() < 0)
                || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)
                || (sellerId != null && sellerId <= 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid listing search parameters");
        }
    }

    private String sortField(String sort, String queryText) {
        String normalizedSort = normalizeSort(sort);
        return switch (normalizedSort) {
            case "price_asc", "price_desc" -> "price";
            case "newest" -> "createdAt";
            case "relevance" -> queryText == null ? "createdAt" : "_score";
            default -> throw invalidSort();
        };
    }

    private SortOrder sortOrder(String sort) {
        return switch (normalizeSort(sort)) {
            case "price_asc" -> SortOrder.Asc;
            case "price_desc" -> SortOrder.Desc;
            case "newest", "relevance" -> SortOrder.Desc;
            default -> throw invalidSort();
        };
    }

    private String normalizeSort(String sort) {
        return sort == null ? "newest" : sort.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidSort() {
        return new ApiException(HttpStatus.BAD_REQUEST, "unsupported listing sort");
    }

    private ListingResponse toResponse(SearchHit<ListingDocument> hit) {
        ListingDocument listing = hit.getContent();
        return new ListingResponse(
                listing.id(),
                listing.sellerId(),
                listing.title(),
                listing.description(),
                listing.price(),
                listing.photos() == null ? List.of() : listing.photos());
    }
}
