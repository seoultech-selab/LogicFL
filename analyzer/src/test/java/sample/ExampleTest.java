package sample;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ExampleTest {

    @Test
    public void test1() {
        Person p = new Person("Art");
        Example e = new Example(p);
        e.lastName("ABC DEF");
        assertEquals(",", e.decorate());
    }

    @Test
    public void test2() {
        Person p = new Person(null, "Art");
        Example e = new Example(p);
        assertEquals(",", e.decorate());
    }
}