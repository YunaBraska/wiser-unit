package berlin.yuna.wiserunit;

import berlin.yuna.wiserjunit.model.annotation.WiserJunitReport;
import berlin.yuna.wiserjunit.model.bdd.Bdd;
import berlin.yuna.wiserjunit.model.exception.BddException;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.stream.LongStream;

import static berlin.yuna.wiserjunit.model.bdd.BddCore.feature;
import static berlin.yuna.wiserjunit.model.bdd.BddCore.given;
import static berlin.yuna.wiserjunit.model.bdd.BddCore.where;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("UnitTest")
@WiserJunitReport
class BddTest {

    Bdd<Integer> given;

    @BeforeEach
    void setUp() {
        given = given("Something new", 11);
    }

    @AfterAll
    static void afterAll() {
        //TODO assert report
    }

    @Test
    @DisplayName("Throwable interface")
    void flow_ShouldCatchException() {
        assertThrows(
                BddException.class,
                () -> given.where("Should fail", value -> { throw new RuntimeException("Interrupted " + value); }),
                "WHERE: Should fail"
        );
    }

    @Test
    @DisplayName("Throwable interface static")
    void static_FlowShouldCatchException() {
        assertThrows(
                BddException.class,
                () -> where("Should fail", () -> { throw new RuntimeException("Interrupted"); }),
                "WHERE: Should fail"
        );
    }

    @Test
    @DisplayName("Hamcrest assertion")
    void flow_ShouldCatchHamcrest_asAssertionException() {
        assertThrows(
                BddException.class,
                () -> given.where("Should fail", value -> {
                    assertThat(value, is(nullValue()));
                    return null;
                }),
                "WHERE: Should fail"
        );
    }

    @Test
    @DisplayName("Hamcrest with match")
    void flow_ShouldCatchHamcrest_asMatchException() {
        final Matcher<Object> matcher = is(nullValue());
        assertThrows(
                BddException.class,
                () -> given.match("Should fail", matcher),
                "WHERE: Should fail"
        );
    }

    @Test
    @DisplayName("Hamcrest variable")
    void flow_ShouldCatchHamcrest_asVariableException() {
        assertThrows(
                BddException.class,
                () -> given.where("ddd", input -> is(nullValue())),
                "WHERE: Should fail"
        );
    }

    @Test
    @DisplayName("Junit assertion")
    void static_ShouldCatchJunit_asAssertionException() {
        assertThrows(
                BddException.class,
                () -> given.where("Should fail", value -> {
                    assertNull(value);
                    return value;
                }),
                "WHERE: Should fail"
        );
    }

    @Test
    @DisplayName("Junit assertion")
    void static_ShouldCatchJunit_asMatchException() {
        assertThrows(
                BddException.class,
                () -> given.match("Should fail", Assertions::assertNull),
                "WHERE: Should fail"
        );
    }

    @Test
    void fullFlowTest() {
        feature("This is a test about an unknown feature")
                .given("Input is my short phone number", 10)
                .when("Filter even numbers",
                        number -> LongStream.rangeClosed(1, number).boxed().filter(value -> value % 2 == 0).collect(toList()))

                //HAMCREST
                .match("[Hamcrest] Should contain five even numbers",
                        is(notNullValue()),
                        hasSize(5)
                )
                .then("[Hamcrest] Should contain five even numbers", value -> {
                    assertThat(value, is(notNullValue()));
                    assertThat(value, hasSize(5));
                    return value;
                })

                //JUNIT
                .match("[Junit] Should not be null", Assertions::assertNotNull)
                .match("[Junit] Should contain five even numbers", value -> {
                    assertNotNull(value);
                    assertEquals(5, value.size());
                })
                .then("[Junit] Should contain five even numbers", value -> {
                    assertNotNull(value);
                    assertEquals(5, value.size());
                    return value;
                })
                .willThrow("will trow", RuntimeException.class, () -> {
                    throw new RuntimeException("expected");
                })
                .end();
    }

    @Test
    @Disabled("I disabled it cause my boss told me")
    @DisplayName("Should be disabled")
    void shouldBeDisabledAndListed() {
    }
}
