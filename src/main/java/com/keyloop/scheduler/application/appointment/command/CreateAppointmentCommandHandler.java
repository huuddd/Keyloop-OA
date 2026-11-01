package com.keyloop.scheduler.application.appointment.command;

import com.keyloop.scheduler.application.cqrs.CommandHandler;
import com.keyloop.scheduler.application.dto.AppointmentDto;
import com.keyloop.scheduler.application.port.EventPublisher;
import com.keyloop.scheduler.application.validation.ValidatingService;
import com.keyloop.scheduler.application.validation.ValidationException;
import com.keyloop.scheduler.domain.appointment.Appointment;
import com.keyloop.scheduler.domain.appointment.AppointmentErrors;
import com.keyloop.scheduler.domain.appointment.AppointmentRepository;
import com.keyloop.scheduler.domain.customer.CustomerId;
import com.keyloop.scheduler.domain.customer.VehicleId;
import com.keyloop.scheduler.domain.dealership.DealershipId;
import com.keyloop.scheduler.domain.dealership.ServiceBayId;
import com.keyloop.scheduler.domain.dealership.ServiceBayRepository;
import com.keyloop.scheduler.domain.dealership.Technician;
import com.keyloop.scheduler.domain.dealership.TechnicianRepository;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeId;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeRepository;
import com.keyloop.scheduler.domain.shared.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
public class CreateAppointmentCommandHandler
        implements CommandHandler<CreateAppointmentCommand, AppointmentDto> {

    private final AppointmentRepository appointmentRepo;
    private final ServiceBayRepository serviceBayRepo;
    private final TechnicianRepository technicianRepo;
    private final ServiceTypeRepository serviceTypeRepo;
    private final EventPublisher eventPublisher;
    private final ValidatingService validatingService;
    private final Clock clock;

    public CreateAppointmentCommandHandler(
            AppointmentRepository appointmentRepo,
            ServiceBayRepository serviceBayRepo,
            TechnicianRepository technicianRepo,
            ServiceTypeRepository serviceTypeRepo,
            EventPublisher eventPublisher,
            ValidatingService validatingService,
            Clock clock) {
        this.appointmentRepo = appointmentRepo;
        this.serviceBayRepo = serviceBayRepo;
        this.technicianRepo = technicianRepo;
        this.serviceTypeRepo = serviceTypeRepo;
        this.eventPublisher = eventPublisher;
        this.validatingService = validatingService;
        this.clock = clock;
    }

    @Override
    public Result<AppointmentDto> handle(CreateAppointmentCommand command) {

        // 1. Validate command (null checks + existence checks)
        var validation = validatingService.validate(command);
        if (!validation.isValid()) {
            throw new ValidationException(validation);
        }

        // Anti-corruption layer: wrap raw IDs into typed IDs
        var customerId = CustomerId.of(command.customerId());
        var vehicleId = VehicleId.of(command.vehicleId());
        var dealershipId = DealershipId.of(command.dealershipId());
        var serviceTypeId = ServiceTypeId.of(command.serviceTypeId());

        var serviceType = serviceTypeRepo.findById(serviceTypeId).orElseThrow();
        var endTime = command.startTime().plusMinutes(serviceType.getDurationMinutes());

        // 2. Find available bay with pessimistic lock
        var bays = serviceBayRepo.findActiveByDealershipId(dealershipId);
        ServiceBayId selectedBayId = null;

        for (var bay : bays) {
            var overlapping = appointmentRepo.findOverlappingForBayWithLock(
                bay.getId(), command.startTime(), endTime);
            if (overlapping.isEmpty()) {
                selectedBayId = bay.getId();
                break;
            }
        }

        if (selectedBayId == null) {
            return Result.failure(AppointmentErrors.NO_BAY_AVAILABLE);
        }

        // 3. Find available qualified technician with pessimistic lock
        var technicians = technicianRepo.findByDealershipIdAndSpecialization(
            dealershipId, serviceType.getRequiredSpecialization());
        Technician selectedTech = null;

        for (var tech : technicians) {
            var overlapping = appointmentRepo.findOverlappingForTechnicianWithLock(
                tech.getId(), command.startTime(), endTime);
            if (overlapping.isEmpty()) {
                selectedTech = tech;
                break;
            }
        }

        if (selectedTech == null) {
            return Result.failure(AppointmentErrors.NO_TECHNICIAN_AVAILABLE);
        }

        // 4. Create domain object (validates invariants via Result)
        var createResult = Appointment.create(
            customerId, vehicleId, selectedBayId,
            selectedTech.getId(), serviceType, selectedTech,
            command.startTime(), clock);

        if (createResult.isFailure()) {
            return Result.failure(createResult.getError());
        }

        var appointment = createResult.getValue();

        // 5. Persist
        var saved = appointmentRepo.save(appointment);

        // 6. Publish domain events after successful persistence
        eventPublisher.publishEventsFor(appointment);

        return Result.success(AppointmentDto.fromDomain(saved));
    }
}
