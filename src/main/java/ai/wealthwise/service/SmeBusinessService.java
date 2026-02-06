package ai.wealthwise.service;

import ai.wealthwise.exception.ResourceNotFoundException;
import ai.wealthwise.model.dto.sme.SmeBusinessRequest;
import ai.wealthwise.model.dto.sme.SmeBusinessResponse;
import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.model.entity.User;
import ai.wealthwise.repository.SmeBusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmeBusinessService {

    private final SmeBusinessRepository smeBusinessRepository;

    @Transactional
    public SmeBusinessResponse createBusiness(User user, SmeBusinessRequest request) {
        log.info("Creating SME business for user: {}", user.getEmail());

        // Check if GSTIN already exists
        if (request.getGstin() != null && smeBusinessRepository.existsByGstin(request.getGstin())) {
            throw new IllegalArgumentException("Business with this GSTIN already exists");
        }

        SmeBusiness business = SmeBusiness.builder()
                .user(user)
                .businessName(request.getBusinessName())
                .gstin(request.getGstin())
                .pan(request.getPan())
                .industryType(request.getIndustryType())
                .businessSize(request.getBusinessSize())
                .annualTurnover(request.getAnnualTurnover())
                .registrationDate(request.getRegistrationDate())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .isActive(true)
                .build();

        SmeBusiness saved = smeBusinessRepository.save(business);
        log.info("SME business created with ID: {}", saved.getId());
        return SmeBusinessResponse.fromEntity(saved);
    }

    public SmeBusinessResponse getBusinessById(User user, Long businessId) {
        SmeBusiness business = smeBusinessRepository.findByIdAndUser(businessId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found with ID: " + businessId));
        return SmeBusinessResponse.fromEntity(business);
    }

    public List<SmeBusinessResponse> getAllBusinesses(User user) {
        return smeBusinessRepository.findActiveBusinessesByUser(user).stream()
                .map(SmeBusinessResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public SmeBusiness getBusinessEntity(User user, Long businessId) {
        return smeBusinessRepository.findByIdAndUser(businessId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found with ID: " + businessId));
    }

    @Transactional
    public SmeBusinessResponse updateBusiness(User user, Long businessId, SmeBusinessRequest request) {
        SmeBusiness business = getBusinessEntity(user, businessId);

        business.setBusinessName(request.getBusinessName());
        business.setGstin(request.getGstin());
        business.setPan(request.getPan());
        business.setIndustryType(request.getIndustryType());
        business.setBusinessSize(request.getBusinessSize());
        business.setAnnualTurnover(request.getAnnualTurnover());
        business.setRegistrationDate(request.getRegistrationDate());
        business.setAddressLine1(request.getAddressLine1());
        business.setAddressLine2(request.getAddressLine2());
        business.setCity(request.getCity());
        business.setState(request.getState());
        business.setPincode(request.getPincode());
        business.setContactEmail(request.getContactEmail());
        business.setContactPhone(request.getContactPhone());

        SmeBusiness saved = smeBusinessRepository.save(business);
        return SmeBusinessResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteBusiness(User user, Long businessId) {
        SmeBusiness business = getBusinessEntity(user, businessId);
        business.setIsActive(false);
        smeBusinessRepository.save(business);
        log.info("SME business soft-deleted: {}", businessId);
    }
}
