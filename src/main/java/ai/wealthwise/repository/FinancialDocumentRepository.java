package ai.wealthwise.repository;

import ai.wealthwise.model.entity.FinancialDocument;
import ai.wealthwise.model.entity.SmeBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialDocumentRepository extends JpaRepository<FinancialDocument, Long> {

    List<FinancialDocument> findBySmeBusinessOrderByUploadedAtDesc(SmeBusiness smeBusiness);

    List<FinancialDocument> findBySmeBusinessAndDocumentCategory(
            SmeBusiness smeBusiness, FinancialDocument.DocumentCategory category);

    List<FinancialDocument> findBySmeBusinessAndParseStatus(
            SmeBusiness smeBusiness, FinancialDocument.ParseStatus status);

    Optional<FinancialDocument> findByIdAndSmeBusiness(Long id, SmeBusiness smeBusiness);

    @Query("SELECT fd FROM FinancialDocument fd WHERE fd.parseStatus = 'PENDING' ORDER BY fd.uploadedAt ASC")
    List<FinancialDocument> findPendingForProcessing();

    @Query("SELECT fd FROM FinancialDocument fd WHERE fd.smeBusiness = :smeBusiness " +
            "AND fd.fiscalYear = :fiscalYear ORDER BY fd.uploadedAt DESC")
    List<FinancialDocument> findByFiscalYear(SmeBusiness smeBusiness, String fiscalYear);

    boolean existsByChecksum(String checksum);
}
