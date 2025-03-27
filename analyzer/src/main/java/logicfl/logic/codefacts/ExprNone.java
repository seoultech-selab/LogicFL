package logicfl.logic.codefacts;

import logicfl.logic.FactManager;

public class ExprNone extends Expr {

    public ExprNone() {
        super("none");
    }

    @Override
    public String toString() {
        return "none";
    }

    @Override
    public String toString(String code) {
        return String.join("", PREFIX, "(", "none", FactManager.getDoubleQuotedString(code), ")");
    }
}
