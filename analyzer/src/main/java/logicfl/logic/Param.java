package logicfl.logic;

import java.util.Arrays;
import java.util.List;

/**
 * {@code param/3} predicate.
 *
 * {@code param(?param_id, ?i:int, ?method_id).}
 *
 * <ul>
 *  <li>param_id: the id of the parameter.</li>
 *  <li>i: the index of the parameter.</li>
 *  <li>method_id: the id of the method.</li>
 * </ul>
 */
public class Param extends Predicate {

    private String paramId;
    private int index;
    private String methodId;

    /**
     * {@code param(?param_id, ?i:int, ?m_name).}
     * 
     * @param paramId the id of the parameter.
     * @param index the index of the parameter.
     * @param methodId the id of th emethod.
     */
    public Param(String paramId, int index, String methodId) {
        this.paramId = paramId == null ? "param" : paramId;
        this.index = index;
        this.methodId = methodId == null ? "method" : methodId;
    }

    public String getParamId() {
        return paramId;
    }

    public int getIndex() {
        return index;
    }

    public String getMethodId() {
        return methodId;
    }

    @Override
    public String getPredicateName() {
        return "param";
    }

    @Override
    public List<String> arguments() {
        return Arrays.asList(paramId, String.valueOf(index), methodId);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Param p) {
            return p.getParamId().equals(paramId)
                && p.getIndex() == index
                && p.getMethodId().equals(methodId);
        }
        return false;
    }
}
