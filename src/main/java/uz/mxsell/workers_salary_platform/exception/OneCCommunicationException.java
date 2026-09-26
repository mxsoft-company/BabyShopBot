package uz.mxsell.workers_salary_platform.exception;

public class OneCCommunicationException extends RuntimeException {
    public OneCCommunicationException(String message) {
        super(message);
    }

    public OneCCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
