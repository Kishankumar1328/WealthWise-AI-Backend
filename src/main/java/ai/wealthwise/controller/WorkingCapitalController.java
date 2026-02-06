package ai.wealthwise.controller;

import ai.wealthwise.model.dto.sme.WorkingCapitalResponse;
import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.model.entity.User;
import ai.wealthwise.repository.UserRepository;
import ai.wealthwise.service.SmeBusinessService;
import ai.wealthwise.service.WorkingCapitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sme/working-capital")
@RequiredArgsConstructor
public class WorkingCapitalController {

    private final WorkingCapitalService workingCapitalService;
    private final SmeBusinessService smeBusinessService;
    private final UserRepository userRepository;

    @GetMapping("/{businessId}/optimization")
    public ResponseEntity<WorkingCapitalResponse> getOptimization(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessId) {
        User user = getUserFromDetails(userDetails);
        SmeBusiness business = smeBusinessService.getBusinessEntity(user, businessId);
        return ResponseEntity.ok(workingCapitalService.getOptimizationPlan(business));
    }

    private User getUserFromDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
