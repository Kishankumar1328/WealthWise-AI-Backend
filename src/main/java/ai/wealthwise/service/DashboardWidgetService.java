package ai.wealthwise.service;

import ai.wealthwise.model.entity.*;
import ai.wealthwise.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardWidgetService {

    private final DashboardWidgetRepository widgetRepository;
    private final UserRepository userRepository;

    @Transactional
    public List<DashboardWidget> initDefaultWidgets(Long userId) {
        if (userId == null)
            throw new IllegalArgumentException("User ID cannot be null");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!widgetRepository.findByUserOrderByPositionOrderAsc(user).isEmpty()) {
            return widgetRepository.findByUserOrderByPositionOrderAsc(user);
        }

        DashboardWidget w1 = DashboardWidget.builder()
                .user(user)
                .widgetType(DashboardWidget.WidgetType.CASH_FLOW_CHART)
                .title("Cash Flow Overview")
                .positionOrder(1)
                .widthCols(2)
                .build();

        DashboardWidget w2 = DashboardWidget.builder()
                .user(user)
                .widgetType(DashboardWidget.WidgetType.REVENUE_TREND)
                .title("Monthly Revenue")
                .positionOrder(2)
                .widthCols(1)
                .build();

        DashboardWidget w3 = DashboardWidget.builder()
                .user(user)
                .widgetType(DashboardWidget.WidgetType.AI_INSIGHTS)
                .title("AI Advisor")
                .positionOrder(3)
                .widthCols(3)
                .build();

        return widgetRepository.saveAll(java.util.Objects.requireNonNull(Arrays.asList(w1, w2, w3)));
    }

    public List<DashboardWidget> getUserWidgets(Long userId) {
        if (userId == null)
            throw new IllegalArgumentException("User ID cannot be null");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return widgetRepository.findByUserOrderByPositionOrderAsc(user);
    }
}
