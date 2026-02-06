package ai.wealthwise.repository;

import ai.wealthwise.model.entity.EWayBill;
import ai.wealthwise.model.entity.SmeBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EWayBillRepository extends JpaRepository<EWayBill, Long> {
    List<EWayBill> findBySmeBusiness(SmeBusiness smeBusiness);

    List<EWayBill> findBySmeBusinessAndStatus(SmeBusiness smeBusiness, String status);
}
