package com.logic.Repository;

import com.logic.DTO.HotelPriceDTO;
import com.logic.entity.Hotel;
import com.logic.entity.HotelMinPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



import java.time.LocalDate;

import java.util.Optional;

public interface HotelMinPriceRepository extends JpaRepository<HotelMinPrice , Long> {



    @Query("""
        SELECT new com.logic.DTO.HotelPriceDTO(i.hotel, AVG(i.price))
            from HotelMinPrice i
                where i.hotel.city = :city
                    AND i.date >= :startDate
                    AND i.date < :endDate
                    AND i.hotel.active = true
                    AND i.hotel IN (
                        SELECT inv.hotel
                        FROM Inventory inv
                        WHERE inv.city = :city
                            AND inv.date >= :startDate
                            AND inv.date < :endDate
                            AND inv.closed = false
                            AND (inv.totalCount - inv.bookedCount - inv.reservedCount) >= :roomsCount
                        GROUP BY inv.hotel, inv.room
                        HAVING COUNT(inv.date) = :dateCount
                    )
                        GROUP BY i.hotel
                        HAVING COUNT(i.date) = :dateCount
    """)
    Page<HotelPriceDTO> findHotelsWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("dateCount") Long dateCount,
            Pageable pageable

    );


    Optional<HotelMinPrice> findByHotelAndDate(Hotel hotel, LocalDate date);
}
