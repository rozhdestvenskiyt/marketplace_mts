package ru.avito.avito.search;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import ru.avito.avito.entity.Listing;
import ru.avito.avito.repository.ListingRepository;

@Component
public class ListingIndexInitializer implements ApplicationRunner {

    private static final int BATCH_SIZE = 500;

    private final ListingRepository listingRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final boolean reindexOnStartup;

    public ListingIndexInitializer(
            ListingRepository listingRepository,
            ElasticsearchOperations elasticsearchOperations,
            @Value("${app.elasticsearch.reindex-on-startup:false}") boolean reindexOnStartup) {
        this.listingRepository = listingRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.reindexOnStartup = reindexOnStartup;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(ListingDocument.class);
        if (indexOperations.exists()) {
            if (!reindexOnStartup) {
                return;
            }
            indexOperations.delete();
        }
        indexOperations.createWithMapping();

        Page<Listing> batch = listingRepository.findAll(
                PageRequest.of(0, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "id")));
        while (true) {
            batch.getContent().stream()
                    .map(this::toDocument)
                    .forEach(elasticsearchOperations::save);
            if (!batch.hasNext()) {
                return;
            }
            batch = listingRepository.findAll(batch.nextPageable());
        }
    }

    private ListingDocument toDocument(Listing listing) {
        return new ListingDocument(
                listing.getId(),
                listing.getUser().getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getPrice(),
                listing.getPhotos() == null ? java.util.List.of() : listing.getPhotos(),
                listing.getCreatedAt());
    }
}
