package ai.wealthwise.repository;

import ai.wealthwise.model.entity.CreditScore;
import ai.wealthwise.model.entity.SmeBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditScoreRepository extends JpaRepository<CreditScore, Long> {

    @Query("SELECT cs FROM CreditScore cs WHERE cs.smeBusiness = :smeBusiness " +
            "ORDER BY cs.assessedAt DESC LIMIT 1")
    Optional<CreditScore> findLatestBySmeBusiness(SmeBusiness smeBusiness);

    List<CreditScore> findBySmeBusinessOrderByAssessedAtDesc(SmeBusiness smeBusiness);

    @Query("SELECT cs FROM CreditScore cs WHERE cs.smeBusiness = :smeBusiness " +
            "AND cs.assessedAt >= :since ORDER BY cs.assessedAt ASC")
    List<CreditScore> findScoreHistory(SmeBusiness smeBusiness, LocalDateTime since);

    @Query("SELECT AVG(cs.overallScore) FROM CreditScore cs WHERE cs.smeBusiness.industryType = :industryType")
    Double findAverageScoreByIndustry(SmeBusiness.IndustryType industryType);

    @Query("SELECT cs FROM CreditScore cs WHERE cs.riskLevel IN ('POOR', 'CRITICAL') " +
            "AND cs.validUntil > :now")
    List<CreditScore> findHighRiskBusinesses(LocalDateTime now);
}
