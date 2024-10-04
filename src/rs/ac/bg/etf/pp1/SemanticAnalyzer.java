package rs.ac.bg.etf.pp1;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {

	ArrayList<Struct> actualParamTypes=new ArrayList<Struct>();
	int printCallCount = 0, currLevel=0;
	Obj currentMethod = null;
	int localVarDeclCount=0, varDeclCount = 0, cnstDeclCount = 0, paramDeclCount = 0;
	Struct currVarDeclType=null, currConstDeclType=null, bracketDesignatorType=null, bracketDesignatorARRType=null;
	int currActualParams=0;
	Obj currNamespace = null;
	boolean returnFound = false;
	int inFor = 0;
	boolean errorDetected = false;
	int nVars;
	private ArrayList<String> nmspcNames = new ArrayList<String>();
	
	Logger log = Logger.getLogger(getClass());
	
	//===========================REPORTS:==============================

	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}
	//===========================================================
	
	//=====================VISITS:===============================
	
	//----------------------PROG:------------------------
    public void visit(ProgName progName){
    	Tab.insert(Obj.Type, "bool", new Struct(Struct.Bool));
    	
    	progName.obj = Tab.insert(Obj.Prog, progName.getProgName(), Tab.noType);
    	Tab.openScope();
    }
    public void visit(Program program){
    	Tab.insert(Obj.Var, "helper1", Tab.intType);
    	Tab.insert(Obj.Var, "helper2", Tab.intType);
    	nVars = Tab.currentScope.getnVars();
    	Tab.chainLocalSymbols(program.getProgName().obj);
    	Tab.closeScope();
    }
    //-------------------------------------------------
    
	//------------------VAR TYPE CHECK:----------------
	public void visit(VarDecl varDecl){
		currVarDeclType = null;
	}
	public void visit(NoDeclarationsList a) {
//		report_info("NODECL ENDED", a);
	}
	public void visit(VarType varType){		
		currVarDeclType = varType.getType().struct;
	}
	public void visit(VarTermListDecl varTermListDecl){
		String prefix="";
		if(currNamespace!=null && currentMethod==null) prefix=currNamespace.getName()+"::";
		varTermListDecl.setVarName(prefix+varTermListDecl.getVarName());
		Obj o=Tab.find(varTermListDecl.getVarName());
    	if(o != Tab.noObj) {
    		report_error("Vec postoji objekat sa imenom "+varTermListDecl.getVarName()+". Greska", varTermListDecl);
    		return;
    	}
		if(currentMethod==null) {
			if(++varDeclCount>65536) report_error("GRESKA: Broj globalnih promenljivih prelazi 65536.", varTermListDecl);
			report_info("Deklarisana globalna promenljiva "+ varTermListDecl.getVarName(), varTermListDecl);
		}else{
			if(++localVarDeclCount+currActualParams>256) report_error("GRESKA: Broj lokalnih promenljivih prelazi 256.", varTermListDecl);
			report_info("Deklarisana lokalna promenljiva "+ varTermListDecl.getVarName(), varTermListDecl);
		}
		Obj varNode = Tab.insert(Obj.Var, varTermListDecl.getVarName(), currVarDeclType);
	}
	public void visit(VarTermOne varTermOne){
		String prefix="";
		if(currNamespace!=null && currentMethod==null) prefix=currNamespace.getName()+"::";
		varTermOne.setVarName(prefix+varTermOne.getVarName());
		Obj o=Tab.find(varTermOne.getVarName());
    	if(o != Tab.noObj) {
    		report_error("Vec postoji objekat sa imenom "+varTermOne.getVarName()+". Greska", varTermOne);
    		return;
    	}
    	if(currentMethod==null) {
			if(++varDeclCount>65536) report_error("GRESKA: Broj globalnih promenljivih prelazi 65536.", varTermOne);
			report_info("Deklarisana globalna promenljiva "+ varTermOne.getVarName(), varTermOne);
		}else{
			if(++localVarDeclCount+currActualParams>256) report_error("GRESKA: Broj lokalnih promenljivih prelazi 256.", varTermOne);
			report_info("Deklarisana lokalna promenljiva "+ varTermOne.getVarName(), varTermOne);
		}
		Obj varNode = Tab.insert(Obj.Var, varTermOne.getVarName(), currVarDeclType);
	}
	public void visit(VarTermListDeclBrackets varTermListDecl){
		String prefix="";
		if(currNamespace!=null && currentMethod==null) prefix=currNamespace.getName()+"::";
		varTermListDecl.setVarName(prefix+varTermListDecl.getVarName());
		Obj o=Tab.find(varTermListDecl.getVarName());
    	if(o != Tab.noObj) {
    		report_error("Vec postoji objekat sa imenom "+varTermListDecl.getVarName()+". Greska", varTermListDecl);
    		return;
    	}
    	if(currentMethod==null) {
			if(++varDeclCount>65536) report_error("GRESKA: Broj globalnih promenljivih prelazi 65536.", varTermListDecl);
			report_info("Deklarisana globalna arr promenljiva "+ varTermListDecl.getVarName(), varTermListDecl);
		}else{
			if(++localVarDeclCount+currActualParams>256) report_error("GRESKA: Broj lokalnih promenljivih prelazi 256.", varTermListDecl);
			report_info("Deklarisana lokalna arr promenljiva "+ varTermListDecl.getVarName(), varTermListDecl);
		}
		Obj varNode = Tab.insert(Obj.Var, varTermListDecl.getVarName(), new Struct(Struct.Array, currVarDeclType));
	}
	public void visit(VarTermOneBrackets varTermOne){
		String prefix="";
		if(currNamespace!=null && currentMethod==null) prefix=currNamespace.getName()+"::";
		varTermOne.setVarName(prefix+varTermOne.getVarName());
		Obj o=Tab.find(varTermOne.getVarName());
    	if(o != Tab.noObj) {
    		report_error("Vec postoji objekat sa imenom "+varTermOne.getVarName()+". Greska", varTermOne);
    		return;
    	}
    	if(currentMethod==null) {
			if(++varDeclCount>65536) report_error("GRESKA: Broj globalnih promenljivih prelazi 65536.", varTermOne);
			report_info("Deklarisana globalna arr promenljiva "+ varTermOne.getVarName(), varTermOne);
		}else{
			if(++localVarDeclCount+currActualParams>256) report_error("GRESKA: Broj lokalnih promenljivih prelazi 256.", varTermOne);
			report_info("Deklarisana localna arr promenljiva "+ varTermOne.getVarName(), varTermOne);
		}
		Obj varNode = Tab.insert(Obj.Var, varTermOne.getVarName(), new Struct(Struct.Array, currVarDeclType));
	}
	public void visit(FormalParamDecls formPar){
		if(currActualParams>256) report_error("GRESKA: Broj lokalnih promenljivih prelazi 256.", formPar);
		report_info("Deklarisana parametarska promenljiva "+ formPar.getFParName(), formPar);
		Obj varNode = Tab.insert(Obj.Var, formPar.getFParName(), formPar.getType().struct);
		varNode.setAdr(paramDeclCount++);
		varNode.setLevel(currLevel);
		report_info("Deklarisana parametar "+ formPar.getFParName(), formPar);
	}
	public void visit(FormalParamDeclBrackets formPar){
		if(currActualParams>256) report_error("GRESKA: Broj lokalnih promenljivih prelazi 256.", formPar);
		report_info("Deklarisana parametarska promenljiva "+ formPar.getFParName(), formPar);
		Obj varNode = Tab.insert(Obj.Var, formPar.getFParName(), new Struct(Struct.Array, formPar.getType().struct));
		varNode.setAdr(paramDeclCount++);
		varNode.setLevel(currLevel);
		report_info("Deklarisan arr parametar "+ formPar.getFParName(), formPar);
	}
	public void visit(ErrorVarTerm err) {
		report_error("Greska! Neispravno deklarisanje promenljive", err);
	}
	//-----------------------------------------
	
	//---------CONST TYPE CHECK:---------------
	public void visit(ConstDecl constDecl){
		currConstDeclType = null;
	}
	public void visit(ConstType constType){		
		currConstDeclType = constType.getType().struct;
	}
	public void visit(ConstTermListDeclNum c){
//		if(currConstDeclType.getKind()==Struct.Int) {
			String prefix="";
			if(currNamespace!=null) prefix=currNamespace.getName()+"::";
			c.setName(prefix+c.getName());
			Obj o=Tab.find(c.getName());
	    	if(o != Tab.noObj) {
	    		report_error("Vec postoji objekat sa imenom "+c.getName()+". Greska", c);
	    		return;
	    	}
			cnstDeclCount++;
			report_info("Deklarisana konstanta "+ c.getName(), c);
			Obj cNode = Tab.insert(Obj.Con, c.getName(), currConstDeclType);
			cNode.setAdr(c.getN());
			cNode.setLevel(currLevel);
//		}
	}
	public void visit(ConstTermOneNum c){
		String prefix="";
		if(currNamespace!=null) prefix=currNamespace.getName()+"::";
		c.setName(prefix+c.getName());
		Obj o=Tab.find(c.getName());
    	if(o != Tab.noObj) {
    		report_error("Vec postoji objekat sa imenom "+c.getName()+". Greska", c);
    		return;
    	}
		cnstDeclCount++;
		report_info("Deklarisana konstanta "+ c.getName(), c);
		Obj cNode = Tab.insert(Obj.Con, c.getName(), currConstDeclType);
		cNode.setAdr(c.getN());
//		report_info("DDDDDDD"+ cNode.getAdr(), c);
		cNode.setLevel(currLevel);
	}
	public void visit(ConstTermListDeclBool c){
		String prefix="";
		if(currNamespace!=null) prefix=currNamespace.getName()+"::";
		c.setName(prefix+c.getName());
		Obj o=Tab.find(c.getName());
    	if(o != Tab.noObj) {
    		report_error("Vec postoji objekat sa imenom "+c.getName()+". Greska", c);
    		return;
    	}
		cnstDeclCount++;
		report_info("Deklarisana konstanta "+ c.getName(), c);
		Obj cNode = Tab.insert(Obj.Con, c.getName(), currConstDeclType);
		cNode.setAdr(c.getB());
		cNode.setLevel(currLevel);
	}
	public void visit(ConstTermOneBool c){
		String prefix="";
		if(currNamespace!=null) prefix=currNamespace.getName()+"::";
		c.setName(prefix+c.getName());
		Obj o=Tab.find(c.getName());
    	if(o != Tab.noObj) {
    		report_error("Vec postoji objekat sa imenom "+c.getName()+". Greska", c);
    		return;
    	}
		cnstDeclCount++;
		report_info("Deklarisana konstanta "+ c.getName(), c);
		Obj cNode = Tab.insert(Obj.Con, c.getName(), currConstDeclType);
		cNode.setAdr(c.getB());
		cNode.setLevel(currLevel);
	}
	public void visit(ConstTermListDeclChar c){
		String prefix="";
		if(currNamespace!=null) prefix=currNamespace.getName()+"::";
		c.setName(prefix+c.getName());
		Obj o=Tab.find(c.getName());
    	if(o != Tab.noObj) {
    		report_error("Vec postoji objekat sa imenom "+c.getName()+". Greska", c);
    		return;
    	}
		cnstDeclCount++;
		report_info("Deklarisana konstanta "+ c.getName(), c);
		Obj cNode = Tab.insert(Obj.Con, c.getName(), currConstDeclType);
		cNode.setAdr(c.getC());
		cNode.setLevel(currLevel);
	}
	public void visit(ConstTermOneChar c){
		//		"\"".*"\"" {returnnew_symbol(sym.STRING, new String(yytext().substring(1, yytext().length()-1)));}
		String prefix="";
		if(currNamespace!=null) prefix=currNamespace.getName()+"::";
		c.setName(prefix+c.getName());
		Obj o=Tab.find(c.getName());
    	if(o != Tab.noObj) {
    		report_error("Vec postoji objekat sa imenom "+c.getName()+". Greska", c);
    		return;
    	}
		cnstDeclCount++;
		report_info("Deklarisana konstanta "+ c.getName(), c);
		Obj cNode = Tab.insert(Obj.Con, c.getName(), currConstDeclType);
		cNode.setAdr(c.getC());
		cNode.setLevel(currLevel);
	}
	public void visit(ErrorConstTerm err) {
		report_error("Greska! Neispravno deklarisanje konstante", err);
	}
	//----------------------------------------------
	
	//---------------------TYPES:------------------------
    public void visit(TypeFromNamespace type){
    	Obj typeNode = Tab.find(type.getTypeName());//nmspc
    	if(typeNode == Tab.noObj){
    		report_error("Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola! ", null);
    		type.struct = Tab.noType;
    	}else{
    		if(Obj.Type == typeNode.getKind()){
//    			report_info("Pronadjen tip "+ type.getTypeName(), type);
    			type.struct = typeNode.getType();
    			type.setTypeName(typeNode.getName());
    		}else{
    			report_error("Greska: Ime " + type.getTypeName() + " ne predstavlja tip!", type);
    			type.struct = Tab.noType;
    		}
    	}
    }
    public void visit(TypeNormal type){
    	Obj typeNode = Tab.find(type.getTypeName());
    	if(typeNode == Tab.noObj){
    		report_error("Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola! ", null);
    		type.struct = Tab.noType;
    	}else{
    		if(Obj.Type == typeNode.getKind()){
//    			report_info("Pronadjen tip "+ type.getTypeName(), type);
    			type.struct = typeNode.getType();
    			type.setTypeName(typeNode.getName());
    		}else{
    			report_error("Greska: Ime " + type.getTypeName() + " ne predstavlja tip!", type);
    			type.struct = Tab.noType;
    		}
    	}
    }
	//---------------------------------------
    
    //-------------------NAMESPACES:----------------
    public void visit(NamespaceName nmspcName) {
    	Obj nmspcObj=Tab.find(nmspcName.getNsName());
    	if(nmspcObj != Tab.noObj) {
    		report_error("Greska! Vec postoji objekat sa imenom "+nmspcName.getNsName(), nmspcName);
    		return;
    	}
    	currNamespace = Tab.insert(Obj.NO_VALUE, nmspcName.getNsName(), Tab.nullType);
    	nmspcName.obj=currNamespace;
    }
    public void visit(Namespace nmspc) {
    	report_info("NMSPC ENDED ", nmspc);
    	currNamespace=null;
    }
    //---------------------------------------
	
	//----------------METHODS & RETURN TYPES:----------------------
    public void visit(MethodTypeNameAny methodTypeName){
    	String prefix="";
		if(currNamespace!=null) prefix=currNamespace.getName()+"::";
		methodTypeName.setMethName(prefix+methodTypeName.getMethName());
    	Obj methObj=Tab.find(methodTypeName.getMethName());
    	if(methObj != Tab.noObj) {
    		report_error("Greska! Vec postoji objekat sa imenom "+methodTypeName.getMethName(), methodTypeName);
    	}
    	else {
        	currentMethod = Tab.insert(Obj.Meth, methodTypeName.getMethName(), methodTypeName.getType().struct);
        	methodTypeName.obj = currentMethod;
        	++currLevel;
        	Tab.openScope();
    		report_info("Obradjuje se funkcija " + methodTypeName.getMethName(), methodTypeName);
    	}
    }
    public void visit(VoidMethodTypeName methodTypeName){
    	String prefix="";
		if(currNamespace!=null) prefix=currNamespace.getName()+"::";
		methodTypeName.setMethName(prefix+methodTypeName.getMethName());
    	Obj methObj=Tab.find(methodTypeName.getMethName());
    	if(methObj != Tab.noObj) {
    		report_error("Greska! Vec postoji objekat sa imenom "+methodTypeName.getMethName(), methodTypeName);
    	}
    	else {
        	currentMethod = Tab.insert(Obj.Meth, methodTypeName.getMethName(), Tab.noType);
        	methodTypeName.obj = currentMethod;
        	++currLevel;
        	Tab.openScope();
    		report_info("Obradjuje se funkcija " + methodTypeName.getMethName(), methodTypeName);
    	}
    }
    public void visit(MethodDecl methodDecl){
    	if(!returnFound && currentMethod.getType() != Tab.noType){
			report_error("Semanticka greska na liniji " + methodDecl.getLine() + ": funkcija " + currentMethod.getName() + " nema return iskaz!", null);
    	}
    	currentMethod.setLevel(paramDeclCount);//postavljanje br parametara
    	//reset svega sto se menjalo:
    	Tab.chainLocalSymbols(currentMethod);
    	Tab.closeScope();
    	--currLevel;
    	paramDeclCount=0;
    	returnFound = false;
    	currentMethod = null;
    }
    public void visit(RetStmt returnExpr){
		returnFound = true;
		Struct currMethType = currentMethod.getType();
		if(!currMethType.compatibleWith(returnExpr.getExpr().struct)){
			report_error("Greska na liniji " + returnExpr.getLine() + " : " + "tip izraza u return naredbi ne slaze se sa tipom povratne vrednosti funkcije " + currentMethod.getName(), null);
		}
	}
    //-------------------------------------------------
    
    //--------------FACTOR, TERM, EXPR:-------------------------
    public void visit(FactorFuncCall funcCall){
    	String name=funcCall.getDesignator().obj.getName();
    	Obj func1 = Tab.find(name);
    	if(func1 == Tab.noObj){
    		if(currNamespace!=null) {
        		Designator d = funcCall.getDesignator();
        		if(d instanceof DesignatorList) {
        			name = ""+currNamespace.getName()+"::"+((DesignatorList) d).getDesignatorName().getName();
        			Obj o = funcCall.getDesignator().obj;
        			funcCall.getDesignator().obj=new Obj(o.getKind(),name,o.getType(), o.getAdr(), o.getLevel());
        		}
//        		report_info("*nmspc func*",null);
        	}
    	}
    	func1 = Tab.find(name);
    	Obj func = func1;
    	if(Obj.Meth == func.getKind()){
			report_info("Pronadjen poziv funkcije " + func.getName() + " na liniji " + funcCall.getLine(), null);
			funcCall.struct = func.getType();
    	}else{
			report_error("Greska na liniji " + funcCall.getLine()+" : ime " + func.getName() + " nije funkcija!", null);
			funcCall.struct = Tab.noType;
    	}
    	if(func.getLevel()!=currActualParams) {
    		report_error("Greska na liniji " + funcCall.getLine()+ " : neodgovarajuci broj argumenata! "+func.getLevel()+" umesto "+currActualParams, null);
    	}
    	func.getLocalSymbols().forEach(param->{
    		if(!func.getLocalSymbols().isEmpty()&&!actualParamTypes.isEmpty()&&param.getAdr()<actualParamTypes.size()) {
    			if(param.getType() == actualParamTypes.get(param.getAdr())) {//ako su isti tip
        			if(param.getType().getKind()==Struct.Array) {//e sad ako su arr mora biti isti elemtype
        				if(param.getType().getElemType() == (actualParamTypes.get(param.getAdr()).getElemType())) {
        					//sve ok
        				}
        				else report_error("Greska na liniji " + funcCall.getLine()+ " : neodgovarajuci tip "+param.getAdr()+". argumenta! ", null);
        			}
        			//sve ok
        		}//ako nisu isti tip
        		else report_error("Greska na liniji " + funcCall.getLine()+ " : neodgovarajuci tip "+param.getAdr()+". argumenta! ", null);
        	
    		}
    	});
    	actualParamTypes=new ArrayList<Struct>();
    	currActualParams=0;
    }
    public void visit(OneFact term){
    	term.struct = term.getFactor().struct;
    }
    public void visit(TermList term){
    	Struct te = term.getTerm().struct;
    	Struct t = term.getFactor().struct;
    	if(te.equals(t) && te == Tab.intType){
    		term.struct = te;
    	}else{
			report_error("Greska na liniji "+ term.getLine()+" : nekompatibilni tipovi u izrazu za mnozenje.", null);
			term.struct = Tab.noType;
    	}
    }
    public void visit(TermExpr termExpr){
    	termExpr.struct = termExpr.getTerm().struct;
    }
    public void visit(MinusTermExpr termExpr){
    	termExpr.struct = termExpr.getTerm().struct;
    	Struct t=termExpr.getTerm().struct;
    	if(t == Tab.intType){
    		termExpr.struct = t;
    	}else{
			report_error("Greska na liniji "+ termExpr.getLine()+" : nekompatibilni tipovi u izrazu za [minus]<izraz>.", null);
			termExpr.struct = Tab.noType;
    	}
    }
    public void visit(AddExpr addExpr){
    	report_info("Pronadjen addExpr" + " na liniji " + addExpr.getLine(), null);
    	Struct te = addExpr.getExpr().struct;
    	Struct t = addExpr.getTerm().struct;
    	if(te.equals(t) && te == Tab.intType){
    		addExpr.struct = te;
    	}else{
			report_error("Greska na liniji "+ addExpr.getLine()+" : nekompatibilni tipovi u izrazu za sabiranje.", null);
			addExpr.struct = Tab.noType;
    	}
    }
    public void visit(FactorNumConst cnst){
    	cnst.struct = Tab.intType;
    }
    public void visit(FactorCharConst cnst){
    	cnst.struct = Tab.charType;
//    	cnst.struct = Tab.intType;
    }
    public void visit(FactorBoolConst cnst){
    	Obj o=Tab.find("bool");
    	cnst.struct = o.getType();
//    	cnst.struct = Tab.intType;
    }
    public void visit(FactorVar var){
    	var.struct = var.getDesignator().obj.getType();
    }
    public void visit(ParenExpr e){
    	e.struct = e.getExpr().struct;
    }
    public void visit(NewArr na){
    	Struct t = na.getExpr().struct;
    	if(t.equals(Tab.intType)){
    		na.struct = new Struct(Struct.Array, na.getType().struct);
    	}else{
			report_error("Greska na liniji "+ na.getLine()+" : nekompatibilni tip u izracunavanju unutar [].", null);
			na.struct = Tab.noType;
    	}
    	
    }
    public void visit(FactorRange fr){
    	Struct t = fr.getExpr().struct;
    	if(t.equals(Tab.intType)){
    		fr.struct = new Struct(Struct.Array, t);
    	}else{
			report_error("Greska na liniji "+ fr.getLine()+" : nedozvoljen tip u range( x )", null);
			fr.struct = Tab.noType;
    	}
    	
    }
    //-----------------------------------
    
    //-------------DESIGNATORS:-----------------------
    public void visit(DesignatorName dn) {
    	SyntaxNode parent = dn.getParent();
    	if(dn.getParent() instanceof DesignatorListNmspc) {
    		DesignatorListNmspc p=(DesignatorListNmspc)parent;
    		dn.setName(p.getNmspc()+"::"+dn.getName());
    	}
    	if(dn.getParent() instanceof DesignatorList) {
    		//ok
    	}
    	Obj obj = Tab.find(dn.getName());
    	if(obj == Tab.noObj) {
    		if(currNamespace!=null)obj = Tab.find(currNamespace.getName()+"::"+dn.getName());
    		if(obj == Tab.noObj) {
    			for(int i=0;i<nmspcNames.size();++i) {
    				obj=Tab.find(nmspcNames.get(i)+"::"+dn.getName());
    				if(obj != Tab.noObj) break;
    			}
    		}
    	}
    	if(obj == Tab.noObj){
			report_error("Greska na liniji " + dn.getLine()+ " : ime "+dn.getName()+" nije deklarisano! ", null);
    	}
    	dn.obj=obj;
    }
    public void visit(DesignatorList designator) {
//    	Obj obj = Tab.find(designator.getDesignatorName().getName());
//    	if(obj == Tab.noObj){
//			report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignatorName().getName()+" nije deklarisano! ", null);
//    	}
//    	designator.obj = obj;
    	designator.obj=designator.getDesignatorName().obj;
    }
    public void visit(DesignatorListNmspc designator) {
//    	designator.getDesignatorName().setName(designator.getNmspc()+"::"+designator.getDesignatorName().getName());
//    	Obj obj = Tab.find(designator.getDesignatorName().getName());
//    	if(obj == Tab.noObj){
//			report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignatorName().getName()+" nije deklarisano! ", null);
//    	}
//    	designator.obj = obj;
    	designator.obj=designator.getDesignatorName().obj;
    }
//    public void visit(ArrIdents arrI) {
//    	Obj obj = Tab.find(arrI.getArrName());
//    	if(obj == Tab.noObj){
//			report_error("Greska na liniji " + arrI.getLine()+ " : ime "+arrI.getArrName()+" nije deklarisano! ", null);
//    	}
//    	arrI.obj = obj;
//    }
//    public void visit(ArrIdentNmspc arrI) {
//    	arrI.setArrName(arrI.getNmspc()+"::"+arrI.getArrName());
//    	Obj obj = Tab.find(arrI.getArrName());
//    	if(obj == Tab.noObj){
//			report_error("Greska na liniji " + arrI.getLine()+ " : ime "+arrI.getArrName()+" nije deklarisano! ", null);
//    	}
//    	arrI.obj = obj;
//    }
    public void visit(OneDesignatorArr designatorArr) {
//    	report_info("GEGEGEGE: "+designatorArr.obj+", "+designatorArr.getDesignator().obj, designatorArr);
    	designatorArr.obj=designatorArr.getDesignator().obj;//ili da se svuda menja??
    	report_info(designatorArr.obj.getName(), null);
    	Obj obj = Tab.find(designatorArr.obj.getName());
    	if(obj == Tab.noObj){
			report_error("Greska na liniji " + designatorArr.getLine()+ " : ime "+designatorArr.obj.getName()+" nije deklarisano! ", null);
    	}
    	if(designatorArr.obj.getType().getKind() != Struct.Array){
			report_error("Greska na liniji " + designatorArr.getLine()+ " : ime "+designatorArr.obj.getName()+" nije niz! ", null);
    	}
    	if(designatorArr.getExpr().struct != Tab.intType){
			report_error("Greska na liniji " + designatorArr.getLine() + ", pogresan tip unutar []", null);
    	}
    	designatorArr.obj=new Obj(Obj.Elem, designatorArr.obj.getName(), designatorArr.obj.getType().getElemType());
//    	designatorArr.getDesignator().obj=new Obj(Obj.Elem, designatorArr.obj.getName(), designatorArr.obj.getType().getElemType());
    }
    // *********DESIGNATOR STATEMENTS:*************
    public void visit(DesignatorProc designator) {
    	String name=designator.getDesignator().obj.getName();
    	Obj obj1 = Tab.find(name);
    	if(obj1 == Tab.noObj){
        	if(currNamespace!=null) {
        		Designator d = designator.getDesignator();
        		if(d instanceof DesignatorList) {
        			name = ""+currNamespace.getName()+"::"+((DesignatorList) d).getDesignatorName().getName();
        			Obj o = designator.getDesignator().obj;
        			designator.getDesignator().obj=new Obj(o.getKind(),name,o.getType(), o.getAdr(), o.getLevel());
        		}
        	}
    	}
    	obj1 = Tab.find(name);
    	Obj obj=obj1;
    	if(obj == Tab.noObj){
			report_error("Greska na linijii " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije deklarisano! ", null);
    	}
    	if(obj.getKind() != Obj.Meth) {
    		report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije funkcija! ", null);
    	}
    	if(obj.getLevel()!=currActualParams) {
    		report_error("Greska na liniji " + designator.getLine()+ " : neodgovarajuci broj argumenata! "+obj.getLevel()+" umesto "+currActualParams, null);
    	}
    	obj.getLocalSymbols().forEach(param->{
    		if(!obj.getLocalSymbols().isEmpty()&&!actualParamTypes.isEmpty()&&param.getAdr()<actualParamTypes.size()) {
    			report_info(""+param.getAdr()+", "+param.getName(),designator);
    			if(param.getType() == 
    					actualParamTypes.get(param.getAdr())) {//ako su isti tip
        			if(param.getType().getKind()==Struct.Array) {//e sad ako su arr mora biti isti elemtype
        				if(param.getType().getElemType() == (actualParamTypes.get(param.getAdr()).getElemType())) {
        					//sve ok
        				}
        				else report_error("Greska na liniji " + designator.getLine()+ " : neodgovarajuci tip "+param.getAdr()+". argumenta! ", null);
        			}
        			//sve ok
        		}//ako nisu isti tip
        		else report_error("Greska na liniji " + designator.getLine()+ " : neodgovarajuci tip "+param.getAdr()+". argumenta! ", null);
    		}
    	});
    	actualParamTypes=new ArrayList<Struct>();
    	currActualParams=0;
    }
    public void visit(DesignatorAssign designator) {
//    	String name=designator.getDesignator().obj.getName();
//    	Obj obj1 = Tab.find(name);
//    	if(obj1 == Tab.noObj){
//        	if(currNamespace!=null) {
//        		Designator d = designator.getDesignator();
//        		if(d instanceof DesignatorList) {
//        			name = ""+currNamespace.getName()+"::"+((DesignatorList) d).getDesignatorName().getName();
//        			Obj o = designator.getDesignator().obj;
//        			designator.getDesignator().obj=new Obj(o.getKind(),name,o.getType(), o.getAdr(), o.getLevel());
//        		}
//        	}
//    	}
//    	obj1 = Tab.find(name);
//    	Obj obj=obj1;
    	Obj obj = Tab.find(designator.getDesignator().obj.getName());
    	if(obj == Tab.noObj){
			report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije deklarisano! ", null);
    	}
    	if(designator.getDesignator().obj.getKind()!=Obj.Var && designator.getDesignator().obj.getKind()!=Obj.Elem) {
    		report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije ni element niza ni promenljiva! ", null);
    	}
    	//mozda assignableTo? vvv
    	if(!designator.getDesignator().obj.getType().compatibleWith(designator.getExpr().struct)) {
    		report_error("Greska na liniji "+ designator.getLine()+" : nekompatibilni tipovi za dodelu vrednosti.", null);
    	}
    }
    public void visit(DesignatorInc designator) {
//    	String name=designator.getDesignator().obj.getName();
//    	Obj obj1 = Tab.find(name);
//    	if(obj1 == Tab.noObj){
//        	if(currNamespace!=null) {
//        		Designator d = designator.getDesignator();
//        		if(d instanceof DesignatorList) {
//        			name = ""+currNamespace.getName()+"::"+((DesignatorList) d).getDesignatorName().getName();
//        			Obj o = designator.getDesignator().obj;
//        			designator.getDesignator().obj=new Obj(o.getKind(),name,o.getType(), o.getAdr(), o.getLevel());
//        		}
//        	}
//    	}
//    	obj1 = Tab.find(name);
//    	Obj obj=obj1;
    	Obj obj = Tab.find(designator.getDesignator().obj.getName());
    	if(obj == Tab.noObj){
			report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije deklarisano! ", null);
    	}
    	if(designator.getDesignator().obj.getKind()!=Obj.Var && designator.getDesignator().obj.getKind()!=Obj.Elem) {
    		report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije ni element niza ni promenljiva! ", null);
    	}
    	if(designator.getDesignator().obj.getType() != Tab.intType) {
    		report_error("Greska na liniji " + designator.getLine() + ", pogresan tip za operator ++", null);
    	}
    }
    public void visit(DesignatorDec designator) {
//    	String name=designator.getDesignator().obj.getName();
//    	Obj obj1 = Tab.find(name);
//    	if(obj1 == Tab.noObj){
//        	if(currNamespace!=null) {
//        		Designator d = designator.getDesignator();
//        		if(d instanceof DesignatorList) {
//        			name = ""+currNamespace.getName()+"::"+((DesignatorList) d).getDesignatorName().getName();
//        			Obj o = designator.getDesignator().obj;
//        			designator.getDesignator().obj=new Obj(o.getKind(),name,o.getType(), o.getAdr(), o.getLevel());
//        		}
//        	}
//    	}
//    	obj1 = Tab.find(name);
//    	Obj obj=obj1;
    	Obj obj = Tab.find(designator.getDesignator().obj.getName());
    	if(obj == Tab.noObj){
			report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije deklarisano! ", null);
    	}
    	if(designator.getDesignator().obj.getKind()!=Obj.Var && designator.getDesignator().obj.getKind()!=Obj.Elem) {
    		report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije ni element niza ni promenljiva! ", null);
    	}
    	if(designator.getDesignator().obj.getType() != Tab.intType) {
    		report_error("Greska na liniji " + designator.getLine() + ", pogresan tip za operator --", null);
    	}
    }
    public void visit(DesignatorBrackets designator) {
    	Obj obj = Tab.find(designator.getDesignator().obj.getName());
    	if(obj == Tab.noObj){
			report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije deklarisano! ", null);
			designator.obj=Tab.noObj;
			return;
    	}
    	if(obj.getType().getKind() != Struct.Array) {
    		report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije niz! ", null);
    		designator.obj=Tab.noObj;
    		return;
    	}
    	if(bracketDesignatorType!=null)
    		if(obj.getType().assignableTo(bracketDesignatorType)) {
    			report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije kompatibilnog tipa sa svim elementima! ", designator);
    		}
//    	if(!designator.getDesignator().obj.getType().compatibleWith(bracketDesignatorARRType) && bracketDesignatorARRType!=null) {
//    		report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije kompatibilnog tipa sa nizom levo! ", null);
//    	}
    	if(!obj.getType().getElemType().compatibleWith(designator.getDesignatorHelper().getDesignator().obj.getType().getElemType())) {
    		report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije kompatibilnog tipa sa nizom levo! ", null);
    	}
    	bracketDesignatorType=null;
    	//bracketDesignatorARRType=null;
    }

    public void visit(DesignatorHelper designator) {
    	Obj obj = Tab.find(designator.getDesignator().obj.getName());
    	if(obj == Tab.noObj){
			report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije deklarisano! ", null);
    	}
    	if(designator.getDesignator().obj.getType().getKind() != Struct.Array) {
    		report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije niz! ", null);
    	}
//    	if(bracketDesignatorARRType!=null) bracketDesignatorARRType=designator.getDesignator().obj.getType();
    }

    public void visit(DesignatorArr designator) {
    	designator.obj=designator.getDesignator().obj;
    	Obj obj = Tab.find(designator.getDesignator().obj.getName());
    	if(obj == Tab.noObj){
			report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije deklarisano! ", null);
    	}
    	if(
    			designator.getDesignator().obj.getKind()!=Obj.Var && 
    			designator.getDesignator().obj.getKind()!=Obj.Elem) 
    			{
    		report_error("Greska na liniji " + designator.getDesignator().getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije ni element niza ni promenljiva! ", null);
    	}
    	if(!designator.getDesignator().obj.getType().compatibleWith(bracketDesignatorType) && bracketDesignatorType!=null) {
    		report_error("Greska na liniji " + designator.getDesignator().getLine()+ " : ime "+designator.getDesignator().obj.getName()+" nije odgovarajuceg tipa! ", null);
    	}else {
    		bracketDesignatorType=designator.getDesignator().obj.getType();
    	}
    }
    //---------------------------------------------

    //-----------------STATEMENTS:-----------------------
    public void visit(BreakStmt stmt) {
    	if(inFor<=0) {
    		report_error("Greska na liniji " + stmt.getLine()+ " : nema okruzujuce petlje ", null);
    		return;
    	}
    }
    public void visit(ContinueStmt stmt) {
    	if(inFor<=0) {
    		report_error("Greska na liniji " + stmt.getLine()+ " : nema okruzujuce petlje ", null);
    	}
    }
    public void visit(ReadStmt stmt) {
    	Obj obj = Tab.find(stmt.getDesignator().obj.getName());
    	if(obj == Tab.noObj){
			report_error("Greska na liniji " + stmt.getLine()+ " : ime "+stmt.getDesignator().obj.getName()+" nije deklarisano! ", null);
    	}
    	if(stmt.getDesignator().obj.getKind()!=Obj.Var && stmt.getDesignator().obj.getKind()!=Obj.Elem) {
    		report_error("Greska na liniji " + stmt.getLine()+ " : ime "+stmt.getDesignator().obj.getName()+" nije ni element niza ni promenljiva! ", null);
    	}
    	if(stmt.getDesignator().obj.getType()!=Tab.charType && stmt.getDesignator().obj.getType()!=Tab.intType) {
    		report_error("Greska na liniji " + stmt.getLine()+ " : ime "+stmt.getDesignator().obj.getName()+" nije odgovarajuceg tipa ", null);
    	}
    }
    public void visit(LabelStmt stmt) {
    	//Obj obj = Tab.find(stmt.getI1());
    	//if(obj != Tab.noObj)report_error("Greska na liniji " + stmt.getLine()+ " : ime "+stmt.getI1()+" je vec deklarisano ", null);
    	//else Tab.insert(Obj.Con, stmt.getI1(), Tab.intType);
    }
    public void visit(GoToStmt stmt) {
    	
    }
    public void visit(PrintStmt stmt) {
		printCallCount++;
		Struct elem=null;
		if(stmt.getExpr().struct.getKind()==Struct.Array) {
			elem = stmt.getExpr().struct.getElemType();
		}
    	if(stmt.getExpr().struct!=Tab.charType && stmt.getExpr().struct!=Tab.intType && (elem!=null && elem!=Tab.intType && elem!= Tab.charType)) {
    		report_error("Greska na liniji " + stmt.getLine()+ " : ime "+stmt.getExpr().struct+" nije odgovarajuci tip ", null);
    	}
    }
    public void visit(PrintStmtAdv stmt) {
		printCallCount++;
		Struct elem=null;
		if(stmt.getExpr().struct.getKind()==Struct.Array) {
			elem = stmt.getExpr().struct.getElemType();
		}
    	if(stmt.getExpr().struct!=Tab.charType && stmt.getExpr().struct!=Tab.intType && (elem!=null && elem!=Tab.intType && elem!= Tab.charType)) {
    		report_error("Greska na liniji " + stmt.getLine()+ " : ime "+stmt.getExpr().struct+" nije odgovarajuci tip ", null);
    	}
    }
    public void visit(UsingStmt stmt) {
    	Obj o = Tab.find(stmt.getI1());
    	if(o.equals(Tab.noObj)) {
    		report_error("Greska na liniji " + stmt.getLine()+ " : ne postoji namspace s tim imenom ", stmt);
    		return;
    	}
    	nmspcNames.add(stmt.getI1());
    }
    public void visit(IfStmt stmt) {
    	if(stmt.getConditionHelper().struct!=Tab.find("bool").getType()) {
    		report_error("Greska na liniji " + stmt.getLine()+ " : tip uslova mora biti bool ", stmt);
    	}
    }
    public void visit(IfStmtElse stmt) {
    	if(stmt.getConditionHelper().struct!=Tab.find("bool").getType()) {
    		report_error("Greska na liniji " + stmt.getLine()+ " : tip uslova mora biti bool ", stmt);
    	}
    }
    public void visit(ConditionHelper stmt) {
    	stmt.struct=stmt.getCondition().struct;
    }
    public void visit(ConditionList stmt) {
    	Obj o=Tab.find("bool");
    	if(
    			stmt.getConditionTermHelper().struct!=Tab.intType && 
    			stmt.getConditionTermHelper().struct!=o.getType()) {
    		report_error("Greska na liniji " + stmt.getLine()+ " : tip uslova mora biti bool ", stmt);
    	}
    	stmt.struct=o.getType();
    }
    public void visit(ConditionOneTerm stmt) {
    	stmt.struct=stmt.getConditionTermHelper().struct;
    }
    public void visit(ConditionTermHelper stmt) {
    	stmt.struct=stmt.getCondTerm().struct;
    }
    public void visit(ConditionTermList stmt) {
    	Obj o=Tab.find("bool");
    	if(
    			stmt.getCondFact().struct!=Tab.intType && 
    			stmt.getCondFact().struct!=o.getType()) {
    		report_error("Greska na liniji " + stmt.getLine()+ " : tip uslova mora biti bool ", stmt);
    	}
    	stmt.struct=o.getType();
    }
    public void visit(ConditionOneFact stmt) {
    	stmt.struct=stmt.getCondFact().struct;
    }
    public void visit(OneCondFact stmt) {
    	Obj o=Tab.find("bool");
    	if(
    			stmt.getExpr().struct!=Tab.intType && 
    			stmt.getExpr().struct!=o.getType()) {
    		report_error(stmt.getExpr().struct.getKind()+"Greska na liniji " + stmt.getLine()+ " : tip uslova mora biti bool ", stmt);
    	}
    	stmt.struct=stmt.getExpr().struct;
    }
    public void visit(TwoCondFact stmt) {
    	if(!stmt.getExpr().struct.compatibleWith(stmt.getExpr1().struct)) {
    		report_error("Greska na liniji " + stmt.getLine()+ " : nekompatibilni tipovi uslova ", stmt);
    		stmt.struct = Tab.noType;
    	}
		//niz samo != ==
    	if(stmt.getExpr().struct.getKind() == Struct.Array) {
    		if(stmt.getRelOp().getClass() == EQOp.class || stmt.getRelOp().getClass() == NEQOp.class) {
    			//sve ok
    		}
    		else {
    			report_error("Greska na liniji " + stmt.getLine()+ " : nedozvoljen operator ", stmt);
    			stmt.struct = Tab.noType;
    		}
    	}
    	stmt.struct = Tab.find("bool").getType();
    	report_info(""+stmt.getRelOp().toString(), null);
    }
    public void visit(OneCondFactForFor stmt) {
    	Obj o=Tab.find("bool");
    	if(
    			stmt.getExpr().struct!=Tab.intType && 
    			stmt.getExpr().struct!=o.getType()) {
    		report_error(stmt.getExpr().struct.getKind()+"Greska na liniji " + stmt.getLine()+ " : tip uslova mora biti bool ", stmt);
    	}
    	stmt.struct=stmt.getExpr().struct;
    }
    public void visit(TwoCondFactForFor stmt) {
    	if(!stmt.getExpr().struct.compatibleWith(stmt.getExpr1().struct)) {
    		report_error("Greska na liniji " + stmt.getLine()+ " : nekompatibilni tipovi uslova ", stmt);
    		stmt.struct = Tab.noType;
    	}
		//niz samo != ==
    	if(stmt.getExpr().struct.getKind() == Struct.Array) {
    		if(stmt.getRelOp().getClass() == EQOp.class || stmt.getRelOp().getClass() == NEQOp.class) {
    			//sve ok
    		}
    		else {
    			report_error("Greska na liniji " + stmt.getLine()+ " : nedozvoljen operator ", stmt);
    			stmt.struct = Tab.noType;
    		}
    	}
    	stmt.struct = Tab.find("bool").getType();
    	report_info(""+stmt.getRelOp().toString(), null);
    }
    public void visit(ForStmt stmt) {
    	--inFor;
    }
    public void visit(ForAfterYes fa) {
    	inFor++;
    }
    public void visit(NoForAfter fa) {
    	inFor++;
    }
    //---------------------------------------------
    
    //----------------PARAMS:---------------------------
    public void visit(ActualParam ap) {
    	++currActualParams;
    	actualParamTypes.add(ap.getExpr().struct);
    }
    public void visit(ActualParams ap) {
    	++currActualParams;
    	actualParamTypes.add(ap.getExpr().struct);
    }
    //---------------------------------------------
    public boolean passed(){
    	return !errorDetected;
    }
    
}
