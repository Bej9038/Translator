import exceptions.InvalidInputException;

import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Main class for Jott translator program.
 *
 * @author Brendan Kapp, Christian Kobb, Derek Chan, Ben Jordan
 */
public final class Jott {
    private static final Logger LOG = Logger.getLogger(Jott.class.getName());

    public static final String C_LANG = "C";
    public static final String JAVA_LANG = "Java";
    public static final String JOTT_LANG = "Jott";
    public static final String PYTHON_LANG = "Python";

    public enum TranslationLanguage {C, JAVA, JOTT, PYTHON};

    /**
     * Entry point for the Jott application.
     *
     * @param args expected args: [input_file_name], [output_file_name], [language_to_convert_too]
     */
    public static void main(String[] args) {
        try {
            if (args.length != 3)
            {
                badArguments(args);
            }
            TranslationLanguage translationLanguage = parseLanguage(args[2]);
            String inputFileName = parseFileName(args[0]);
            String outputFileName = parseFileName(args[1]);
            String outfile = "";

            ArrayList<Token> tokens = JottTokenizer.tokenize(inputFileName);
            if (tokens != null){
                JottTree root = null;
                root = JottParser.parse(tokens);
                if(!root.validateTree()){
                    System.exit(1);
                }
                if (root != null){
                    try {
                        File f = new File(outputFileName);

                        // Uncomment to send output to a file
                        PrintStream out = new PrintStream(f);
                        System.setOut(out);

                        outfile = f.getName();
                        if (outfile.indexOf(".") > 0)
                        {
                            outfile = outfile.substring(0, outfile.lastIndexOf("."));
                        }
                    }
                    catch(Exception e)
                    {
                        System.err.println(e.getMessage());
                        System.exit(1);
                    }
                    switch(translationLanguage){ //Add Phase 4 stuff here
                        case JOTT:
                            String jottoutput = root.convertToJott();
//                            System.out.println("Tree Output: ");
                            System.out.println(jottoutput); //Eventually this needs to write a file.
                            break;
                        case C:
                            String coutput = root.convertToC();
//                            System.out.println("Tree Output: ");
                            System.out.println(coutput);
                            break;
                        case JAVA:
//                            System.out.println("Java output: ");
                            String joutput = root.convertToJava();
                            String javaoutput = joutput.replaceAll("public class Output", "Public class " + outfile);
//                            System.out.println("Tree Output: ");
                            System.out.println(javaoutput);
                            break;
                        case PYTHON:
                            String poutput = root.convertToPython();
//                            System.out.println("Tree Output: ");
                            System.out.println(poutput);
                            break;
                    }
                }
                else {
                    System.out.println("no root");
                }
            }
        } catch (InvalidInputException iie)
        {
            System.err.println(iie.getMessage());
        }
    }

    /**
     * Parse a valid filename out of the argument input.
     * @param fileNameArgument argument to parse.
     * @return valid filename.
     * @throws InvalidInputException
     */
    private static String parseFileName(String fileNameArgument) throws InvalidInputException
    {
        try {
            Paths.get(fileNameArgument);
        } catch (InvalidPathException | NullPointerException ex) 
        {
            throw new InvalidInputException("Filename is invalid: " + fileNameArgument);
        }
        return fileNameArgument;
    }

    /**
     * Parse the language if valid.
     * @param language the language argument to parse.
     * @return the valid translation language.
     * @throws InvalidInputException
     */
    private static TranslationLanguage parseLanguage(String language) throws InvalidInputException
    {
        switch (language)
        {
            case C_LANG:
                return TranslationLanguage.C;
            case JAVA_LANG:
                return TranslationLanguage.JAVA;
            case JOTT_LANG:
                return TranslationLanguage.JOTT;
            case PYTHON_LANG:
                return TranslationLanguage.PYTHON;
            default:
                throw new InvalidInputException("Invalid language: " + language);
        }
    }

    /**
     * Throws an exception stating incorrect input arguments.
     * @throws InvalidInputException
     */
    private static void badArguments(String[] args) throws InvalidInputException
    {
        String sumArgs = "";
        for (int i = 0; i < args.length; i++)
        {
            sumArgs += "[" + args[i] + "] ";
        }
        throw new InvalidInputException("Incorrect Arguments: " + sumArgs + "Expected: [input_file_name], [output_file_name], [language_to_convert_too]");
    }
}
