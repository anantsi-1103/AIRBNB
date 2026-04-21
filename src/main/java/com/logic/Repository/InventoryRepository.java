package com.logic.Repository;

import com.logic.entity.Hotel;
import com.logic.entity.Inventory;
import com.logic.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    int deleteByDateAfterAndRoom(LocalDate date, Room room);

    @Query("""
        SELECT DISTINCT i.hotel
            from Inventory i
                where i.city = :city
                    AND i.date between :startDate AND :endDate
                        AND i.closed = false
                            AND (i.totalCount-i.bookedCount - i.reserveCount) >= :roomsCount
                       GROUP BY i.hotel, i.room
                           having count(i.date) = :dateCount
    """)
    Page<Hotel> findHotelsWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("dateCount") Long dateCount,
            Pageable pageable);


    @Query("""
    select i
    from Inventory i
    where i.room.id = :roomId
                      AND i.date between :startDate AND :endDate
                      AND i.closed = false
                      AND (i.totalCount-i.bookedCount - i.reserveCount) >= :roomsCount
                      
""")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockAvailableInventory(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount
    );
}