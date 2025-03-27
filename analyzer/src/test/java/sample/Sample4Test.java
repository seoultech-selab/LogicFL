package sample;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class Sample4Test {

    @Test
    public void test1() {
        Sample4 s = new Sample4("a", 5);
        assertEquals("aaaaa", s.generate());
        s = new Sample4(null, 2);
        assertEquals("nullnull", s.generate());
    }
}