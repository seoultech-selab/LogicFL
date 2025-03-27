package logicfl.logic.codefacts;

/**
 * {@code method/2} predicate to represent a method declaration.
 *
 * {@code method(?method_id, ?range:Range)}.
 *
 * <ul>
 *  <li>{@code method_id}: the declared method id.</li>
 *  <li>{@code range}: code range of the method.</li>
 * </ul>
 */
public class MethodDecl extends CodeEntity {

    public static final String PREFIX = "method";

    public MethodDecl(String methodId, Range range) {
        super(methodId, range);
    }

    @Override
    public String toString() {
        return String.join("", PREFIX, "(",
            id, ", ",
            range.toString(),
        ")");
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof MethodDecl method) {
            return id != null && id.equals(method.getId());
        }
        return false;
    }
}
