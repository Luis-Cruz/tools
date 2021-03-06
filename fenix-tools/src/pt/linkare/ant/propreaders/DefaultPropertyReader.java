package pt.linkare.ant.propreaders;

import pt.linkare.ant.InvalidPropertySpecException;

public class DefaultPropertyReader extends AbstractPropertyReader {

    public DefaultPropertyReader() {
        super();
    }

    @Override
    public String readProperty() throws InvalidPropertySpecException {
        return readPropertySimple();
    }

    private String readPropertySimple() throws InvalidPropertySpecException {
        if (getProperty().getPropertyDefaultValue() == null) {
            throw new InvalidPropertySpecException("DefaultGUIPropertyReader needs a defaultValue for the property");
        } else {
            return getProperty().getPropertyDefaultValue();
        }
    }

}
