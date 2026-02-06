package ai.wealthwise.repository;

import ai.wealthwise.model.entity.Invoice;
import ai.wealthwise.model.entity.SmeBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

        List<Invoice> findBySmeBusinessOrderByDueDateDesc(SmeBusiness smeBusiness);

        List<Invoice> findBySmeBusinessAndInvoiceType(SmeBusiness smeBusiness, Invoice.InvoiceType type);

        List<Invoice> findBySmeBusinessAndStatus(SmeBusiness smeBusiness, Invoice.InvoiceStatus status);

        @Query("SELECT i FROM Invoice i WHERE i.smeBusiness = :smeBusiness AND i.invoiceType = :type " +
                        "AND i.status IN ('PENDING', 'PARTIALLY_PAID', 'OVERDUE') ORDER BY i.dueDate ASC")
        List<Invoice> findPendingByType(SmeBusiness smeBusiness, Invoice.InvoiceType type);

        @Query("SELECT i FROM Invoice i WHERE i.smeBusiness = :smeBusiness AND i.dueDate < :date " +
                        "AND i.status NOT IN ('PAID', 'CANCELLED')")
        List<Invoice> findOverdueInvoices(SmeBusiness smeBusiness, LocalDate date);

        @Query("SELECT SUM(i.totalAmount - i.paidAmount) FROM Invoice i WHERE i.smeBusiness = :smeBusiness " +
                        "AND i.invoiceType = :type AND i.status NOT IN ('PAID', 'CANCELLED')")
        BigDecimal sumOutstandingByType(SmeBusiness smeBusiness, Invoice.InvoiceType type);

        @Query("SELECT SUM(i.totalAmount - i.paidAmount) FROM Invoice i WHERE i.smeBusiness = :smeBusiness " +
                        "AND i.invoiceType = 'RECEIVABLE' AND i.dueDate < :date AND i.status = 'OVERDUE'")
        BigDecimal sumOverdueReceivables(SmeBusiness smeBusiness, LocalDate date);

        @Query("SELECT COUNT(i) FROM Invoice i WHERE i.smeBusiness = :smeBusiness AND i.status = 'OVERDUE'")
        Long countOverdueInvoices(SmeBusiness smeBusiness);

        List<Invoice> findBySmeBusinessAndInvoiceTypeAndStatusNot(SmeBusiness smeBusiness, Invoice.InvoiceType type,
                        Invoice.InvoiceStatus status);
}
