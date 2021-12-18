package exceptions;

public class SemanticErrorException extends Exception {

    /**
     * for RuntimeException
     */
    private static final long serialVersionUID = 1L;

    //
    // Constructors
    //
    public SemanticErrorException()
    {
        super("Semantic Error");
    }
    public SemanticErrorException(String message)
    {
        super("Semantic Error: \n" + message);
    }
}
