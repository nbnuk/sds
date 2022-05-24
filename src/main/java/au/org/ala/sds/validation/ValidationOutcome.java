/**
 *
 */
package au.org.ala.sds.validation;

import au.org.ala.sds.model.SensitivityInstance;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ValidationOutcome {

    private boolean valid;
    private boolean sensitive = false;
    private boolean loadable = false;
    private boolean controlledAccess = false;
    private List<SensitivityInstance> instances;
    private ValidationReport report;
    private Map<String, Object> result;

    public ValidationOutcome(){
        this.valid = true;
    }

    public ValidationOutcome(ValidationReport report) {
        this.report = report;
        this.valid = true;
    }

    public ValidationOutcome(ValidationReport report, boolean valid) {
        this.report = report;
        this.valid = valid;
    }

    public ValidationReport getReport() {
        return report;
    }

    public void setReport(ValidationReport report) {
        this.report = report;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public boolean isLoadable() {
        return loadable;
    }

    public void setLoadable(boolean loadable) {
        this.loadable = loadable;
    }

    public void setControlledAccess(boolean controlledAccess){
        this.controlledAccess = controlledAccess;
    }

    public List<SensitivityInstance> getInstances() {
        return this.instances;
    }

    public void setInstances(List<SensitivityInstance> instances) {
        this.instances = instances;
    }

    public boolean isControlledAccess(){
        return this.controlledAccess;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "ValidationOutcome{" +
                "valid=" + valid +
                ", sensitive=" + sensitive +
                ", loadable=" + loadable +
                ", instances=" + instances +
                ", report=" + report +
                ", result=" + result +
                '}';
    }
}
