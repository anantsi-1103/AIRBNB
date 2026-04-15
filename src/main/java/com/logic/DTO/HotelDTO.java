package com.logic.DTO;

import com.logic.entity.HotelContactInfo;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
public class HotelDTO {

    private Long id;
    private String name;
    private String city;
    // Photos table
    private String[] photos;
    // Amenities table
    private String[] amenities;
    private HotelContactInfo contactInfo;
    private Boolean active;
}
