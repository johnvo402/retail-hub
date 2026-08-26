package com.johnvo.retailhub.infrastructure.elasticsearch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.UUID;

public interface SpringDataProductSearchRepository
        extends ElasticsearchRepository<ProductSearchDocument, UUID> {
    @Query("""
            {"bool":{"must":[{"multi_match":{"query":"?0","fields":["name^3","description","sku^4","categoryName^2"],"fuzziness":"AUTO"}}],"filter":[{"term":{"active":true}}]}}
            """)
    Page<ProductSearchDocument> search(String query, Pageable pageable);

    Page<ProductSearchDocument> findAllByActiveTrue(Pageable pageable);
}

