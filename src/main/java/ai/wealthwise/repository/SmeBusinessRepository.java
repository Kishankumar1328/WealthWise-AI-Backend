package ai.wealthwise.repository;

import ai.wealthwise.model.entity.SmeBusiness;
import ai.wealthwise.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmeBusinessRepository extends JpaRepository<SmeBusiness, Long> {

    Optional<SmeBusiness> findByUserAndIsActiveTrue(User user);

    List<SmeBusiness> findAllByUserAndIsActiveTrue(User user);

    Optional<SmeBusiness> findByGstin(String gstin);

    Optional<SmeBusiness> findByIdAndUser(Long id, User user);

    boolean existsByGstin(String gstin);

    @Query("SELECT s FROM SmeBusiness s WHERE s.user = :user AND s.isActive = true ORDER BY s.createdAt DESC")
    List<SmeBusiness> findActiveBusinessesByUser(User user);

    @Query("SELECT s FROM SmeBusiness s WHERE s.industryType = :industryType AND s.isActive = true")
    List<SmeBusiness> findByIndustryType(SmeBusiness.IndustryType industryType);
}
