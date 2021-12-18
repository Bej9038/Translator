import exceptions.SemanticErrorException;
import exceptions.SyntaxErrorException;
import java.util.ArrayList;

public class JottNodeFunction extends JottNode {

    JottNode returntype;
    int param; //checks whether or not to add ;
    static int printStrcatCount = 0;


    JottNodeFunction(JottNode root, Token identifier, JottNode left, JottNode right, JottNode returntype, Integer depth) {
        //identifier = name
        //left = params
        //right = body; null means is a call and not a def
        //return type = return type; null means is a call and not a def; just this null means if statement
        super(root, left, right, depth);
        this.tok = identifier;
        this.returntype = returntype;
        this.param = 0;
        this.type = 1;
        this.returning = 0; //used for print functions
    }

    JottNodeFunction(JottNode root, Token identifier, JottNode left, JottNode right, JottNode returntype, int param, Integer depth) {
        //        //identifier = name
        //        //left = params
        //        //right = body; null means is a call and not a def
        //        //return type = return type; null means is a call and not a def; just this null means if statement
        super(root, left, right, depth);
        this.tok = identifier;
        this.returntype = returntype;
        this.param = param;
        this.type = 1;
    }

    public Token getTok() {
        return this.tok;
    }

    public JottNode getReturntype()
    {
        return this.returntype;
    }

    @Override
    public String convertToJott() {
        String output = this.tok.getToken();
        if (this.left == null) {
            if (this.tok.getToken().equals("else")) {
                output += "";
            } else {
                output += "[]";
            }
        } else {
            output += "[";
            output += this.left.convertToJott();
            output += "]";
        }
        if (this.right == null) {
            if (param == 0 || param == 2) {
                output += ";\n"; //newline here
            }
        } else {
            if (this.returntype != null) {
                output += ":" + this.returntype.convertToJott();
            }
            output += "{\n"; //newline here
            output += this.right.convertToJott().indent(4);
            output += "}\n"; //newline here
        }
        return output;
    }

    @Override
    public String convertToJava() {
        String output = "";
        if(this.tok.getToken().equals("elseif"))
        {
            output += "else if";
        }
        else if (this.tok.getToken().equals("print"))
        {
            output += "System.out.println";
        }
        else {
            output += this.tok.getToken();
        }
        if (this.left == null)
        {
            if (!this.tok.equals("else"))
            {
                output += "()";
            }
        }
        else
        {
            String lefty = this.left.convertToJava();
            if(output.equals("concat")){
                output = lefty.replace(", ", " + ");
            }
            else{
                output += "(" + lefty + ")";
            }
        }
        if (this.right == null)
        {
            if(param == 0 || param == 2)
            {
                output += ";\n";
            }
        }
        else
        {
            if (this.tok.getToken().equals("main"))
            {
                output = "public static void " + output;
            }
            else if (this.returntype != null)
            {
                output = "private static " + this.returntype.convertToJava() + " " + output;
            }
            else if (this.tok.getToken().equals("if") || this.tok.getToken().equals("elseif") || this.tok.getToken().equals("else") || this.tok.getToken().equals("while"))
            {
                output = output;
            }
            else
            {
                output = "private static void " + output;
            }
            output += "{\n";
            output += this.right.convertToJava().indent(4);
            if (this.tok.getToken().equals("main"))
            {
                String end = "System.exit(1);\n";
                output = output.substring(0, output.indexOf("return"));
                output += end;
            }
            output += "}\n";
        }
        return output;
    }

    private String convertConcat(String varName, String concatString){
        String output = "";
        String x = concatString.replaceAll("strcat\\(", "").replaceAll("\\)", "").replaceAll(",", "");
        String[] params = x.split(" ");

        output += "char " + varName + "[] = (char *) malloc(";
        for(String param : params){
            output += "strlen(" + param + ") + ";
        }
        output = output.substring(0, output.length()-3);
        output += ");\n";

        output += "strcpy(" + varName + ", " + "\"\"" + ");\n";
        for(String param : params){
            output += "strcat(" + varName + ", " + param + ");\n";
        }
        return output;
    }

    @Override
    public String convertToC() {
        String output = "";
        if (this.tok.getToken().equals("print")) {
            output = "printf";
        } else if (this.tok.getToken().equals("elseif")) {
            output = "else if";
        } else if (this.tok.getToken().equals("concat")) {
            output = "strcat";
        } else {
            output = this.tok.getToken();
        }
        if (param == 0) //is in a definition
        {
            for (int i = 0; i < this.root.vars.size(); i++)
            {
                String curt = this.root.vars.get(i).get(0);
                if (this.tok.getToken().equals(curt))
                {
                    this.root.currentfunc = i;
                }
            }
        }
        if (this.left == null) {
            if (this.tok.getToken().equals("else")) {
                output += "";
            } else {
                if (param == 1 || param == 2) { //0 is definition, 1 is part of params, 2 if a function call
                    output += "()";
                } else {
                    output += "(void)";
                }
            }
        } else {
            String lefty = this.left.convertToC();
            if(output.equals("printf")){
                if(lefty.contains("strcat")){
                    String varName = "printConcatHolder" + printStrcatCount;
                    printStrcatCount++;
                    output = convertConcat(varName, lefty);
                    output += "printf(\"%s\\n\", " + varName + ")";
                }
                else if(lefty.substring(0,1).matches("[0-9]")){
                    if(lefty.contains(".")){
                        output += "(\"%f\\n\", " + lefty + ")";
                    }
                    else{
                        output += "(\"%d\\n\", " + lefty + ")";
                    }
                }
                else if(lefty.equals("True") || lefty.equals("False")){
                    output += "(\"" + lefty + "\")";
                }
                else{
                    int paren = 0;
                    String lefty2 = "";
                    for (int k = 0; k < lefty.length(); k++)
                    {
                        if (lefty.charAt(k) == '(')
                        {
                            paren ++;
                        }
                        else if(lefty.charAt(k) == ')')
                        {
                            paren --;
                        }
                        if (paren == 0 && lefty.charAt(k) != ')')
                        {
                            lefty2 += lefty.charAt(k);
                        }
                    }
                    String[] usedvars = lefty2.split(" ");
                    ArrayList<String> curvars = this.root.vars.get(this.root.currentfunc);
                    boolean gotit = false;
                    Integer gotind = 0;
                    String distype = "";
                    for (int l = 0; l < usedvars.length; l++)
                    {
                        for (int m = 1; m < curvars.size(); m += 2)
                        {
                            if (usedvars[l].equals(curvars.get(m)))
                            {
                                gotit = true;
                                gotind = m;
                                distype = curvars.get(gotind + 1);
                                break;
                            }
                        }
                        if (gotit)
                        {
                            break;
                        }
                    }
                    if (!gotit)
                    {
                        for (int l = 0; l < usedvars.length; l++)
                        {
                            for(int m = 0; m < this.root.funcs.size(); m++)
                            {
                                if(usedvars[l].equals(this.root.funcs.get(m).get(0)))
                                {
                                    gotit = true;
                                    gotind = m;
                                    distype = this.root.funcs.get(m).get(1);
                                    break;
                                }
                            }
                            if(gotit)
                            {
                                break;
                            }
                        }
                    }
                    if (gotit)
                    {
                        if (distype.equals("String"))
                        {
                            output += "(\"%s\\n\", " + lefty + ")";
                        }
                        else if (distype.equals("Integer"))
                        {
                            output += "(\"%d\\n\", " + lefty + ")";
                        }
                        else if (distype.equals("Boolean")){
                            output += "(\"%s\\n\", " + lefty + " ? \"True\":\"False\")";
                        }
                        else if (distype.equals("Double"))
                        {
                            output += "(\"%f\\n\", " + lefty + ")";
                        }
                    }
                    else {
                        output += "(" + '"' + lefty + '"' + ")";
                    }
                }
            }
            else{
                output += "(" + lefty + ")";
            }
        }
        if (this.right == null) {
            if (param == 0 || param == 2) {
                output += ";\n";
            }
        } else {
            if (this.returntype != null) {
                output = this.returntype.convertToC() + " " + output;
            } else if (this.tok.getToken().equals("if") || this.tok.getToken().equals("elseif") || this.tok.getToken().equals("else") || this.tok.getToken().equals("while")) {
                output = output;
            } else {
                output = "void " + output;
            }
            output += "{\n";
            output += this.right.convertToC().indent(4);
            output += "}\n";
        }
        return output;
    }

    @Override
    public String convertToPython() {
        String output = "";
        if (this.tok.getToken().equals("elseif")) {
            output += "elif";
        } else {
            output += this.tok.getToken();
        }
        if (this.left == null) {
            if (this.tok.getToken().equals("else")) {
                output += "";
            } else {
                output += "()";
            }
        } else {
            String lefty = this.left.convertToPython();
            if(output.equals("concat")){
                output = lefty.replace(", ", " + ");
            }
            else{
                output += "(" + lefty + ")";
            }
        }
        if (this.right == null) {
            if (param == 2) {
                output += "\n";
            }
        } else {
            if (tok.getToken().equals("if") ||
                    tok.getToken().equals("elseif") ||
                    tok.getToken().equals("else") ||
                    tok.getToken().equals("while")) {
                output += ":\n";
            } else if (param == 0) {
                output = "def " + output + ":\n";
            }
            output += this.right.convertToPython().indent(4);
            output += "\n";
        }
        return output;
    }


    @Override
    public boolean validateTree() {

        //check current function node for validity

        if (this.right == null) //function call
        {
            return true;
        }
        else if (this.returntype == null) //if or while
        {

        }
        else //function definition
        {
            //make sure function def has return type
            if (this.returntype.getLeft() == null) {
                semanticError("Missing return type", this.getTok());
                return false;
            }

            //make sure return type is valid
            Token rtype = this.returntype.getLeft().getLeft().getTok();
            if (!rtype.getToken().equals("Void") &&
                    !rtype.getToken().equals("Integer") &&
                    !rtype.getToken().equals("Boolean") &&
                    !rtype.getToken().equals("Double") &&
                    !rtype.getToken().equals("String"))
            {
                semanticError("Unknown return type", rtype);
                return false;
            }

            //iterate until last line in function (return)
            JottNode curr = this.right;
            while(curr.getRight().getRight() != null)
            {
                curr = curr.getRight();
            }

            //non void function doesnt return
//            if(!rtype.getToken().equals("Void") && !curr.getLeft().getTok().getToken().equals("return"))
//            {
//                semanticError("Missing return statement for non-void function", curr.getLeft().getTok());
//                return false;
//            }
//
//            //void function has return
//            if(rtype.getToken().equals("Void") && curr.getLeft().getTok().getToken().equals("return"))
//            {
//                semanticError("Void function cannot return a value", curr.getLeft().getTok());
//                return false;
//            }

        }

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
}
