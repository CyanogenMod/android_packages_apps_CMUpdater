package cmupdaterapp.customExceptions;

public class NotEnoughSpaceException extends Exception {
    private static final long serialVersionUID = 658447306729869141L;

    public NotEnoughSpaceException(String msg) {
        super(msg);
    }
}