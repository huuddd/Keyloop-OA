package com.keyloop.scheduler.application.validation;

import com.keyloop.scheduler.application.appointment.command.CreateAppointmentCommand;
import com.keyloop.scheduler.domain.customer.CustomerId;
import com.keyloop.scheduler.domain.customer.CustomerRepository;
import com.keyloop.scheduler.domain.customer.VehicleId;
import com.keyloop.scheduler.domain.customer.VehicleRepository;
import com.keyloop.scheduler.domain.dealership.DealershipId;
import com.keyloop.scheduler.domain.dealership.DealershipRepository;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeId;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class CreateAppointmentCommandValidator
        implements CommandValidator<CreateAppointmentCommand> {

    private final CustomerRepository customerRepo;
    private final VehicleRepository vehicleRepo;
    private final DealershipRepository dealershipRepo;
    private final ServiceTypeRepository serviceTypeRepo;

    public CreateAppointmentCommandValidator(
            CustomerRepository customerRepo,
            VehicleRepository vehicleRepo,
            DealershipRepository dealershipRepo,
            ServiceTypeRepository serviceTypeRepo) {
        this.customerRepo = customerRepo;
        this.vehicleRepo = vehicleRepo;
        this.dealershipRepo = dealershipRepo;
        this.serviceTypeRepo = serviceTypeRepo;
    }

    @Override
    public ValidationResult validate(CreateAppointmentCommand command) {
        var errors = new ArrayList<String>();

        if (command.customerId() == null) {
            errors.add("customerId must not be null");
        }
        if (command.vehicleId() == null) {
            errors.add("vehicleId must not be null");
        }
        if (command.dealershipId() == null) {
            errors.add("dealershipId must not be null");
        }
        if (command.serviceTypeId() == null) {
            errors.add("serviceTypeId must not be null");
        }
        if (command.startTime() == null) {
            errors.add("startTime must not be null");
        }

        // Only check existence if IDs are non-null
        if (command.customerId() != null
                && customerRepo.findById(CustomerId.of(command.customerId())).isEmpty()) {
            errors.add("Customer not found: " + command.customerId());
        }
        if (command.vehicleId() != null
                && vehicleRepo.findById(VehicleId.of(command.vehicleId())).isEmpty()) {
            errors.add("Vehicle not found: " + command.vehicleId());
        }
        if (command.dealershipId() != null
                && dealershipRepo.findById(DealershipId.of(command.dealershipId())).isEmpty()) {
            errors.add("Dealership not found: " + command.dealershipId());
        }
        if (command.serviceTypeId() != null
                && serviceTypeRepo.findById(ServiceTypeId.of(command.serviceTypeId())).isEmpty()) {
            errors.add("ServiceType not found: " + command.serviceTypeId());
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.withErrors(errors);
    }

    @Override
    public Class<CreateAppointmentCommand> getCommandType() {
        return CreateAppointmentCommand.class;
    }
}
