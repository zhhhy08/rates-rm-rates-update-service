package com.hertz.api.karate;

import static org.springframework.test.util.AssertionErrors.assertTrue;

import com.hertz.api.HertzMsTemplateApplication;
import com.intuit.karate.Results;
import com.intuit.karate.junit5.Karate;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {HertzMsTemplateApplication.class})
public class KarateTestRunner {

  @Autowired
  Environment environment;

  @Karate.Test
  public Karate actuatorTest() {
    Karate karate = Karate.run("classpath:karate/Placeholder.feature")
        .outputCucumberJson(true)
        .systemProperty("port", environment.getProperty("local.server.port"));
    Results results = karate.parallel(1);
    generateReport(results.getReportDir());

    assertTrue(results.getErrorMessages(), results.getFailCount() == 0);
    return karate;
  }

  private void generateReport(String karateOutputPath) {
    Collection<File> jsonFiles =
        FileUtils.listFiles(new File(karateOutputPath), new String[] {"json"}, true);
    List<String> jsonPaths = new ArrayList<>(jsonFiles.size());
    jsonFiles.forEach(file -> jsonPaths.add(file.getAbsolutePath()));
    Configuration config = new Configuration(new File("target"), "rates-rm-rates-update-service");
    ReportBuilder reportBuilder = new ReportBuilder(jsonPaths, config);
    reportBuilder.generateReports();
  }

}