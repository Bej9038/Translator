import java.util.Locale;

public class JottNodeOp extends JottNode{

    String resultType;

    JottNodeOp(JottNode root, Token op, JottNode left, JottNode right, Integer depth){
        super(root, left, right, depth);
        this.tok = op;
        this.type = 2;
        this.returning = 0;
    }

    @Override
    public String convertToJott() {
        String output = "";
        //Check if null
        String space = " ";
        if (this.left == null || this.right == null || this.left.isNull() || this.right.isNull()){
            space = "";
        }
        if (tok == null){
            output += this.left.convertToJott() + " " + this.right.convertToJott();
        }
        else if (tok.getTokenType() == TokenType.SEMICOLON){
            output += this.left.convertToJott() + " " + this.right.convertToJott() + ";\n";
        }
        else if (tok.getToken().equals("return")){
            output += tok.getToken();
            output += " ";
            if (this.left != null){
                output += this.left.convertToJott();
            }
            output += space;
            if (this.right != null){
                output += this.right.convertToJott();
            }
            output += ";\n";
        }
        else if (tok.getTokenType() == TokenType.ASSIGN){
            output += this.left.convertToJott() + " " + tok.getToken() + " " + this.right.convertToJott() + ";\n";
        }
        else if (tok.getTokenType() == TokenType.REL_OP || tok.getTokenType() == TokenType.MATH_OP){
            output += this.left.convertToJott() + " " + tok.getToken() + " " + this.right.convertToJott(); //No ; here
        }
        else if (tok.getTokenType() == TokenType.COLON){
            output += this.left.convertToJott() + ":" + this.right.convertToJott();
        }
        return output;
    }

    @Override
    public String convertToJava() {
        String output = "";
        String space = " ";
        if (this.left == null || this.right == null || this.left.isNull() || this.right.isNull())
        {
            space = "";
        }
        if (this.tok == null)
        {
            output += this.left.convertToJava() + " " + this.right.convertToJava();
        }
        else if (this.tok.getTokenType() == TokenType.SEMICOLON)
        {
            output += this.left.convertToJava() + " " + this.right.convertToJava() + ";\n";
        }
        else if (this.tok.getToken().equals("return"))
        {
            output += tok.getToken();
            output += " ";
            if (this.left != null)
            {
                output += this.left.convertToJava();
            }
            output += space;
            if (this.right != null)
            {
                output += this.right.convertToJava();
            }
            output += ";\n";
        }
        else if (this.tok.getTokenType() == TokenType.ASSIGN)
        {
            output += this.left.convertToJava() + " " + tok.getToken() + " " + this.right.convertToJava() + ";\n";
        }
        else if (this.tok.getTokenType() == TokenType.REL_OP || this.tok.getTokenType() == TokenType.MATH_OP)
        {
            output += this.left.convertToJava() + " " + tok.getToken() + " " + this.right.convertToJava();
        }
        else if (this.tok.getTokenType() == TokenType.COLON)
        {
            output += this.left.convertToJava() + " " + this.right.convertToJava();
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
        String space = " ";
        if (this.left == null || this.right == null || this.left.isNull() || this.right.isNull()){
            space = "";
        }
        if (tok == null){
            String lefty = this.left.convertToC();
            String righty = this.right.convertToC();
            if (lefty.equals("char*"))
            {
                output += "char " + righty + "[]";
            }
            else {
                output += lefty + " " + righty;
            }
        }
        else if (tok.getTokenType() == TokenType.SEMICOLON){
            String lefty = this.left.convertToC();
            String righty = this.right.convertToC();
            if (lefty.equals("char*"))
            {
                output += "char " + righty + "[1024];\n";
            }
            else {
                output += lefty + " " + righty + ";\n";
            }
        }
        else if (tok.getToken().equals("return")){
            output += tok.getToken();
            output += " ";
            if (this.left != null){
                output += this.left.convertToC();
            }
            output += space;
            if (this.right != null){
                output += this.right.convertToC();
            }
            output += ";\n";
        }
        else if (tok.getTokenType() == TokenType.ASSIGN){
            String lefty = this.left.convertToC();
            String righty = this.right.convertToC();
            if (lefty.contains("char"))
            {
                if (lefty.contains("[]") && righty.length() != 0)
                {
                    if(righty.contains("strcat")){
                        String varName = lefty.substring(lefty.indexOf(" ") + 1, lefty.indexOf("["));
                        output += convertConcat(varName, righty);
                    }
                    else{
                        Integer rlen = righty.length();
                        rlen -= 2;
                        if (rlen <= 0)
                        {
                            rlen = 1024;
                        }
                        lefty = lefty.replaceAll("\\[\\]", "[" + rlen + "]");
                        output += lefty + " " + tok.getToken() + " " + righty + ";\n";
                    }
                }
            } else{
                output += lefty + " " + tok.getToken() + " " + righty + ";\n";
            }
        }
        else if (tok.getTokenType() == TokenType.REL_OP || tok.getTokenType() == TokenType.MATH_OP){
            output += this.left.convertToC() + " " + tok.getToken() + " " + this.right.convertToC(); //No ; here
        }
        else if (tok.getTokenType() == TokenType.COLON){
            //output += this.left.convertToC() + " " + this.right.convertToC();
            output += this.right.convertToC() + " " + this.left.convertToC();
        }
        return output;
    }

    @Override
    public String convertToPython(){
        String output = "";
        String space = " ";
        if (this.left == null || this.right == null || this.left.isNull() || this.right.isNull()){
            space = "";
        }
        if (tok == null){
            output += this.left.convertToPython() + space + this.right.convertToPython();
        }
        else if (tok.getToken().equals("return")){
            output += tok.getToken();
            output += " ";
            if (this.left != null){
                output += this.left.convertToPython();
            }
            output += space;
            if (this.right != null){
                output += this.right.convertToPython();
            }
            output += "\n";
        }
        else if (tok.getTokenType() == TokenType.ASSIGN){
            output += this.left.convertToPython() + " " + tok.getToken() + " " + this.right.convertToPython() + "\n";
        }
        else if (tok.getTokenType() == TokenType.REL_OP || tok.getTokenType() == TokenType.MATH_OP){
            output += this.left.convertToPython() + " " + tok.getToken() + " " + this.right.convertToPython();
        }
        else if (tok.getTokenType() == TokenType.COLON){
            output += this.left.convertToPython();
        }
        return output;
    }

    @Override
    public boolean validateTree() {
        // Done first so that assignment operations can compare the right side's type when it is a op node
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

        //check current function node for validity

        //invalid type assigned to variable
//        if(this.tok != null)
//        {
//            if(this.tok.getTokenType() == TokenType.MATH_OP)
//            {
////                Token rhs = null;
////                if(this.getRight().getType() == 3)
////                {
////                    rhs = this.getRight().getTok();
////                }
////                else {
////                    // gets the already confirmed type of the operation from its right child
////                    JottNode rightNode = this.getRight(); //get a child of the op node
////                    while(rightNode.getTok().getTokenType() != TokenType.NUMBER){
////                        rightNode = rightNode.getRight();
////                    }
////                    rhs = rightNode.getTok();
////                }
////
////                Token lhs = this.getLeft().getTok();
////                if(!(lhs.getTokenType() == TokenType.ID_KEYWORD || rhs.getTokenType() == TokenType.ID_KEYWORD))
////                {
////                    if(!((lhs.getTokenType() == TokenType.NUMBER && lhs.getToken().contains(".") && rhs.getTokenType() == TokenType.NUMBER && rhs.getToken().contains(".")) ||
////                        (lhs.getTokenType() == TokenType.NUMBER && !lhs.getToken().contains(".") && rhs.getTokenType() == TokenType.NUMBER && !rhs.getToken().contains("."))))
////                    {
////                        semanticError("Invalid math operation. Both vars must be of type <int> or type <dbl>", lhs);
////                        return false;
////                    }
////                }
//            }
//            else if(this.tok.getTokenType() == TokenType.ASSIGN)
//            {
//                Token rhs;
//                if(this.getRight().getType() == 3) //basenode
//                {
//                    rhs = this.getRight().getTok();
//                }
//                else //opnode
//                {
//                    JottNode rightNode = this.getRight(); //get a child of the op node
//                    while(rightNode.getTok().getTokenType() != TokenType.NUMBER){
//                        rightNode = rightNode.getRight();
//                    }
//                    rhs = rightNode.getTok();
//                    //op node
//                }
//                Token lhs = this.getLeft().getRight().getTok();
//                Token lhsVarType = this.getLeft().getLeft().getTok();
//                if(lhsVarType.getToken().equals("Integer"))
//                {
//                    if(rhs.getTokenType() != TokenType.NUMBER || (rhs.getTokenType() == TokenType.NUMBER && rhs.getToken().contains(".")))
//                    {
//                        semanticError("Invalid type assigned into variable. Expected type <int>", lhs);
//                        return false;
//                    }
//                }
//                else if(lhsVarType.getToken().equals("Double"))
//                {
//                    if(rhs.getTokenType() != TokenType.NUMBER || (rhs.getTokenType() == TokenType.NUMBER && !rhs.getToken().contains("."))) {
//                        semanticError("Invalid type assigned into variable. Expected type <dbl>", lhs);
//                        return false;
//                    }
//                }
//                else if(lhsVarType.getToken().equals("String"))
//                {
//                    if(rhs.getTokenType() != TokenType.STRING)
//                    {
//                        semanticError("Invalid type assigned into variable. Expected type <str>", lhs);
//                        return false;
//                    }
//                }
//                else if(lhsVarType.getToken().equals("Boolean"))
//                {
//                    if((!rhs.getToken().equals("True")) && !rhs.getToken().equals("False"))
//                    {
//                        semanticError("Invalid type assigned into variable. Expected type <bool>", lhs);
//                        return false;
//                    }
//                }
//            }
//        }
        return true;
    }
}
