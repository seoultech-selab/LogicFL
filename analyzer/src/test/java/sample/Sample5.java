package sample;

public class Sample5 {
    public String field1;
    public int field2;

    public Sample5(String field1, int field2) {
        this.field1 = field1;
        this.field2 = field2;
    }

    public boolean test() {
        method1();
        method1();
        method1();
        method2();
        method3();


        return field1.contains("");
    }

    private void method1() {
        int count = 0;
        if(field2 > 0) {
            count = 0;
        } else if(field2 == 0) {
            count = field2 - 1;
        } else {
            count = field2 + 2;
        }
        field2 = count;
    }

    private int method2() {
        String s = field1 + field1 + field2;

        return s.length();
    }

    private void method3() {
        int count = 0;
        field2 = 5;
        if(field2 < 0) {
            count = 1;
        } else if(count < field2) {
            for(int i=count; i < field2; i++) {
                count += 2;
            }
        }
    }
}