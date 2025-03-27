package sample;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Sample {

    private int num;
    private String str;

    public Sample() {
        this.num = 0;
        str = null;
    }

    public int getNum() {
        return num;
    }

    public String getStr() {
        return str;
    }

    public boolean run() {
        try (BufferedReader br = Files.newBufferedReader(Paths.get("src/test/resources/config.sample.properties"))) {
            String line = br.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Sample> list = new ArrayList<>();
        for(int i=0; i<5; i++)
            list.add(new Sample());
        for(Sample s : list) { System.out.println(s.toString()); };
        Iterator<Sample> it = list.iterator();
        int num = 0;
        while(num < 10
            && it.hasNext()) {
            num += it.next().getNum();
            if(num >= 10) {
                num = num - 10;
                break;
            }
        }
        list.forEach((s) -> {
            if(s.num >= 0) {
                String str = s.toString();
                s.num = str.length();
            }
        });
        if(list.size() < 0)
            num = list.size();
        else
            list.add(list.get(0));
        String test = getStr() == null ? null : getStr();
        test = test == null ? test : "";
        System.out.println(
                test.length());
        return true;
    }
}