package exceptions;

public class SyntaxErrorException extends Exception {

    /**
     * for RuntimeException
     */
    private static final long serialVersionUID = 1L;

    //
    // Constructors
    //
    public SyntaxErrorException()
    {
        super("Syntax Error");
    }
    public SyntaxErrorException(String message)
    {
        super("Syntax Error: \n" + message);
    }
}
