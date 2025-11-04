package ai.content.auto.dtos;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Generic paginated response DTO.
 * Provides pagination metadata along with the data.
 * 
 * @param <T> Type of data in the response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {

    private List<T> content;
    private PaginationMetadata pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationMetadata {
        private int page; // Current page number (0-based)
        private int size; // Page size
        private long totalElements; // Total number of elements
        private int totalPages; // Total number of pages
        private boolean first; // Is this the first page?
        private boolean last; // Is this the last page?
        private boolean hasNext; // Are there more pages?
        private boolean hasPrevious; // Are there previous pages?
        private int numberOfElements; // Number of elements in current page
    }

    /**
     * Create a paginated response from Spring Data Page object.
     * 
     * @param page Spring Data Page object
     * @param <T>  Type of data
     * @return PaginatedResponse
     */
    public static <T> PaginatedResponse<T> fromPage(org.springframework.data.domain.Page<T> page) {
        PaginationMetadata metadata = PaginationMetadata.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .numberOfElements(page.getNumberOfElements())
                .build();

        return PaginatedResponse.<T>builder()
                .content(page.getContent())
                .pagination(metadata)
                .build();
    }
}