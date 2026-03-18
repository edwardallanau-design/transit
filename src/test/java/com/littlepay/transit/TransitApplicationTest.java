package com.littlepay.transit;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class TransitApplicationTest {

    @Test
    void contextLoads() {
    }

    @Test
    void main_callsSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            TransitApplication.main(new String[]{});
            mocked.verify(() -> SpringApplication.run(TransitApplication.class, new String[]{}));
        }
    }
}
