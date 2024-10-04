package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;

public class CounterVisitor extends VisitorAdaptor {

	protected int count;
	
	public int getCount(){
		return count;
	}
	
	public static class FormParamCounter extends CounterVisitor{
	
		public void visit(FormalParamDecls formParamDecl){
			count++;
		}
		public void visit(FormalParamDeclBrackets formParamDecl){
			count++;
		}
	}
	
	public static class VarCounter extends CounterVisitor{
		
		public void visit(VarTermListDecl varDecl){
			count++;
		}
		public void visit(VarTermOne varDecl){
			count++;
		}
		public void visit(VarTermListDeclBrackets varDecl){
			count++;
		}
		public void visit(VarTermOneBrackets varDecl){
			count++;
		}
	}
}
