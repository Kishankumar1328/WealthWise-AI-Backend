package ai.wealthwise.repository;

import ai.wealthwise.model.entity.User;
import ai.wealthwise.model.entity.DashboardWidget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {
    List<DashboardWidget> findByUserOrderByPositionOrderAsc(User user);
}
