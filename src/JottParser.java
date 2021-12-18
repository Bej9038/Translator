/*
  This class is responsible for paring Jott Tokens
  into a Jott parse tree.

  @author
*/

import exceptions.SyntaxErrorException;

import java.util.ArrayList;

public class JottParser {
    /**
     * Parses an ArrayList of Jotton tokens into a Jott Parse Tree.
     *
     * @param tokens the ArrayList of Jott tokens to parse
     * @return the root of the Jott Parse Tree represented by the tokens.
     * or null upon an error in parsing.
     */
    public static JottNode realroot;

    public static JottTree parse(ArrayList<Token> tokens){
        try{
            JottTree parsed = parsehelper(null, tokens, 0, false, false, false, false, false);
            return parsed;
        }
        catch (SyntaxErrorException see){
            System.err.println(see.getMessage());
            System.exit(1);
            return null;
        }
    }

    public static boolean isType(Token tok){
        return tok.getToken().equals("Integer") || tok.getToken().equals("Double") || tok.getToken().equals("String") || tok.getToken().equals("Boolean") || tok.getToken().equals("Void");
    }


    public static boolean startsWithDigit(Token tok){
        return Character.isDigit(tok.getToken().charAt(0));
    }

    public static void checkValidName(Token tok) throws SyntaxErrorException{
        if (isType(tok)){
            throw new SyntaxErrorException("Expected <id> got <type>\n" + tok.getFilename() + ":" + tok.getLineNum());
        }
        if (startsWithDigit(tok)){
            throw new SyntaxErrorException("Expected <id> got <Integer>\n" + tok.getFilename() + ":" + tok.getLineNum());
        }
        if (Character.isUpperCase(tok.getToken().charAt(0))){
            if (tok.getToken().equals("True") || tok.getToken().equals("False"))
            {
                //do nothing
            }
            else
            {
                throw new SyntaxErrorException("Expected <id> should start with lowercase letter\n" + tok.getFilename() + ":" + tok.getLineNum());
            }
        }
        if (tok.getToken().equals("while") ||
                tok.getToken().equals("if") ||
                tok.getToken().equals("elseif") ||
                tok.getToken().equals("else") ||
                tok.getToken().equals("print") ||
                tok.getToken().equals("main") ||
                tok.getToken().equals("input") ||
                tok.getToken().equals("concat") ||
                tok.getToken().equals("length")
        ){
            //throw new SyntaxErrorException("Keyword '" + tok.getToken() + "' reserved by system\n" + tok.getFilename() + ":" + tok.getLineNum());
        }
    }

    public static JottTree parsehelper(JottNode rootnode, ArrayList<Token> tokens, int depth, boolean infunction, boolean inIf, boolean inEquals, boolean inPrint, boolean inParams) throws SyntaxErrorException {
        int curDepth = depth + 1;
        try{
            //Start of any "line"(command) is an ID_KEYWORD
            //Otherwise it must be an inner body
            //Commands end in ; or }(if a definition)
            if (tokens == null || tokens.size() == 0){ //base case
                return null;
            }
            JottNode root = new JottNode(null, null, null, curDepth);
            JottNode cur = root;
            if (realroot == null)
            {
                realroot = root;
            }
            root.root = realroot; //all nodes will be able to point to the root.
            if (tokens.size() == 1){
                if(tokens.get(0).getTokenType() == TokenType.ID_KEYWORD) {
                    checkValidName(tokens.get(0));
                }
                return new JottNodeBase(realroot, tokens.get(0), curDepth + 1); //base case
            }
            int i = 0;
            while (i < tokens.size()) {
                Token tok = tokens.get(i);
                switch (tok.getTokenType()){
                    case ID_KEYWORD:    //Start of a command
                        if (tokens.get(i + 1).getTokenType() == null){
                            try{
                                if(!tokens.get(0).getToken().equals("True") && !tokens.get(0).getToken().equals("False"))
                                checkValidName(tok);
                            }
                            catch (SyntaxErrorException see){
                                throw new SyntaxErrorException("Function '"+ tok.getToken() + "' requires arguments\n" + tok.getFilename() + ":" + tok.getLineNum());
                            }
                            return new JottNodeBase(realroot, tokens.get(i), curDepth + 1);
                        }
                        if (tok.getToken().equals("return")){ //special return stuff, need to add function check
                            if (inPrint){
                                throw new SyntaxErrorException("Cannot return inside of print\n" + tok.getFilename() + ":" + tok.getLineNum());
                            }
                            int j = i + 2;
                            while (j < tokens.size() && tokens.get(j).getTokenType() != TokenType.SEMICOLON){
                                j++;
                            }
                            if (j == tokens.size()){
                                throw new SyntaxErrorException("Expected ;\n" + tok.getFilename() + ":" + tok.getLineNum());
                            }
                            if (j != tokens.size() - 1){ //so if return is between {} (including if/else) it should be the last thing to occur. This checks that that is the case.
                                throw new SyntaxErrorException("Expected }\n" + tok.getFilename() + ":" + tok.getLineNum());
                            }
                            ArrayList<Token> sub = new ArrayList<> (tokens.subList(i + 1, j));
                            tokens.subList(i, j + 1).clear();
                            cur.left = new JottNodeOp(realroot, tok, null, (JottNode) parsehelper(realroot, sub, curDepth + 1, infunction, inIf, true, inPrint, inParams), curDepth + 1);
                            cur.right = new JottNode(realroot, null, null, curDepth + 1);
                            cur = cur.right;
                            break;
                        }
                        if (tok.getToken().equals("elseif")){
                            throw new SyntaxErrorException("<elseif> without an <if>\n" + tok.getFilename() + ":" + tok.getLineNum());
                        }
                        if (tok.getToken().equals("else")){
                            throw new SyntaxErrorException("<else> without an <if>\n" + tok.getFilename() + ":" + tok.getLineNum());
                        }
                        switch (tokens.get(i + 1).getTokenType()){
                            case L_BRACKET: //Is a function call or definition (or if statement)
                                while (true) {
                                    tok = tokens.get(i);
                                    int brackets = 1;
                                    int j = i + 1;
                                    ArrayList<Token> subTok = null;
                                    JottNode subParams = null;
                                    if (!tokens.get(i).getToken().equals("else")){
                                        while (j < tokens.size() && brackets != 0) {
                                            j++;
                                            if (j == tokens.size()){
                                                break;
                                            }
                                            if (tokens.get(j).getTokenType() == TokenType.R_BRACKET){
                                                brackets --;
                                            }
                                            if (tokens.get(j).getTokenType() == TokenType.L_BRACKET){
                                                brackets++;
                                            }
                                        }
                                        if (j == tokens.size()) {
                                            throw new SyntaxErrorException("Expected ]\n" + tok.getFilename() + ":" + tok.getLineNum());
                                        }
                                        if (j > i + 2) {
                                            subTok = new ArrayList<>(tokens.subList(i + 2, j));
                                            int k = 0;
                                            subParams = new JottNode(realroot, null, null, curDepth + 1);
                                            JottNode curParam = subParams;
                                            while (k < subTok.size()){
                                                int l = k + 1;
                                                brackets = 0;
                                                while (true){
                                                    if (!(l < subTok.size())){
                                                        break;
                                                    }
                                                    if (subTok.get(l).getTokenType() == TokenType.L_BRACKET){
                                                        brackets++;
                                                    }
                                                    if (subTok.get(l).getTokenType() == TokenType.R_BRACKET){
                                                        brackets--;
                                                    }
                                                    if (subTok.get(l).getTokenType() == TokenType.COMMA){
                                                        if (brackets == 0){
                                                            break;
                                                        }
                                                    }
                                                    l++;
                                                }
                                                ArrayList<Token> tempSubList = new ArrayList<> (subTok.subList(k, l));
                                                tempSubList.add(new Token("", tok.getFilename(), tok.getLineNum(), null)); //This allows recursion without errors.
                                                if (l == subTok.size()){ //Meaning it's the last item
                                                    subTok.clear();
                                                    if(tok.getToken().equals("if") || tok.getToken().equals("elseif") || tok.getToken().equals("while")){
                                                        if(inPrint){
                                                            throw new SyntaxErrorException("Cannot call if statement inside of print\n" + tok.getFilename() + ":" + tok.getLineNum());
                                                        }
                                                        curParam.left = new JottNode(realroot, (JottNode) parsehelper(realroot, tempSubList, curDepth + 1, infunction, true, inEquals, inPrint, inParams), null, curDepth + 1);
                                                    }
                                                    else if(tok.getToken().equals("print")){
                                                        curParam.left = new JottNode(realroot, (JottNode) parsehelper(realroot, tempSubList, curDepth + 1, infunction, true, inEquals, true, true), null, curDepth + 1);
                                                    }
                                                    else{
                                                        curParam.left = new JottNode(realroot, (JottNode) parsehelper(realroot, tempSubList, curDepth + 1, infunction, inIf, inEquals, inPrint, true), null, curDepth + 1);
                                                    }
                                                }
                                                else{
                                                    if(tok.getToken().equals("if") || tok.getToken().equals("elseif") || tok.getToken().equals("while")){
                                                        if(inPrint){
                                                            throw new SyntaxErrorException("Cannot call if statement inside of print\n" + tok.getFilename() + ":" + tok.getLineNum());
                                                        }
                                                        subTok.subList(k, l + 1).clear();
                                                        curParam.left = new JottNode(realroot, (JottNode) parsehelper(realroot, tempSubList, curDepth + 1, infunction, true, inEquals, inPrint, inParams), new JottNodeBase(realroot, new Token(", ", tok.getFilename(), tok.getLineNum(), TokenType.COMMA), curDepth + 1), curDepth + 1);
                                                    }
                                                    else if(tok.getToken().equals("print")){
                                                        curParam.left = new JottNode(realroot, (JottNode) parsehelper(realroot, tempSubList, curDepth + 1, infunction, true, inEquals, true, true), null, curDepth + 1);
                                                    }
                                                    else{
                                                        subTok.subList(k, l + 1).clear();
                                                        curParam.left = new JottNode(realroot, (JottNode) parsehelper(realroot, tempSubList, curDepth + 1, infunction, inIf, inEquals, inPrint, true), new JottNodeBase(realroot, new Token(", ", tok.getFilename(), tok.getLineNum(), TokenType.COMMA), curDepth + 1), curDepth + 1);
                                                    }
                                                }
                                                curParam.right = new JottNode(realroot, null, null, curDepth + 1);
                                                curParam = curParam.right;
                                            }
                                        }
                                        tokens.subList(i + 1, j + 1).clear(); //Remove [...]
                                        if (tokens.size() == 1){
                                            if (inParams || inEquals || inIf || inPrint){
                                                tokens.add(new Token("-", tok.getFilename(), tok.getLineNum(), TokenType.SEMICOLON));
                                            }
                                            else{
                                                throw new SyntaxErrorException("Expected ;\n" + tok.getFilename() + ":" + tok.getLineNum());
                                            }
                                        }
                                    }
                                    ArrayList<Token> returntype = null;
                                    JottNode subReturn = null;
                                    if (tokens.get(i + 1).getTokenType() == TokenType.COLON){ //is declaration
                                        tokens.remove(i + 1);
                                        j = i + 1;
                                        while (j < tokens.size() && tokens.get(j).getTokenType() != TokenType.L_BRACE){
                                            j++;
                                        }
                                        if (j == tokens.size()) {
                                            throw new SyntaxErrorException("Expected { got <type>\n" + tok.getFilename() + ":" + tok.getLineNum());
                                        }
                                        returntype = new ArrayList<>(tokens.subList(i + 1, j));
                                        tokens.subList(i + 1, j).clear();
                                        int k = 0;
                                        subReturn = new JottNode(realroot, null, null, curDepth + 1);
                                        JottNode curReturn = subReturn;
                                        while (k < returntype.size()){
                                            int l = k + 1;
                                            while (l < returntype.size() && returntype.get(l).getTokenType() != TokenType.COMMA){
                                                l++;
                                            }
                                            ArrayList<Token> tempRetList = new ArrayList<> (returntype.subList(k, l));
                                            if (l == returntype.size()){
                                                returntype.clear();
                                                curReturn.left = new JottNode(realroot, new JottNodeBase(realroot, tempRetList.get(0), curDepth + 1), null, curDepth + 1);

                                            }
                                            else {
                                                returntype.subList(k, l + 1).clear();
                                                curReturn.left = new JottNode(realroot, new JottNodeBase(realroot, tempRetList.get(0), curDepth + 1), new JottNodeBase(realroot, new Token(", ", tok.getFilename(), tok.getLineNum(), TokenType.COMMA), curDepth + 1), curDepth + 1);

                                            }
                                            if (tempRetList.size() != 1){
                                                throw new SyntaxErrorException("Invalid function return\n" + tok.getFilename() + ":" + tok.getLineNum());
                                            }
                                            curReturn.right = new JottNode(realroot, null, null, curDepth + 1);
                                            curReturn = curReturn.right;
                                        }
                                    }
                                    else{
                                        if(!infunction){
                                            throw new SyntaxErrorException("Statement outside of function\n" + tok.getFilename() + ":" + tok.getLineNum());
                                        }
                                        if (tokens.get(i + 1).getTokenType() == TokenType.SEMICOLON) { // Is a function call
                                            int typey = 2;
                                            if (tokens.get(i + 1).getToken().equals("-")) {
                                                typey = 1; //consider part of params, can only happen if the above occurs.
                                            }
                                            tokens.remove(i);
                                            tokens.remove(i);
                                            cur.left = new JottNodeFunction(realroot, tok, subParams, null, null, typey, curDepth + 1);
                                            cur.right = new JottNode(realroot, null, null, curDepth + 1);
                                            cur = cur.right;
                                            break;
                                        }
                                        if (tokens.get(i + 1).getTokenType() == null) { // [func[], func[]]
                                            tokens.remove(i);
                                            tokens.remove(i);
                                            cur.left = new JottNodeFunction(realroot, tok, subParams, null, null, 1, curDepth + 1);
                                            cur.right = new JottNode(realroot, null, null, curDepth + 1);
                                            cur = cur.right;
                                            break;
                                        }
                                        if (tokens.get(tokens.size() - 1).getTokenType() == null) { //Inside of an if statement
                                            if (tokens.get(i + 1).getTokenType() == TokenType.REL_OP) { //Can't remember why this works
                                                JottNode relly = new JottNodeFunction(realroot, tok, subParams, null, null, 1, curDepth + 1);
                                                JottNode rellllly = (JottNode) parsehelper(realroot, new ArrayList<>(tokens.subList(i + 2, tokens.size())), curDepth + 1, infunction, inIf, inEquals, inPrint, inParams);
                                                return new JottNodeOp(realroot, tokens.get(i + 1), relly, rellllly, curDepth + 1);
                                            }
                                        }
                                    }
                                    if (tokens.get(i + 1).getTokenType() == TokenType.L_BRACE){
                                        int braces = 1;
                                        j = i + 1;
                                        ArrayList<Token> tokBody = null;
                                        while (j < tokens.size() && braces != 0) {
                                            j++;
                                            if (j == tokens.size()){
                                                break;
                                            }
                                            if (tokens.get(j).getTokenType() == TokenType.R_BRACE){
                                                braces --;
                                            }
                                            if(tokens.get(j).getTokenType() == TokenType.L_BRACE){
                                                braces ++;
                                            }
                                        }
                                        if (j == tokens.size()) {
                                            throw new SyntaxErrorException("<func_def> missing closing }\n" + tok.getFilename() + ":" + tok.getLineNum());
                                        }
                                        if (j != i + 2) {
                                            tokBody = new ArrayList<>(tokens.subList(i + 2, j));
                                        }
                                        tokens.subList(i + 1, j + 1).clear(); //Remove {...}
                                        tokens.remove(i);
                                        if(inPrint){
                                            throw new SyntaxErrorException("Cannot define function inside of print\n" + tok.getFilename() + ":" + tok.getLineNum());
                                        }
                                        cur.left = new JottNodeFunction(realroot, tok, subParams, (JottNode) parsehelper(realroot, tokBody, curDepth + 1, true, inEquals, inIf, inPrint, inParams), subReturn, curDepth + 1);
                                        cur.right = new JottNode(realroot, null, null, curDepth + 1);
                                        cur = cur.right;
                                        if (tokens.size() == 0){
                                            break;
                                        }
                                        if (tok.getToken().equals("else") || tok.getToken().equals("while")){
                                            break;
                                        }
                                        if (tokens.get(i).getToken().equals("elseif") || tokens.get(i).getToken().equals("else")){
                                            continue;
                                        }
                                        else{
                                            break;
                                        }
                                    }
                                    else{ ///why
                                        if (inEquals)
                                        {
                                                if(tokens.get(i + 1).getTokenType() == TokenType.REL_OP || tokens.get(i + 1).getTokenType() == TokenType.MATH_OP)
                                                {
                                                    Token tok2 = tokens.get(i + 1);
                                                    tokens.remove(i);tokens.remove(i);
                                                    JottNode tempRight2 = (JottNode) parsehelper(realroot, tokens, curDepth + 1, infunction, inIf, inEquals, inPrint, inParams);
                                                    if (tempRight2 == null){
                                                        throw new SyntaxErrorException("Operation missing right operand\n" + tok.getFilename() + ":" + tok.getLineNum());
                                                    }
                                                    if (root.left == null && root.right == null){//This case must be a subnode and cannot be root.
                                                        root = new JottNodeOp(realroot, tok2, new JottNodeFunction(realroot, tok, subParams, null, null, 1, curDepth + 1), tempRight2,curDepth + 1);
                                                        tokens.clear();
                                                        return root;
                                                    }
                                                }
                                        }
                                        else
                                        {
                                            throw new SyntaxErrorException("Expected { got <type>\n" + tok.getFilename() + ":" + tok.getLineNum());
                                        }
                                    }
                                    //}
                                    //throw new SyntaxErrorException("Expected ;\n" + tok.getFilename() + ":" + tok.getLineNum());
                                }
                                continue;
                            case ID_KEYWORD: //(int i)
                                checkValidName(tokens.get(i + 1));
                                if (tokens.get(i + 2).getTokenType() == TokenType.SEMICOLON) { // Is just an initialize
                                    cur.left = new JottNodeOp(realroot, tokens.get(i + 2), new JottNodeBase(realroot, tok, curDepth + 1), new JottNodeBase(realroot, tokens.get(i + 1), curDepth + 1), curDepth + 1);
                                    cur.right = new JottNode(realroot, null, null, curDepth + 1);
                                    cur = cur.right;
                                    tokens.remove(i);tokens.remove(i);tokens.remove(i);
                                }
                                else if (tokens.get(i + 2).getTokenType() == TokenType.ASSIGN) { //int i =
                                    int j = i + 3;
                                    while (j < tokens.size() && tokens.get(j).getTokenType() != TokenType.SEMICOLON){
                                        j++;
                                    }
                                    if (j == tokens.size()){
                                        throw new SyntaxErrorException("Assignment missing right operand\n" + tok.getFilename() + ":" + tok.getLineNum());
                                    }
                                    ArrayList<Token> tempRightList = new ArrayList<> (tokens.subList(i + 3, j));
                                    tokens.subList(i + 3, j + 1).clear();
                                    JottNode tempRight = (JottNode) parsehelper(realroot, tempRightList, curDepth + 1, infunction, inIf, true, inPrint, inParams);
                                    if (tempRight == null){
                                        throw new SyntaxErrorException("Expected <exp> got <end_stmt>\n" + tok.getFilename() + ":" + tok.getLineNum());
                                    }
                                    cur.left = new JottNodeOp( //...;
                                            //new Token(";", tok.getFilename(), tok.getLineNum(), TokenType.SEMICOLON),
                                            //new JottNodeOp(
                                            realroot,
                                            tokens.get(i + 2), // "left = right"
                                            new JottNodeOp(realroot, null, new JottNodeBase(realroot, tok, curDepth + 1), new JottNodeBase(realroot, tokens.get(i + 1), curDepth + 1), curDepth + 1), //"int i"
                                            tempRight, curDepth + 1); //"5"
                                    cur.right = new JottNode(realroot, null, null, curDepth + 1);
                                    cur = cur.right;
                                    tokens.remove(i);tokens.remove(i);tokens.remove(i);
                                }
                                else{
                                    throw new SyntaxErrorException("Expected ;\n" + tok.getFilename() + ":" + tok.getLineNum());
                                }
                                continue;
                            case ASSIGN: //i =
                                int j = i + 2;
                                while (j < tokens.size() && tokens.get(j).getTokenType() != TokenType.SEMICOLON){
                                    j++;
                                }
                                if (j == tokens.size()){
                                    throw new SyntaxErrorException("Assignment missing right operand\n" + tok.getFilename() + ":" + tok.getLineNum());
                                }
                                ArrayList<Token> tempRightList = new ArrayList<> (tokens.subList(i + 2, j));
                                tokens.subList(i + 2, j + 1).clear();
                                JottNode tempRight = (JottNode) parsehelper(realroot, tempRightList, curDepth + 1, infunction, inIf, true, inPrint, inParams);
                                if (tempRight == null){
                                    throw new SyntaxErrorException("Expected <exp> got <end_stmt>\n" + tok.getFilename() + ":" + tok.getLineNum());
                                }
                                cur.left = new JottNodeOp(realroot, tokens.get(i + 1), new JottNodeBase(realroot, tok, curDepth + 1), tempRight, curDepth + 1);
                                cur.right = new JottNode(realroot, null, null, curDepth + 1);
                                cur = cur.right;
                                tokens.remove(i);tokens.remove(i);
                                continue;
                            case NUMBER:
                                throw new SyntaxErrorException("Incomplete Statement\n" + tok.getFilename() + ":" + tok.getLineNum());
                                //^^These are all cases for "functions"
                                //vvThese are all cases for "parameters"
                            case REL_OP: //i <
                            case MATH_OP: //i *
                                if (tokens.get(i + 1).getTokenType() == TokenType.REL_OP){
                                    if (!inIf && !inEquals && !inPrint){
                                        throw new SyntaxErrorException("Expected <stmt> got <Expr>\n" + tok.getFilename() + ":" + tok.getLineNum());
                                    }
                                }
                                else{
                                    if(!inEquals && !inPrint){
                                        throw new SyntaxErrorException("Expected <stmt> got <Expr>\n" + tok.getFilename() + ":" + tok.getLineNum());
                                    }
                                }
                                Token tok2 = tokens.get(i + 1);
                                tokens.remove(i);tokens.remove(i);
                                JottNode tempRight2 = (JottNode) parsehelper(realroot, tokens, curDepth + 1, infunction, inIf, inEquals, inPrint, inParams);
                                if (tempRight2 == null){
                                    throw new SyntaxErrorException("Operation missing right operand\n" + tok.getFilename() + ":" + tok.getLineNum());
                                }
                                if (root.left == null && root.right == null){//This case must be a subnode and cannot be root.
                                    root = new JottNodeOp(realroot, tok2, new JottNodeBase(realroot, tok, curDepth + 1), tempRight2, curDepth + 1);
                                    tokens.clear();
                                    return root;
                                }
                                continue;
                            case SEMICOLON:
                                if (tokens.size() == 2){
                                    root = new JottNodeBase(realroot, tok, curDepth + 1);
                                    tokens.clear();
                                    return root;
                                }
                                else{
                                    throw new SyntaxErrorException("Invalid operation: " + tok.getToken() + "\n" + tok.getFilename() + ":" + tok.getLineNum());
                                }
                            case COLON: //this should only ever happen if you are inside of a [] in a function declaration.
                                if(tokens.size() == 4 && tokens.get(3).getTokenType() == null) {//Make sure that's the case
                                    try {
                                        checkValidName(tok);
                                    }
                                    catch (SyntaxErrorException see){
                                        throw see;
                                    }
                                    if (isType(tokens.get(2))){
                                        if(tokens.get(2).getToken().equals("Void")){
                                            throw new SyntaxErrorException("expected <type> got Void\n" + tok.getFilename() + ":" + tok.getLineNum());
                                        }
                                    }
                                    else{
                                        throw new SyntaxErrorException("Expected <type> got <id>\n" + tok.getFilename() + ":" + tok.getLineNum());
                                    }
                                    root = new JottNodeOp(realroot, tokens.get(i + 1), new JottNodeBase(realroot, tok, curDepth + 1), new JottNodeBase(realroot, tokens.get(i + 2), curDepth + 1), curDepth + 1);
                                    tokens.clear();
                                    return root;
                                }
                                else{
                                    throw new SyntaxErrorException("Expected <comma> or ]\n" + tok.getFilename() + ":" + tok.getLineNum());
                                }
                            default:
                                throw new SyntaxErrorException("Unknown/Incomplete Statement\n" + tok.getFilename() + ":" + tok.getLineNum());
                        }
                    case NUMBER: //Will only ever hit this inside of arguments
                        if (!infunction && !inPrint &&!inEquals){
                            throw new SyntaxErrorException("Expected <id> got <int>\n" + tok.getFilename() + ":" + tok.getLineNum());
                        }
                        if (tokens.get(i + 1).getTokenType() == null || tokens.get(i + 1).getTokenType() == TokenType.SEMICOLON) {
                            if (tokens.size() == 2) {
                                root = new JottNodeBase(realroot, tok, curDepth + 1);
                                tokens.clear();
                                return root;
                            } else {
                                throw new SyntaxErrorException("Invalid operation: " + tok.getToken() + "\n" + tok.getFilename() + ":" + tok.getLineNum());
                            }
                        }
                        switch (tokens.get(i + 1).getTokenType()){
                            case REL_OP: //5 <
                            case MATH_OP: //5 *
                                if (tokens.get(i + 1).getTokenType() == TokenType.REL_OP){
                                    if (!inIf && !inEquals && !inPrint){
                                        throw new SyntaxErrorException("Expected <stmt> got <Expr>\n" + tok.getFilename() + ":" + tok.getLineNum());
                                    }
                                }
                                else{
                                    if(!inEquals && !inPrint){
                                        throw new SyntaxErrorException("Expected <stmt> got <Expr>\n" + tok.getFilename() + ":" + tok.getLineNum());
                                    }
                                }
                                //Should only hit a relational op after = or in an if
                                //Should only hit a math op after =
                                //Add check to here and parameter to parsehelper to pass through
                                Token tok2 = tokens.get(i + 1);
                                tokens.remove(i);tokens.remove(i);
                                JottNode tempRight2 = (JottNode) parsehelper(realroot, tokens, curDepth + 1, infunction, inIf, inEquals, inPrint, inParams);
                                if (tempRight2 == null){
                                    throw new SyntaxErrorException("Operation missing right operand\n" + tok.getFilename() + ":" + tok.getLineNum());
                                }
                                if (root.left == null && root.right == null){//This case must be a subnode and cannot be root.
                                    root = new JottNodeOp(realroot, tok2, new JottNodeBase(realroot, tok, curDepth + 1), tempRight2, curDepth + 1);
                                    tokens.clear();
                                    return root;
                                }
                            case STRING:
                                throw new SyntaxErrorException("Unexpected token <string> after <int>\n" + tok.getFilename() + ":" + tok.getLineNum());
                            case ID_KEYWORD:
                                throw new SyntaxErrorException("Unexpected token <id_keyword> after <int>\n" + tok.getFilename() + ":" + tok.getLineNum());
                        }
                        continue;
                    case STRING:
                        if (tokens.get(i + 1).getTokenType() == null){
                            if (tokens.size() == 2){
                                root = new JottNodeBase(realroot, tok, curDepth + 1);
                                tokens.clear();
                                return root;
                            }
                            else{
                                throw new SyntaxErrorException("Invalid operation: " + tok.getToken() + "\n" + tok.getFilename() + ":" + tok.getLineNum());
                            }
                        }
                        else{
                            throw new SyntaxErrorException("Invalid operation: " + tok.getToken() + "\n" + tok.getFilename() + ":" + tok.getLineNum());
                        }
                    case SEMICOLON:
                        return null;
                    default:
                        if (tok.getTokenType() == TokenType.MATH_OP){
                            throw new SyntaxErrorException("<op> without leading <expr>\n" + tok.getFilename() + ":" + tok.getLineNum());
                        }

                }
            }
            if (!(root.isNull())){ //Basically if it didn't do anything.
                return null;
            }
            return root;
        }
        catch (SyntaxErrorException see){
            throw see;
        }
    }
}
