package sample;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExampleJUnitTest {

    @Test
    public void test1() {
        Person p = new Person("Art");
        Example e = new Example(p);
        assertEquals(",", e.decorate());
    }

    @Test
    public void test2() {
        Person p = new Person(null, "Art");
        Example e = new Example(p);
        assertEquals(",", e.decorate());
    }
}