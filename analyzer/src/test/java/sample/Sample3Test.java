package sample;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class Sample3Test {

    @Test
    public void test1() {
        Sample3 sample = new Sample3("string", 1, null);
        String source = "public class Test { public void method1() {} \n public void method2() {} }";
        List<String> methods = sample.getMethods(source);
        assertEquals(2, methods.size());
        assertEquals("string/1/null", sample.toString());
    }
}