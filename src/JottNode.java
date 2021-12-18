import exceptions.SemanticErrorException;

import java.util.ArrayList;

public class JottNode implements JottTree{
    Token tok;
    JottNode right;
    JottNode left;
    JottNode root;
    Integer depth;
    Integer type;
    Integer returning;
    ArrayList<ArrayList<String>> vars;
    Integer currentfunc;
    ArrayList<ArrayList<String>> funcs;

    JottNode(JottNode root, JottNode left, JottNode right, Integer depth){
        this.right = right;
        this.left = left;
        this.depth = depth;
        this.type = 0;
        this.returning = 0;
        this.root = root;
        this.tok = null;

    }

    JottNode(JottNode node)
    {
        this.tok = node.tok;
        this.right = node.right;
        this.left = node.left;
        this.depth = node.depth;
        this.type = node.type;
        this.returning = node.returning;
        this.root = node.root;
    }

    public boolean isNull(){
        if (this.left == null || this.left.isNull()){
            if (this.right == null || this.right.isNull()){
                return true;
            }
        }
        return false;
    }

    public Integer getDepth()
    {
        return this.depth;
    }

    public Token getTok()
    {
        return this.tok;
    }

    public JottNode getLeft()
    {
        return this.left;
    }

    public JottNode getRight()
    {
        return this.right;
    }

    public Integer getType()
    {
        return this.type;
    }

    public String getFile(JottNode n){
        if (n.getClass() == new JottNodeBase(null,null, 0).getClass()){
            return ((JottNodeBase) n).tok.getFilename();
        }
        String filename = "";
        if (n.left == null){
            if (n.right == null){
                return null;
            }
            else{
                filename = getFile(n.right);
                if (filename != null){
                    return filename;
                }
            }
        }
        else{
            filename = getFile(n.left);
            if (filename != null){
                return filename;
            }
            if (n.right == null){
                return null;
            }
            else{
                return getFile(n.right);
            }
        }
        return null;
    }

    /**
     * Will output a string of this tree in Jott
     *
     * @return a string representing the Jott code of this tree
     */
    public String convertToJott() {
        String output = "";
        if (this.left != null) {
            output += this.left.convertToJott();
        }
        if (this.right != null) {
            output += this.right.convertToJott();
        }
        if (this.depth == 1){
            output += ""; //output = "outer{\n" + output.indent(4) + "\n}" ;
        }
        return output;
    }

    /**
     * Will output a string of this tree in Java
     *
     * @return a string representing the Java code of this tree
     */
    public String convertToJava() {
        String output = "";
        if (this.left != null) {
            output += this.left.convertToJava();
        }
        if (this.right != null) {
            output += this.right.convertToJava();
        }
        if (this.depth == 1){
            output = "public class Output" + "{\n" + output.indent(4) + "}";// Output will be replaced later
        }
        return output;
    }

    /**
     * Will output a string of this tree in C
     *
     * @return a string representing the C code of this tree
     */
    public String convertToC() {
        String output = "";
        if (this.left != null) {
            output += this.left.convertToC();
        }
        if (this.right != null) {
            output += this.right.convertToC();
        }
        if (this.depth == 1){
            output = "#include <stdio.h>\n#include <string.h>\n#include <stdlib.h>\n\n" + output; //output = "outer{\n" + output.indent(4) + "\n}" ;
        }
        return output;
    }

    /**
     * Will output a string of this tree in Python
     *
     * @return a string representing the Python code of this tree
     */
    public String convertToPython() {
        String output = "";
        if (this.left != null) {
            output += this.left.convertToPython();
        }
        if (this.right != null) {
            output += this.right.convertToPython();
        }
        if (this.depth == 1){
            output = "import sys\n\n" + output + "\nsys.exit(main())"; //output = "outer{\n" + output.indent(4) + "\n}" ;
        }
        return output;
    }

    /**
     * This will validate that the tree follows the semantic rules of Jott
     * Errors validating will be reported to System.err
     *
     * @return true if valid Jott code; false otherwise
     */
    public boolean validateTree() {

        //find & verify main
        if(this.depth == 1)
        {
            //create function table and define built in functions
            ArrayList<ArrayList<String>> functionList = new ArrayList();
            ArrayList<String> printCall = new ArrayList<>();
            printCall.add("print"); //name
            printCall.add("Void"); //return
            printCall.add("?"); //? can be any param type
            ArrayList<String> inputCall = new ArrayList<>();
            inputCall.add("input");
            inputCall.add("String");
            inputCall.add("String");
            inputCall.add("Integer");
            ArrayList<String> concatCall = new ArrayList<>();
            concatCall.add("concat");
            concatCall.add("String");
            concatCall.add("String");
            concatCall.add("String");
            ArrayList<String> lengthCall = new ArrayList<>();
            lengthCall.add("length");
            lengthCall.add("Integer");
            lengthCall.add("String");
            functionList.add(printCall);
            functionList.add(inputCall);
            functionList.add(concatCall);
            functionList.add(lengthCall);

            ArrayList<ArrayList<String>> varTable = new ArrayList(); //varible table for function
            if(!this.buildVarTable(varTable))
            {
//                semanticError("Keyword cannot be used as variable name", this.getTok());
                return false;
            }
            boolean hasMain = false;
            JottNode curr = this;
            JottNodeFunction mainNode = null;
            //iterate through the list of the programs functions
            while(curr.getRight() != null)
            {
                String funcName = curr.getLeft().getTok().getToken(); // function name
                Token rtype = ((JottNodeFunction) curr.getLeft()).getReturntype().getLeft().getLeft().getTok(); //function return type

                //check for main
                if(funcName.equals("main"))
                {
                    if(hasMain)
                    {
                        semanticError("Duplicate main definition", this.getLeft().getTok());
                        return false;
                    }
                    else
                    {
                        hasMain = true;
                        mainNode = (JottNodeFunction) curr.getLeft();
                    }
                }
                else //if not main add to list and search function for function calls
                {
                    //add curr function to list
                    ArrayList<String> func = new ArrayList<String>();
                    func.add(funcName); //add name
                    func.add(rtype.getToken()); //add return type
                    if(curr.getLeft().getLeft() != null) //has params
                    {
                        JottNode searchParams = new JottNode(curr).getLeft().getLeft();
                        while(searchParams.getRight() != null)
                        {
                            //add param types to func
                            func.add(searchParams.getLeft().getLeft().getRight().getTok().getToken());
                            searchParams = searchParams.getRight();
                        }
                    }
                    functionList.add(func);
                }

                //search curr function body for calls, ifs, and whiles
                if(curr.getLeft().getRight() != null) //has body
                {
                    ArrayList<String> returnList = new ArrayList<>();
                    //call on function node
                    if (!curr.getLeft().validateIfsAndWhiles(functionList, varTable, funcName)) {
                        return false;
                    }

                    if (!curr.getLeft().getRight().functionCallSearch(functionList, varTable, funcName)) //search body for func calls
                    {
                        return false;
                    }

                    if (!curr.getLeft().getRight().checkOpNodes(functionList, varTable, funcName)) {
                        return false;
                    }

                    JottNode returnExp = new JottNode(curr).getLeft().findReturn(); // function return line
                    if (returnExp != null) {
                        if(!validateReturn(rtype, returnExp, functionList, varTable, funcName))
                        {
                            return false;
                        }
                        returnList.add("body");
                    }
                    curr.checkIfElseReturns(rtype, functionList, varTable, funcName, returnList);

                    if(!rtype.getToken().equals("Void"))
                    {
                        if(returnList.contains("if") && !returnList.contains("body") && !returnList.contains("else"))
                        {
                            semanticError("Missing return statement for non-void function", curr.getLeft().getTok());
                            return false;
                        }
                        else if(returnList.size() == 0 && !rtype.getToken().equals("Void"))
                        {
                            semanticError("Missing return statement for non-void function", curr.getLeft().getTok());
                            return false;
                        }
                        else if(returnList.contains("else") && !returnList.contains("body") && !returnList.contains("if"))
                        {
                            semanticError("Missing return statement for non-void function", curr.getLeft().getTok());
                            return false;
                        }
                    }
                    else{
                        if(returnList.size() > 0)
                        {
                            semanticError("Void function cannot return a value", curr.getLeft().getTok());
                            return false;
                        }
                    }
                    //System.out.println(returnList);
                }
                curr.root.funcs = functionList;
                curr.root.vars = varTable;
                curr = curr.getRight(); //move to next function in root function list
            }
            if(!hasMain)
            {
                semanticError("Missing main function", this.getLeft().getTok());
                return false;
            }
            //make sure main doesn't have params
            if(mainNode.getLeft() != null)
            {
                semanticError("Invalid main definition", mainNode.getTok());
                return false;
            }
            //check if Integer return type
            if(mainNode.getReturntype() != null)
            {
                if(!mainNode.getReturntype().getLeft().getLeft().getTok().getToken().equals("Integer"))
                {
                    semanticError("Invalid main definition", mainNode.getTok());
                    return false;
                }
            }
        }
        //if everything's valid
        if(this.left != null)
        {
            if(!this.left.validateTree())
            {
                return false;
            }
        }
        if(this.right != null)
        {
            if(!this.right.validateTree())
            {
                return false;
            }
        }
        return true;
    }

    public boolean checkOpNodes(ArrayList<ArrayList<String>> funcList, ArrayList<ArrayList<String>> varTable, String funcName)
    {
        JottNode curr = this;
        while(curr.getRight() != null) //iterate through lines of body
        {
            if(curr.getLeft().getType() == 2)
            {
                if(curr.getLeft().evaluateExprType(funcList, varTable, funcName) == null)
                {
                    semanticError("Invalid Operation", this.getLeft().getTok());
                    return false;
                }
            }
            curr = curr.getRight();
        }
        return true;
    }

    public void checkIfElseReturns(Token rtype, ArrayList<ArrayList<String>> funcList, ArrayList<ArrayList<String>> varTable, String funcName, ArrayList<String> returnList)
    {
        JottNode curr = this.getLeft().getRight(); //body
        while(curr.getRight() != null) {

            if (curr.getLeft().getType() == 1) {
                if (curr.getLeft().getTok().getToken().equals("if")) {
                    JottNode r = curr.getLeft().findReturn();
                    if(r != null)
                    {
                        validateReturn(rtype, r, funcList, varTable, funcName);
                        returnList.add("if");
                    }
                }
                else if(curr.getLeft().getTok().getToken().equals("elseif"))
                {
                    JottNode r = curr.getLeft().findReturn();
                    if(r != null)
                    {
                        validateReturn(rtype, r, funcList, varTable, funcName);
                        returnList.add("elseif");
                    }
                }
                else if(curr.getLeft().getTok().getToken().equals("else"))
                {
                    JottNode r = curr.getLeft().findReturn();
                    if(r != null)
                    {
                        validateReturn(rtype, r, funcList, varTable, funcName);
                        returnList.add("else");
                    }
                }
            }

            curr = curr.getRight();
        }
    }

    public boolean validateReturn(Token rtype, JottNode returnExp, ArrayList<ArrayList<String>> functionList, ArrayList<ArrayList<String>> varTable, String funcName)
    {
        //check return type
        //r type is the expected return type defined in the function header
        //expType is the type of the returned value
        if(funcName.equals("main"))
        {
            String expType = returnExp.evaluateExprType(functionList, varTable, funcName);
            if(!expType.equals("Integer"))
            {
                semanticError("Invalid main definition. Must return type <int>.", returnExp.getTok());
                return false;
            }
        }
        else if(!rtype.equals("Void"))
        {
            String expType = returnExp.evaluateExprType(functionList, varTable, funcName);

            if(expType == null)
            {
                semanticError("Invalid or missing return statement", returnExp.getTok());
                return false;
            }

            if(rtype.getToken().equals("Double"))
            {
                if(!expType.equals("Double"))
                {
                    semanticError("Invalid return type. Expected type <dbl>", returnExp.getTok());
                    return false;
                }
            }
            else if(rtype.getToken().equals("Integer"))
            {
                if(!expType.equals("Integer"))
                {
                    semanticError("Invalid return type. Expected type <int>", returnExp.getTok());
                    return false;
                }
            }
            else if(rtype.getToken().equals("String"))
            {
                if(!expType.equals("String"))
                {
                    semanticError("Invalid return type. Expected type <str>", returnExp.getTok());
                    return false;
                }
            }
            else if(rtype.getToken().equals("Boolean"))
            {
                if(!expType.equals("Boolean")) {
                    semanticError("Invalid return type. Expected type <bool>", returnExp.getTok());
                    return false;
                }
            }
        }
        return true;
    }

    public boolean validateIfsAndWhiles(ArrayList<ArrayList<String>> funcList, ArrayList<ArrayList<String>> varTable, String funcName)
    {
        JottNode curr = this.getRight(); //body
        while(curr.getRight() != null) {
            if (curr.getLeft().getType() == 1) {
                if (curr.getLeft().getTok().getToken().equals("if")) {

                    //check condition
                    JottNode cond = curr.getLeft().getLeft().getLeft().getLeft();
                    if (cond.evaluateExprType(funcList, varTable, funcName) == null ||
                            !cond.evaluateExprType(funcList, varTable, funcName).equals("Boolean")) {
                        semanticError("Invalid if condition", curr.getLeft().getTok());
                        return false;
                    }

                }
                else if (curr.getLeft().getTok().getToken().equals("while")) {
                    JottNode cond = curr.getLeft().getLeft().getLeft().getLeft();
                    if (cond.evaluateExprType(funcList, varTable, funcName) == null ||
                            !cond.evaluateExprType(funcList, varTable, funcName).equals("Boolean")) {
                        semanticError("Invalid while condition", curr.getLeft().getTok());
                        return false;
                    }
                }
            }
            curr = curr.getRight();
        }
        return true;
    }

    /**\
     * Determines the type of an expression
     * @return type of the expression
     */
    public String evaluateExprType(ArrayList<ArrayList<String>> funcList, ArrayList<ArrayList<String>> varTable, String funcName)
    {
        if(this.getType() == 3) // base case base node
        {
            Token t = this.getTok();
            TokenType tType = t.getTokenType();

            if(t.getTokenType() == TokenType.NUMBER && t.getToken().contains(".")) //not num or is int
            {
                return "Double";
            }
            else if(t.getTokenType() == TokenType.NUMBER && !t.getToken().contains(".")) //not num or is dub
            {
                return "Integer";
            }
            else if(t.getTokenType() == TokenType.STRING)
            {
                return "String";
            }
            else if(t.getToken().equals("True") || t.getToken().equals("False"))
            {
                return "Boolean";
            }
            else //variable
            {
                return getVariableType(varTable, funcName, t.getToken());
            }
        }
        else if(this.getType() == 1) //base case function
        {
            if(this.getRight() == null)
            {
                int funcFoundAt = getFuncFromList(funcList);
                return funcList.get(funcFoundAt).get(1);
            }
        }
        else if(this.getType() == 2)
        {
            if(this.getTok() == null)
            {
                return this.getLeft().getTok().getToken();
            }
            else if(this.getTok().getToken().equals("return"))
            {
                return this.getRight().evaluateExprType(funcList, varTable, funcName);
            }
            else if(this.getTok().getTokenType() == TokenType.MATH_OP)
            {
                String left = this.getLeft().evaluateExprType(funcList, varTable, funcName);
                String right = this.getRight().evaluateExprType(funcList, varTable, funcName);
                if(!left.equals(right))
                {
                    return null;
                }
                else if(!left.equals("Integer") && !left.equals("Double"))
                {
                    return null;
                }
                else
                {
                    return left;
                }
            }
            else if(this.getTok().getTokenType() == TokenType.REL_OP)
            {
                String left = this.getLeft().evaluateExprType(funcList, varTable, funcName);
                String right = this.getRight().evaluateExprType(funcList, varTable, funcName);
                if(!left.equals(right))
                {
                    return null;
                }
                else if(!left.equals("Integer") && !left.equals("Double"))
                {
                    return null;
                }
                else
                {
                    return "Boolean";
                }
            }
            else if(this.getTok().getTokenType() == TokenType.ASSIGN)
            {
                String left = this.getLeft().evaluateExprType(funcList, varTable, funcName);
                String right = this.getRight().evaluateExprType(funcList, varTable, funcName);

                if(!left.equals(right))
                {
                    return null;
                }
                else
                {
                    return left;
                }
            }
        }
        else
        {
            return this.getLeft().evaluateExprType(funcList, varTable, funcName);
        }
        return null;
    }

    public String getVariableType(ArrayList<ArrayList<String>> varTable, String funcName, String varName)
    {
        int functionVariables = -1;
        for(int i = 0; i < varTable.size(); i++)
        {
            if(varTable.get(i).get(0).equals(funcName))
            {
                functionVariables = i;
            }
        }

        if(functionVariables != -1)
        {
            for(int i = 1; i < varTable.get(functionVariables).size(); i = i + 2)
            {
                if(varTable.get(functionVariables).get(i).equals(varName))
                {
                    return varTable.get(functionVariables).get(i + 1);
                }
            }
        }
        return null;
    }

    /**
     * Iterates through lines of a function body to see if a return statement exists
     * @return
     */
    public JottNode findReturn()
    {
        JottNode curr = this.getRight();
        while(curr.getRight() != null)
        {
            if(curr.getLeft().getTok().getToken().equals("return"))
            {
                return curr.getLeft();
            }
            curr = curr.getRight();
        }
        return null;
    }

    /**
     * Gets function from function list
     * @param funcList
     * @return
     */
    public int getFuncFromList(ArrayList<ArrayList<String>> funcList)
    {
        int funcFoundAt = -1;
        for(int i = 0; i < funcList.size(); i++)
        {
            if(this.getTok().getToken().equals(funcList.get(i).get(0)))
            {
                funcFoundAt = i;
            }
        }
        return funcFoundAt;
    }

    /**
     * Used to search the body of a function (function.getRight()) for a function call and check if its
     * parameters are valid.
     * @param funcList Table of defined functions
     * @return False if an invalid call is found. T rue otherwise.
     */
    public boolean functionCallSearch(ArrayList<ArrayList<String>> funcList, ArrayList<ArrayList<String>> varTable, String funcName)
    {
        if(this.getType() == 1 && this.getRight() == null) //found a function call
        {
            //check if call is defined
            int funcFoundAt = getFuncFromList(funcList);

            if(funcFoundAt == -1)
            {
                semanticError("Function not defined", this.getTok());
                return false;
            }


            JottNode params = this.getLeft();
            if(params != null)
            {
                int i = 2;
                while(params.getRight() != null)
                {
                    Token t = params.getLeft().getLeft().getTok();
                    String type;
                    if(t == null)
                    {
                        type = params.getLeft().getLeft().getLeft().evaluateExprType(funcList, varTable, funcName);
                    }
                    else {
                        TokenType tokenType = t.getTokenType();
                        if(tokenType == TokenType.NUMBER && !t.getToken().contains("."))
                        {
                            type = "Integer";
                        }
                        else if(tokenType == TokenType.NUMBER && t.getToken().contains("."))
                        {
                            type = "Double";
                        }
                        else if(tokenType == TokenType.STRING)
                        {
                            type = "String";
                        }
                        else if(t.getToken().equals("False") || t.getToken().equals("True"))
                        {
                            type = "Boolean";
                        }
                        else if(tokenType == TokenType.ID_KEYWORD)
                        {
//                        type = getVariableType(varTable, funcList.get(funcFoundAt).get(0), t.getToken());
                            type = getVariableType(varTable, funcName, t.getToken());
                            if(type == null ||
                                    t.getToken().equals("while") ||
                                    t.getToken().equals("if") ||
                                    t.getToken().equals("else") ||
                                    t.getToken().equals("elseif") ||
                                    t.getToken().equals("elif"))
                            {
                                semanticError("Invalid function param", this.getTok());
                                return false;
                            }
                        }
                        else
                        {
                            type = "?";
//                        semanticError("Invalid type being passed into function param", this.getTok());
//                        return false;
                        }
                    }

                    if((i - 1) > funcList.get(funcFoundAt).size() - 1)
                    {
                        semanticError("Invalid function param count", this.getTok());
                        return false;
                    }
                    else if(!funcList.get(funcFoundAt).get(i).equals(type) && !funcList.get(funcFoundAt).get(i).equals("?"))
                    {
                        semanticError("Invalid function param", this.getTok());
                        return false;
                    }
                    i++;
                    params = params.getRight();
                }
                if((i - 1) < funcList.get(funcFoundAt).size() - 1)
                {
                    semanticError("Invalid function param count", this.getTok());
                    return false;
                }
            }
            else
            {
                if(!funcList.get(funcFoundAt).get(1).equals("?") && funcList.get(funcFoundAt).size() != 2)
                {
                    semanticError("Invalid function param count", this.getTok());
                    return false;
                }
            }
        }
        if(this.left != null)
        {
            if(!this.left.functionCallSearch(funcList, varTable, funcName))
            {return false;}
        }
        if(this.right != null)
        {
            if(!this.right.functionCallSearch(funcList, varTable, funcName))
            {return false;}
        }
        return true;
    }

    public boolean buildVarTable(ArrayList<ArrayList<String>> varTable) {
        if(this.getLeft().getRight() != null)
        {
            JottNode curr = this; //root

            while (curr.getRight() != null) {
                JottNode funcLine = curr.getLeft().getRight();
                ArrayList<String> funcVars = new ArrayList<String>();
                funcVars.add(curr.getLeft().getTok().getToken());

                if(curr.getLeft().getLeft() != null)
                {
                    JottNode params = curr.getLeft().getLeft();
                    while(params.getRight() != null)
                    {
                        String var = params.getLeft().getLeft().getLeft().getTok().getToken();
                        if(var.equals("while") ||
                                var.equals("if") ||
                                var.equals("else") ||
                                var.equals("elseif") ||
                                var.equals("elif"))
                        {
                            semanticError("Keyword cannot be used as param name", params.getLeft().getLeft().getLeft().getTok());
                            return false;
                        }
                        funcVars.add(var);
                        funcVars.add(params.getLeft().getLeft().getRight().getTok().getToken());
                        params = params.getRight();
                    }
                }

                while(funcLine.getRight() != null)
                {
                    //add variables defined inside function
                    if(funcLine.getLeft().getTok().getTokenType() == TokenType.ASSIGN)
                    {
                        String var = funcLine.getLeft().getLeft().getRight().getTok().getToken();
                        if(var.equals("while") ||
                                var.equals("if") ||
                                var.equals("else") ||
                                var.equals("elseif") ||
                                var.equals("elif"))
                        {
                            semanticError("Keyword cannot be used as variable name", funcLine.getLeft().getLeft().getRight().getTok());
                            return false;
                        }
                        funcVars.add(var);
                        funcVars.add(funcLine.getLeft().getLeft().getLeft().getTok().getToken());
                    }
                    funcLine = funcLine.getRight();
                }
                varTable.add(funcVars);
                curr = curr.getRight();
            }
        }
        return true;
    }

    public void semanticError(String message, Token tok)
    {
        System.err.println("Semantic Error:");
        System.err.println(message);
        System.err.println(tok.getFilename() + ":" + tok.getLineNum());
    }
}
