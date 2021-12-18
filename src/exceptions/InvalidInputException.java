package exceptions;

public class InvalidInputException extends Exception {

    /**
     * for RuntimeException
     */
    private static final long serialVersionUID = 1L;

    //
    // Constructors
    //
    public InvalidInputException()
    {
        super("Invalid Input");
    }
    public InvalidInputException(String message)
    {
        super("Invalid Input: " + message);
    }
}
