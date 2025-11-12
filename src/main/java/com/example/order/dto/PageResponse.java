package com.example.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Schema(description = "Paginated response")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    @Schema(description = "Orders on this page", 
            required = true)
    private List<T> content;
    
    @Schema(description = "Page number (starts at 0)", 
            example = "0",
            required = true)
    private int page;
    
    @Schema(description = "Items per page", 
            example = "20",
            required = true)
    private int size;
    
    @Schema(description = "Total number of orders", 
            example = "150",
            required = true)
    private long totalElements;
}
