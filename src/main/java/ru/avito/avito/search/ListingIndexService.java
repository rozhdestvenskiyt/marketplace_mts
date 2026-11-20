package ru.avito.avito.search;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class ListingIndexService {

    private final ElasticsearchOperations elasticsearchOperations;

    public ListingIndexService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void updateIndex(ListingIndexEvent event) {
        if (event.document() == null) {
            elasticsearchOperations.delete(event.listingId().toString(), ListingDocument.class);
        } else {
            elasticsearchOperations.save(event.document());
        }
    }
}
