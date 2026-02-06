package ai.wealthwise.service;

import ai.wealthwise.exception.ResourceNotFoundException;
import ai.wealthwise.model.dto.financialgoal.FinancialGoalRequest;
import ai.wealthwise.model.dto.financialgoal.FinancialGoalResponse;
import ai.wealthwise.model.entity.FinancialGoal;
import ai.wealthwise.model.entity.User;
import ai.wealthwise.repository.FinancialGoalRepository;
import ai.wealthwise.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialGoalService {

    private final FinancialGoalRepository financialGoalRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<FinancialGoalResponse> getAllGoals() {
        User user = securityUtils.getCurrentUser();
        return financialGoalRepository.findByUserOrderByPriorityDescTargetDateAsc(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FinancialGoalResponse createGoal(FinancialGoalRequest request) {
        User user = securityUtils.getCurrentUser();

        FinancialGoal goal = FinancialGoal.builder()
                .user(user)
                .title(request.getTitle())
                .goalType(FinancialGoal.GoalType.valueOf(request.getGoalType()))
                .targetAmount(request.getTargetAmount())
                .currentAmount(request.getCurrentAmount() != null ? request.getCurrentAmount() : BigDecimal.ZERO)
                .targetDate(request.getTargetDate())
                .priority(FinancialGoal.Priority.valueOf(request.getPriority()))
                .status(FinancialGoal.GoalStatus.IN_PROGRESS)
                .build();

        return mapToResponse(financialGoalRepository.save(goal));
    }

    @Transactional
    public FinancialGoalResponse updateGoal(Long id, FinancialGoalRequest request) {
        User user = securityUtils.getCurrentUser();
        FinancialGoal goal = financialGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        goal.setTitle(request.getTitle());
        goal.setGoalType(FinancialGoal.GoalType.valueOf(request.getGoalType()));
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(request.getCurrentAmount());
        goal.setTargetDate(request.getTargetDate());
        goal.setPriority(FinancialGoal.Priority.valueOf(request.getPriority()));

        return mapToResponse(financialGoalRepository.save(goal));
    }

    @Transactional
    public void deleteGoal(Long id) {
        User user = securityUtils.getCurrentUser();
        FinancialGoal goal = financialGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        financialGoalRepository.delete(goal);
    }

    @Transactional
    public FinancialGoalResponse addFunds(Long id, BigDecimal amount) {
        User user = securityUtils.getCurrentUser();
        FinancialGoal goal = financialGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        goal.setCurrentAmount(goal.getCurrentAmount().add(amount));

        // Update status if goal reached
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(FinancialGoal.GoalStatus.COMPLETED);
        }

        return mapToResponse(financialGoalRepository.save(goal));
    }

    private FinancialGoalResponse mapToResponse(FinancialGoal goal) {
        return FinancialGoalResponse.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .goalType(goal.getGoalType().name())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .targetDate(goal.getTargetDate())
                .priority(goal.getPriority().name())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
