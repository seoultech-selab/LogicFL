package sample;

public class Person<T> {
    private String name;
    private String major;
    public T field;

    public Person(String major) {
        this(null, major);
    }

    public Person(String name, String major) {
        this.name = name;
        this.major = major;
    }

    public String getName() {
        return name;
    }

    public String getMajor() {
        return major == null ? new String("") : major;
    }
}