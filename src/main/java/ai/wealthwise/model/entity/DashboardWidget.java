package ai.wealthwise.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dashboard_widgets", indexes = {
        @Index(name = "idx_widget_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardWidget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "widget_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private WidgetType widgetType;

    @Column(name = "title")
    private String title;

    @Column(name = "position_order")
    private Integer positionOrder; // For sorting layout

    @Column(name = "width_cols")
    private Integer widthCols; // e.g. 1=small, 2=medium, 3=full width

    @Column(name = "settings_json", columnDefinition = "TEXT")
    private String settingsJson; // Custom configurations

    public enum WidgetType {
        CASH_FLOW_CHART,
        EXPENSE_BREAKDOWN,
        REVENUE_TREND,
        PROFIT_MARGIN,
        WORKING_CAPITAL_GAUGE,
        NOTIFICATIONS,
        AI_INSIGHTS
    }
}
