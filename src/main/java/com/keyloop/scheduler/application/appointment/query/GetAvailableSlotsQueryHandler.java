package com.keyloop.scheduler.application.appointment.query;

import com.keyloop.scheduler.application.EntityNotFoundException;
import com.keyloop.scheduler.application.cqrs.QueryHandler;
import com.keyloop.scheduler.application.dto.AvailableSlotDto;
import com.keyloop.scheduler.application.dto.PagedResult;
import com.keyloop.scheduler.domain.appointment.AppointmentRepository;
import com.keyloop.scheduler.domain.appointment.TimeSlot;
import com.keyloop.scheduler.domain.dealership.DealershipId;
import com.keyloop.scheduler.domain.dealership.DealershipRepository;
import com.keyloop.scheduler.domain.dealership.ServiceBayRepository;
import com.keyloop.scheduler.domain.dealership.TechnicianRepository;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeId;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetAvailableSlotsQueryHandler
        implements QueryHandler<GetAvailableSlotsQuery, PagedResult<AvailableSlotDto>> {

    private final DealershipRepository dealershipRepo;
    private final ServiceTypeRepository serviceTypeRepo;
    private final ServiceBayRepository serviceBayRepo;
    private final TechnicianRepository technicianRepo;
    private final AppointmentRepository appointmentRepo;

    public GetAvailableSlotsQueryHandler(
            DealershipRepository dealershipRepo,
            ServiceTypeRepository serviceTypeRepo,
            ServiceBayRepository serviceBayRepo,
            TechnicianRepository technicianRepo,
            AppointmentRepository appointmentRepo) {
        this.dealershipRepo = dealershipRepo;
        this.serviceTypeRepo = serviceTypeRepo;
        this.serviceBayRepo = serviceBayRepo;
        this.technicianRepo = technicianRepo;
        this.appointmentRepo = appointmentRepo;
    }

    @Override
    public PagedResult<AvailableSlotDto> handle(GetAvailableSlotsQuery query) {
        // Anti-corruption layer: convert raw IDs to typed IDs
        var dealershipId = DealershipId.of(query.dealershipId());
        var serviceTypeId = ServiceTypeId.of(query.serviceTypeId());

        // 1. Validate
        var dealership = dealershipRepo.findById(dealershipId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Dealership not found: " + query.dealershipId()));
        var serviceType = serviceTypeRepo.findById(serviceTypeId)
            .orElseThrow(() -> new EntityNotFoundException(
                "ServiceType not found: " + query.serviceTypeId()));
        if (query.date().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Date must not be in the past");
        }

        // 2-3. Generate possible start times
        var possibleStarts = dealership.generatePossibleStartTimes(
            serviceType.getDurationMinutes());

        // 4-5. Fetch resources
        var bays = serviceBayRepo.findActiveByDealershipId(dealershipId);
        var technicians = technicianRepo.findByDealershipIdAndSpecialization(
            dealershipId, serviceType.getRequiredSpecialization());

        // 6. Existing non-cancelled appointments for that date
        var existingAppointments = appointmentRepo.findByDealershipAndDate(
            dealershipId, query.date());

        // 7. Compute available slots
        List<AvailableSlotDto> availableSlots = new ArrayList<>();

        for (var bay : bays) {
            for (var startLocalTime : possibleStarts) {
                var startDateTime = LocalDateTime.of(query.date(), startLocalTime);
                var timeSlot = TimeSlot.of(startDateTime,
                    serviceType.getDurationMinutes());

                // Check bay availability
                boolean bayBusy = existingAppointments.stream()
                    .anyMatch(apt -> apt.getServiceBayId().equals(bay.getId())
                                  && apt.getTimeSlot().overlapsWith(timeSlot));
                if (bayBusy) continue;

                // Find first available qualified technician
                for (var tech : technicians) {
                    boolean techBusy = existingAppointments.stream()
                        .anyMatch(apt -> apt.getTechnicianId().equals(tech.getId())
                                      && apt.getTimeSlot().overlapsWith(timeSlot));
                    if (!techBusy) {
                        availableSlots.add(new AvailableSlotDto(
                            timeSlot.startTime(), timeSlot.endTime(),
                            bay.getId().value(), bay.getName(),
                            tech.getId().value(), tech.getName()));
                        break; // 1 tech per bay-slot combo
                    }
                }
            }
        }

        // 8. Sort
        availableSlots.sort(Comparator.comparing(AvailableSlotDto::startTime));

        // 9. In-memory pagination
        int totalCount = availableSlots.size();
        int fromIndex = Math.min(query.pageIndex() * query.pageSize(), totalCount);
        int toIndex = Math.min(fromIndex + query.pageSize(), totalCount);
        var page = availableSlots.subList(fromIndex, toIndex);

        return new PagedResult<>(page, totalCount, query.pageIndex(), query.pageSize());
    }
}
