package net.afpro.dorm;

public class DormParseException extends Exception {
    public DormParseException() {
    }

    public DormParseException(String message) {
        super(message);
    }

    public DormParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DormParseException(Throwable cause) {
        super(cause);
    }

    public DormParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
