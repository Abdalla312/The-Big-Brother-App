package com.expensetracker.big_brother;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestContainersConfiguration.class)
class BigBrotherApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {
    }

}
