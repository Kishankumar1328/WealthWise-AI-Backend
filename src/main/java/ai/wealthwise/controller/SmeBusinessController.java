package ai.wealthwise.controller;

import ai.wealthwise.model.dto.sme.SmeBusinessRequest;
import ai.wealthwise.model.dto.sme.SmeBusinessResponse;
import ai.wealthwise.model.entity.User;
import ai.wealthwise.repository.UserRepository;
import ai.wealthwise.service.SmeBusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sme/businesses")
@RequiredArgsConstructor
public class SmeBusinessController {

    private final SmeBusinessService smeBusinessService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<SmeBusinessResponse> createBusiness(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SmeBusinessRequest request) {
        User user = getUserFromDetails(userDetails);
        SmeBusinessResponse response = smeBusinessService.createBusiness(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SmeBusinessResponse>> getAllBusinesses(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        List<SmeBusinessResponse> businesses = smeBusinessService.getAllBusinesses(user);
        return ResponseEntity.ok(businesses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SmeBusinessResponse> getBusinessById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = getUserFromDetails(userDetails);
        SmeBusinessResponse response = smeBusinessService.getBusinessById(user, id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SmeBusinessResponse> updateBusiness(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody SmeBusinessRequest request) {
        User user = getUserFromDetails(userDetails);
        SmeBusinessResponse response = smeBusinessService.updateBusiness(user, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusiness(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = getUserFromDetails(userDetails);
        smeBusinessService.deleteBusiness(user, id);
        return ResponseEntity.noContent().build();
    }

    private User getUserFromDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
