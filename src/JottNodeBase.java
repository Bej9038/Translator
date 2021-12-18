public class JottNodeBase extends JottNode{
    //keywords: true, false, elseif, else

    JottNodeBase(JottNode root, Token tok, Integer depth){
        super(root, null, null, depth);
        this.tok = tok;
        this.type = 3;
        this.returning = 0;
    }

    public Token getTok()
    {
        return this.tok;
    }

    @Override
    public String convertToJott() {
        return this.tok.getToken();
    }

    @Override
    public String convertToJava(){ return this.tok.getToken(); }

    @Override
    public String convertToC(){
        if (this.tok.getTokenType() == TokenType.ID_KEYWORD){
            if (this.tok.getToken().equals("Integer")){
                return "int";
            }
            else if (this.tok.getToken().equals("String")){
                return "char*";
            }
            if (this.tok.getToken().equals("Double")){
                return "double";
            }
            if (this.tok.getToken().equals("Boolean")){
                return "bool";
            }
        }
        return this.tok.getToken();
    }
    @Override
    public String convertToPython(){
        if (this.tok.getTokenType() == TokenType.ID_KEYWORD){
            if (this.tok.getToken().equals("Integer")||
                    this.tok.getToken().equals("Double") ||
                    this.tok.getToken().equals("Boolean") ||
                    this.tok.getToken().equals("String")){
                return "";
            }
        }
        return this.tok.getToken();
    }

    @Override
    public boolean validateTree() {

        //check current function node for validity

        //if no issues found

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
