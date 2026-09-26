package uz.mxsell.workers_salary_platform.exception;

public class DuplicateBotTokenException extends RuntimeException {
    public DuplicateBotTokenException(String message) {
        super(message);
    }
}
