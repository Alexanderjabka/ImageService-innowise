package io.github.alexanderjabka.imageservice;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.github.alexanderjabka.imageservice.config.TestSecurityConfig;
import io.github.alexanderjabka.imageservice.service.ImageEventsProducer;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    protected ImageEventsProducer imageEventsProducer;

    protected static PostgreSQLContainer<?> postgresContainer;
    protected static LocalStackContainer localStackContainer;
    protected static AmazonS3 s3Client;

    static {
        postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
        postgresContainer.start();

        localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
                .withServices(S3);
        localStackContainer.start();
    }

    @Value("${cloud.aws.s3.bucket}")
    protected String bucketName;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);

        registry.add("cloud.aws.s3.endpoint", () -> localStackContainer.getEndpointOverride(S3).toString());
        registry.add("cloud.aws.region.static", localStackContainer::getRegion);
        registry.add("cloud.aws.credentials.access-key", localStackContainer::getAccessKey);
        registry.add("cloud.aws.credentials.secret-key", localStackContainer::getSecretKey);
    }

    @BeforeAll
    static void setupS3Client() {
        s3Client = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                localStackContainer.getEndpointOverride(S3).toString(),
                                localStackContainer.getRegion()
                        )
                )
                .withCredentials(
                        new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials(
                                        localStackContainer.getAccessKey(),
                                        localStackContainer.getSecretKey()
                                )
                        )
                )
                .withPathStyleAccessEnabled(true)
                .build();
    }
}
