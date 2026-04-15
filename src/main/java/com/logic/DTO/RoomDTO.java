package com.logic.DTO;

import com.logic.entity.Hotel;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Data
public class RoomDTO {

    private Long id;
    private String type;
    private BigDecimal basePrice;
    // Photos table
    private String[] photos;
    private String[] amenities;
    private Integer totalCount;
    private Integer capacity;


}
