package sample;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class Sample5Test {

    @Test
    public void test1() {
        Sample5 s = new Sample5(null, 5);
        assertTrue(s.test());
    }
}