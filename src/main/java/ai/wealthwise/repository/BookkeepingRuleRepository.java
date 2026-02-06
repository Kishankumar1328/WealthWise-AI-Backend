package ai.wealthwise.repository;

import ai.wealthwise.model.entity.BookkeepingRule;
import ai.wealthwise.model.entity.SmeBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookkeepingRuleRepository extends JpaRepository<BookkeepingRule, Long> {

    List<BookkeepingRule> findByBusinessOrderByPriorityDesc(SmeBusiness business);

    List<BookkeepingRule> findByBusinessAndIsActiveTrueOrderByPriorityDesc(SmeBusiness business);
}
