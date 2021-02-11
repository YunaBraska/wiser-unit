package com.berlin.yuna;

import berlin.yuna.wiserjunit.model.annotation.WiserJunitReport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestReporter;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
@Disabled("Its just a playground")
@WiserJunitReport
class BddPlayground {

    @AfterEach
    void after(TestInfo testInfo) {
        String callingTest = testInfo.getTestMethod().get().getName();
    }

    @AfterAll
    static void afterAll() {

    }

    @Tag("login")
    @DisplayName("RepeatingTest")
    @RepeatedTest(value = 5, name = "{displayName} {currentRepetition}/{totalRepetitions}")
    void customDisplayName(RepetitionInfo repInfo, TestInfo testInfo) {
        int i = 3;
        System.out.println(testInfo.getDisplayName() +
                " --> " + repInfo.getCurrentRepetition()
        );

        assertEquals(repInfo.getCurrentRepetition(), i);
    }

    @Test
    @Tag("search")
    void searchTest() {
    }

    @Test
    @Tag("search")
    void searchTest2() {
    }

    @Test
    void reportMultipleKeyValuePairs(TestReporter testReporter) {
        Map<String, String> values = new HashMap<>();
        values.put("user name", "dk38");
        values.put("award year", "1974");

        testReporter.publishEntry(values);
    }
}
