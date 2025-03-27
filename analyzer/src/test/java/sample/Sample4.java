package sample;

import java.util.List;

public class Sample4 {

    public String field1;
    public int field2;

    public Sample4(String field1, int field2) {
        this.field1 = field1;
        this.field2 = field2;
    }

    public String generate() {
        StringBuffer sb2 = new StringBuffer();
        for(int i=0; i<field2; i++)
            sb2.append(field1);

        StringBuffer sb = new StringBuffer();
        method1(sb);
        sb = new StringBuffer();
        method2(sb);
        method3();
        method4();
        method5();

        if(field1.length() > 0)
            return sb2.toString();
        else
            return null;
    }

    private void method1(StringBuffer sb) {
        int count = 0;
        do {
            sb.append(field1);
            if(field1 == null) {
                continue;
            }
            count++;
        } while((Boolean)(count++ < field2 * 2));
    }

    private void method2(StringBuffer sb) {
        int i, b;
        for(i=field2-field2, b=i+1; i < field2 && b <= field2*2 ; i++, b=b+2) {
            sb.append(field1);
        }
        field2 += 1;
    }

    private void method3() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);
        for(int x : list.subList(0, 2)) {
            field2 += x;
        }
    }

    private void method4() {
        int count = 0;
        int i = 5;
        while(count++ < i+1) {}
        do {} while(count-- > 0);
        for(int j=0, k=j+1;
            j < k;
            j++) {}
    }

    private void method5() {
        Sample4 s1 = new Sample4("1", 1);
        Sample4 s2 = new Sample4("2", 2);
        Sample4 s3 = new Sample4("3", 3);
        s1.field1 = this.field1;
        s2.field2 = s3.field2;
        this.field2 = s1.field2;
        s2.field2 = s1.field2;
    }
}