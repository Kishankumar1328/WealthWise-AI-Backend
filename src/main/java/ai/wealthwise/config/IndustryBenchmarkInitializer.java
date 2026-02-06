package ai.wealthwise.config;

import ai.wealthwise.model.entity.IndustryBenchmark;
import ai.wealthwise.repository.IndustryBenchmarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IndustryBenchmarkInitializer implements CommandLineRunner {

    private final IndustryBenchmarkRepository benchmarkRepository;

    @Override
    public void run(String... args) {
        if (benchmarkRepository.count() == 0) {
            log.info("Seeding industry benchmarks for cost optimization...");

            List<IndustryBenchmark> benchmarks = Arrays.asList(
                    // IT & Technology
                    createBenchmark("IT_TECHNOLOGY", "Cloud Services", 2.0, 5.0, 3.5),
                    createBenchmark("IT_TECHNOLOGY", "Software Licenses", 1.0, 3.0, 2.0),
                    createBenchmark("IT_TECHNOLOGY", "Office Rent", 5.0, 12.0, 8.0),
                    createBenchmark("IT_TECHNOLOGY", "Marketing", 10.0, 25.0, 15.0),

                    // Retail
                    createBenchmark("RETAIL", "Inventory Logistics", 5.0, 10.0, 7.5),
                    createBenchmark("RETAIL", "Rent", 10.0, 20.0, 15.0),
                    createBenchmark("RETAIL", "Marketing", 2.0, 8.0, 5.0),
                    createBenchmark("RETAIL", "Utilities", 1.0, 3.0, 2.0),

                    // Manufacturing
                    createBenchmark("MANUFACTURING", "Raw Materials", 40.0, 60.0, 50.0),
                    createBenchmark("MANUFACTURING", "Energy / Utilities", 5.0, 15.0, 10.0),
                    createBenchmark("MANUFACTURING", "Maintenance", 2.0, 7.0, 4.5),

                    // Services
                    createBenchmark("SERVICES", "Professional Fees", 2.0, 8.0, 5.0),
                    createBenchmark("SERVICES", "Marketing", 5.0, 15.0, 10.0),
                    createBenchmark("SERVICES", "Software & Tools", 1.0, 5.0, 3.0));

            if (benchmarks != null && !benchmarks.isEmpty()) {
                benchmarkRepository.saveAll(benchmarks);
                log.info("Successfully seeded {} industry benchmarks.", benchmarks.size());
            }
        }
    }

    private IndustryBenchmark createBenchmark(String sector, String category, double low, double high, double avg) {
        return IndustryBenchmark.builder()
                .sector(sector)
                .expenseCategory(category)
                .benchmarkRatioLow(BigDecimal.valueOf(low))
                .benchmarkRatioHigh(BigDecimal.valueOf(high))
                .benchmarkRatioAvg(BigDecimal.valueOf(avg))
                .description("Industry standard for " + category + " in " + sector)
                .source("Global SME Benchmarks 2025")
                .build();
    }
}
