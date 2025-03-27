package sample;

public class Sample2 {
    public void run() {
        Object foo = null;
        Object bar = foo;

        foo = new Object();
        bar = foo;

        foo = null;
        bar = foo;

        System.out.println(bar.toString());
    }
}
