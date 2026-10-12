package com.ridelink.farepayment;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractMongoTest {

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer(
        DockerImageName.parse("mongo:6.0.9")
    )
        .withStartupTimeout(Duration.ofSeconds(120))
        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(120)));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        // directConnection=true: MongoDBContainer runs a single-node replica set whose member
        // advertises the container-internal hostname; without this the driver switches to replica-set
        // discovery, can't reach that hostname, and every DB call times out.
        registry.add("spring.data.mongodb.uri", () -> "mongodb://" + mongoDBContainer.getHost() + ":"
                + mongoDBContainer.getMappedPort(27017) + "/ridelink_payments_test?directConnection=true");
    }
}
