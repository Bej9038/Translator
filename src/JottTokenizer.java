/**
 * This class is responsible for tokenizing Jott code.
 * 
 * @author Brendan Kapp
 **/

import exceptions.*;

import java.util.ArrayList;
import java.io.*;

public class JottTokenizer {

    private enum TokenizerMode {GENERAL, INTEGER, FLOAT, ID_KEYWORD, STRING, STRING_END, COMMENT};

    private static final int ASCII_NEWLINE = 10;
    private static final int ASCII_PERIOD = 46;
    private static final int ASCII_DOUBLE_QUOTE = 34;
    private static final int ASCII_SPACE = 32;
    private static final int ASCII_EQUALS = 61;
    private static final int ASCII_GREATER_THAN = 60;
    private static final int ASCII_LESS_THAN = 62;
    private static final int ASCII_TAB = 9;

    /**
     * Takes in a filename and tokenizes that file into Tokens
     * based on the rules of the Jott Language
     * @param filename the name of the file to tokenize; can be relative or absolute path
     * @return an ArrayList of Jott Tokens
     */
    public static ArrayList<Token> tokenize(String filename)
    {
        InputStream istream;
		int nextChar; // the char that was just read
        int lineNum = 1; // the current line number in the file
        int prevChar;
        boolean hasError = false; // should we return a token list or null
        ArrayList<Token> tokens = new ArrayList<>();
		final int EOF = -1;
		try {
			File inputFile = new File(filename);
			istream = new FileInputStream(inputFile);
			try {
                currentString = "";
                mode = TokenizerMode.GENERAL;
				while ((nextChar = istream.read()) != EOF)
                {
                    if (nextChar == ASCII_NEWLINE)
                    {
                        lineNum++;
                        // new line, must tokenize rest of current line
                        Token newToken = tokenizeCurrentString((char)nextChar, filename, lineNum);
                        if (newToken == null)
                        {
                            // check if there wasn't just a comment
                            // check if the current string isn't just a newline
                            if (!justDumpComment && currentString.length() != 0 && currentString.length() != 1)
                            {
                                throw new SyntaxErrorException("Cannot tokenize: " + currentString + "\n" + filename + ":" + lineNum);
                            } else {
                                // either there was just a comment dump
                                // or the current string is just a newline
                                justDumpComment = false;
                                currentString = "";
                            }
                        } else {
                            // new token, add it
                            tokens.add(newToken);
                        }
                    } else {
                        //tokenize current string with the next char
                        Token newToken = tokenizeCurrentString((char)nextChar, filename, lineNum);
                        if (newToken != null) tokens.add(newToken);
                    }
                    prevChar = nextChar;
                }
                // call a final tokenize, nextChar is EOF
                Token newToken = tokenizeCurrentString((char)nextChar, filename, lineNum);
                if (newToken == null)
                {
                    // check if there wasn't just a comment
                    // check if the current string isn't just a newline
                    if (!justDumpComment && currentString.length() != 1 && mode != TokenizerMode.COMMENT)
                    {
                        throw new SyntaxErrorException("Cannot tokenize: " + currentString + "\n" + filename + ":" + lineNum);
                    } else {
                        // either there was just a comment dump
                        // or the current string is just a newline
                    }
                } else {
                    // new token, add it
                    tokens.add(newToken);
                }
                justDumpComment = false;
                currentString = "";
			} catch (IOException e)
            {
				System.err.println("Error: " + e.getMessage());
			} catch (SyntaxErrorException see)
            {
                System.err.println(see.getMessage());
                hasError = true;
            }finally 
            {
				try {
					istream.close();
				} catch (IOException e)
                {
					System.err.println("File did not close");
				}
			}
		} catch (FileNotFoundException e)
        {
			e.printStackTrace();
			System.exit(1);
		}
        return !hasError ? tokens : null;   
    }
    /**
     * Check if c is a digit.
     * @param c the char to check.
     * @return true if c is a digit, false if it is not.
     */
    private static boolean isDigit(char c)
    {
        return (c >= 48 && c <= 57);
    }
    /**
     * Check if c is a letter.
     * @param c the char to check.
     * @return true if c is a letter, false otherwise.
     */
    private static boolean isLetter(char c)
    {
        return ((c >= 65 && c <= 90) || (c >= 97 && c <= 122));
    }
    private static String currentString = ""; // the current string, holds state between runs of tokenizeCurrentString()
    private static TokenizerMode mode = TokenizerMode.GENERAL; // DFA current state
    private static boolean justDumpComment = false; // flag for if a comment was just dumped
    /**
     * Tokenizes characters by slowly adding them and routing them through a simplified version
     * of the DFA. TokenizerMode contains all the DFA states.
     * The basic format of this function is to first check if we can make a token with the current string.
     * Then if we can, tokenize it, prep for the next token by adding nextChar to the new string, and returning the new token.
     * If we can't, add the nextChar to the string and return null.
     * @param nextChar the next char that has been read, will be added to current string at the end of the function
     * @param filename the filename that nextChar is from
     * @param lineNum the linnum that nextChar is on
     * @return the next token, or null if nothing to tokenize
     * @throws SyntaxErrorException
     */
    private static Token tokenizeCurrentString(char nextChar, String filename, int lineNum) throws SyntaxErrorException
    {
        Token newToken = null;
        if (mode == TokenizerMode.INTEGER)
        {
            // if it's a digit, set mode to digit
            //  if next is not a digit or a . tokenize, otherwise return null
            if (isDigit(nextChar))
            {
                // keep going
            } else if (nextChar == ASCII_PERIOD)
            {
                mode = TokenizerMode.FLOAT;
                // keep going
            } else {
                // tokenize
                newToken = new Token(currentString, filename, lineNum, TokenType.NUMBER);
            }
        } else if (mode == TokenizerMode.FLOAT)
        {
            if (isDigit(nextChar))
            {
                // keep going
            } else {
                // tokenize
                // check if the token is just a period, if so fail
                if (!currentString.equals("."))
                {
                    newToken = new Token(currentString, filename, lineNum, TokenType.NUMBER);
                }
            }
        } else if (mode == TokenizerMode.ID_KEYWORD)
        {
            if (isDigit(nextChar) || isLetter(nextChar))
            {
                // keep going
            } else {
                // tokenize
                newToken = new Token(currentString, filename, lineNum, TokenType.ID_KEYWORD);
            }
        } else if (mode == TokenizerMode.STRING)
        {
            if (isDigit(nextChar) || isLetter(nextChar) || nextChar == ASCII_SPACE || nextChar == ASCII_TAB)
            {
                // keep going
            } else if (nextChar == ASCII_DOUBLE_QUOTE)
            {
                mode = TokenizerMode.STRING_END;
                // keep going
            } else
            {
                // error, invalid string
                throw new SyntaxErrorException("Invalid char: " + nextChar + " in string");
            }
        } else if (mode == TokenizerMode.STRING_END)
        {
            // tokenize
            mode = TokenizerMode.GENERAL;
            newToken = new Token(currentString, filename, lineNum, TokenType.STRING);
        } else if (mode == TokenizerMode.GENERAL)
        {
            switch (currentString)
            {
                case ",":
                    newToken = new Token(currentString, filename, lineNum, TokenType.COMMA);
                    break;
                case "]":
                    newToken = new Token(currentString, filename, lineNum, TokenType.R_BRACKET);
                    break;
                case "[":
                    newToken = new Token(currentString, filename, lineNum, TokenType.L_BRACKET);
                    break;
                case "}":
                    newToken = new Token(currentString, filename, lineNum, TokenType.R_BRACE);
                    break;
                case "{":
                    newToken = new Token(currentString, filename, lineNum, TokenType.L_BRACE);
                    break;
                case "=":
                    if (nextChar != ASCII_GREATER_THAN && nextChar != ASCII_EQUALS && nextChar != ASCII_LESS_THAN)
                    {
                        newToken = new Token(currentString, filename, lineNum, TokenType.ASSIGN);
                    }
                    break;
                case "==":
                    newToken = new Token(currentString, filename, lineNum, TokenType.REL_OP);
                    break;
                case "<":
                    if (nextChar != ASCII_EQUALS)
                    {
                        newToken = new Token(currentString, filename, lineNum, TokenType.REL_OP);
                    }
                    break;
                case ">":
                    if (nextChar != ASCII_EQUALS)
                    {
                        newToken = new Token(currentString, filename, lineNum, TokenType.REL_OP);
                    }
                    break;
                case "<=":
                    newToken = new Token(currentString, filename, lineNum, TokenType.REL_OP);
                    break;
                case ">=":
                    newToken = new Token(currentString, filename, lineNum, TokenType.REL_OP);
                    break;
                case "/":
                    newToken = new Token(currentString, filename, lineNum, TokenType.MATH_OP);
                    break;
                case "+":
                    newToken = new Token(currentString, filename, lineNum, TokenType.MATH_OP);
                    break;
                case "-":
                    newToken = new Token(currentString, filename, lineNum, TokenType.MATH_OP);
                    break;
                case "*":
                    newToken = new Token(currentString, filename, lineNum, TokenType.MATH_OP);
                    break;
                case ";":
                    newToken = new Token(currentString, filename, lineNum, TokenType.SEMICOLON);
                    break;
                case ":":
                    newToken = new Token(currentString, filename, lineNum, TokenType.COLON);
                    break;
                case "!=":
                    newToken = new Token(currentString, filename, lineNum, TokenType.REL_OP);
                    break;
                case "#":
                    mode = TokenizerMode.COMMENT;
                    break;
            }
        }
        // dump the current string if the comment is over
        if (mode == TokenizerMode.COMMENT && nextChar == ASCII_NEWLINE)
        {
            currentString = "";
            justDumpComment = true;
        }
        // if a new token has been created, or there's no string
        if (newToken != null || currentString.length() == 0)
        {
            // check if we should head into one of other states
            // if the newChar = int, then number
            // if the newChar = letter, then id_keyword
            // if the newChar = quote, then string
            if (isDigit(nextChar))
            {
                mode = TokenizerMode.INTEGER;
            } else if (nextChar == ASCII_PERIOD)
            {
                mode = TokenizerMode.FLOAT;
            } else if (isLetter(nextChar))
            {
                mode = TokenizerMode.ID_KEYWORD;
            } else if (nextChar == ASCII_DOUBLE_QUOTE)
            {
                mode = TokenizerMode.STRING;
            } else {
                mode = TokenizerMode.GENERAL;
            }
        }
        // at the end, if no token is made, add to string, otherwise reset string
        if (newToken == null)
        {
            if (((nextChar != ASCII_SPACE && nextChar != ASCII_TAB) || mode == TokenizerMode.STRING) && nextChar != ASCII_NEWLINE) currentString += nextChar;
        } else 
        {
            if (nextChar != ASCII_SPACE && nextChar != ASCII_TAB && nextChar != ASCII_NEWLINE) currentString = "" + nextChar;
            else currentString = "";
        }
        //System.out.println("Mode: " + mode + "/" + (newToken != null ? newToken : "null") + "/" + justDumpComment + "/" + currentString + "/" + currentString.length());
        return newToken;
    }
}