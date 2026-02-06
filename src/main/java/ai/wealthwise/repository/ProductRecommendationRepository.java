package ai.wealthwise.repository;

import ai.wealthwise.model.entity.ProductRecommendation;
import ai.wealthwise.model.entity.SmeBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRecommendationRepository extends JpaRepository<ProductRecommendation, Long> {

    List<ProductRecommendation> findByBusinessOrderByMatchScoreDesc(SmeBusiness business);

    List<ProductRecommendation> findByBusinessAndStatus(SmeBusiness business, ProductRecommendation.Status status);
}
