package com.personal.happygallery.adapter.out.persistence.reward;

import com.personal.happygallery.domain.reward.RewardReservationAllocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface RewardReservationAllocationRepository
        extends JpaRepository<RewardReservationAllocation, Long> {

    List<RewardReservationAllocation> findByReservationIdOrderByIdAsc(Long reservationId);
}
