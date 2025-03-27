package logicfl.logic;

import java.util.List;

public abstract class Predicate {

    public String getPredicateName() {
        return "pred";
    }

    public String createTerm() {
        String argStr = String.join(", ", arguments());
        return String.join("", getPredicateName(), "(", argStr, ")");
    }

    public abstract List<String> arguments();

    @Override
    public String toString() {
        return createTerm();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Predicate p) {
            return p.getPredicateName().equals(this.getPredicateName())
                && p.arguments().equals(this.arguments());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public String toFactString() {
        return createTerm() + ".";
    }
}
