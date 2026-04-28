package com.logic.Repository;

import com.logic.DTO.HotelPriceDTO;
import com.logic.entity.HotelMinPrice;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface HotelMinPriceRepository extends JpaRepository<HotelMinPrice , Long> {



    @Query("""
        SELECT new com.logic.DTO.HotelPriceDTO(i.hotel, AVG(i.price))
            from HotelMinPrice i
                where i.hotel.city = :city
                    AND i.date between :startDate AND :endDate
                    AND i.hotel.active = true
                        GROUP BY i.hotel
    """)
    Page<HotelPriceDTO> findHotelsWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("dateCount") Long dateCount,
            Pageable pageable

    );

}
