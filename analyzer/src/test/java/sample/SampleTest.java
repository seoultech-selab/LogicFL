package sample;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SampleTest {

    @Test
    public void test1() {
        Sample sample = new Sample();
        assertTrue(sample.run());
    }
}