package ru.avito.avito.search;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.MultiField;

@Document(indexName = "#{@environment.getProperty('app.elasticsearch.index', 'listings')}")
@Mapping(mappingPath = "elasticsearch/listings-mapping.json")
public record ListingDocument(
        @Id Long id,
        @Field(type = FieldType.Long) Long sellerId,
        @MultiField(
                mainField = @Field(type = FieldType.Text, analyzer = "russian"),
                otherFields = @InnerField(suffix = "en", type = FieldType.Text, analyzer = "english"))
        String title,
        @MultiField(
                mainField = @Field(type = FieldType.Text, analyzer = "russian"),
                otherFields = @InnerField(suffix = "en", type = FieldType.Text, analyzer = "english"))
        String description,
        @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
        BigDecimal price,
        @Field(type = FieldType.Keyword, index = false) List<String> photos,
        @Field(type = FieldType.Date) Instant createdAt) {
}
