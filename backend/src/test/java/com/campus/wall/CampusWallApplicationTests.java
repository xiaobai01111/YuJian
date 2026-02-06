package com.campus.wall;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CampusWallApplicationTests {

    @Test
    void contextLoads() {
        org.junit.jupiter.api.Assertions.assertTrue(true);
    }

}
