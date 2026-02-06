package ai.wealthwise.repository;

import ai.wealthwise.model.entity.FinancialProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialProductRepository extends JpaRepository<FinancialProduct, Long> {

    List<FinancialProduct> findByProductTypeAndIsActiveTrue(FinancialProduct.ProductType productType);

    List<FinancialProduct> findByIsActiveTrue();
}
