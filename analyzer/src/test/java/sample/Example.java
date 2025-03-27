package sample;

public class Example {
    private Person<?> p;
    private Object s;

    public Example(Person<?> p) {
        this(p, p.field);
    }

    public Example(Person<?> p, Object s) {
        this.p = p;
        s = p.field;
    }

    public String decorate() {
        String firstName = firstName((this.p.getName()));
        String lastName = lastName(p.getName());

        return lastName.toUpperCase() + "," + firstName;
    }

    public String firstName(String name) {
        int index = name.indexOf(' ');
        return name.substring(0, index > 0 ? index : 0);
    }

    public String lastName(String name) throws NullPointerException {
        if(name == null)
            throw new NullPointerException();
        int index = name.indexOf(' ');
        if(name != null && index >= 0 && !(index+1 < name.length()))
            return name.substring(index+1);
        else
            index = index + 1;
        name = index > 0 ?
            name.substring(0, index) :
            firstName(name);
        return null;
    }
}