package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.CounterVisitor.FormParamCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class CodeGenerator extends VisitorAdaptor {

	private int mainPc;
	private boolean errorDetected;
	private String currNamespace="";
	private ArrayList<Integer> thencs, elsecs, condfacts, conds, condfactsForSKIP, condfactsForGO, jumpFA, jumpFC, continueFixup;
	private ArrayList<Integer> breakFixup, breakFixupLevelInFor;
	private int inFor=0;
	private int iter=0, weirdArrCnt=0;
	private int trapAdr, nVars;
	private ArrayList<Obj> weirdArr;
	private HashMap<String, Integer> nmspc_func_hm = new HashMap<String, Integer>();
	private HashMap<String, Integer> gotolabels = new HashMap<String, Integer>();
	private HashMap<String, ArrayList<Integer>> unpairedGoTos = new HashMap<String, ArrayList<Integer>>();
	Logger log = Logger.getLogger(getClass());
	
	public int getMainPc(){
		return mainPc;
	}
	
	//====================START & BUILT-IN METHS:=========================
	public CodeGenerator(int nv) {
		errorDetected=false;
		nVars=nv;
		Obj o=Tab.find("len");
		o.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(o.getLevel());
		Code.put(o.getLocalSymbols().size());
		Code.put(Code.load_n);
		Code.put(Code.arraylength);
		Code.put(Code.exit);
		Code.put(Code.return_);
		o=Tab.find("ord");
		basic_f_oper(o);
		o=Tab.find("chr");
		basic_f_oper(o);
		elsecs=new ArrayList<Integer>();
		thencs=new ArrayList<Integer>();
		condfacts=new ArrayList<Integer>();
		conds=new ArrayList<Integer>();
		condfactsForSKIP=new ArrayList<Integer>();
		condfactsForGO=new ArrayList<Integer>();
		jumpFA=new ArrayList<Integer>();
		jumpFC=new ArrayList<Integer>();
		continueFixup=new ArrayList<Integer>();
		breakFixup=new ArrayList<Integer>();
		breakFixupLevelInFor=new ArrayList<Integer>();
		weirdArr=new ArrayList<Obj>();
	}
	private void basic_f_oper(Obj o) {
		o.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(o.getLevel());
		Code.put(o.getLocalSymbols().size());
		Code.put(Code.load_n);
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	//=================================================================
	
	//=========================REPORTS:================================
	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder("*CODEGEN* "+message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.error(msg.toString());
	}
	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder("*CODEGEN* "+message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}
	//=====================================================
	
	//=====================VISITS:=========================
	public void visit(ProgName p) {
		Code.putJump(0);
		int adr=Code.pc-2;
		trapAdr=Code.pc;
		Code.put(Code.trap);
		Code.loadConst(1);
		Code.fixup(adr);
	}
	public void visit(PrintStmt printStmt){//G
//		if(printStmt.getExpr().struct == Tab.intType){
//			Code.loadConst(5);
//			Code.put(Code.print);
//		}else{
//			Code.loadConst(1);
//			Code.put(Code.bprint);
//		}
		if(printStmt.getExpr().struct == Tab.intType){
			Code.loadConst(5);
			Code.put(Code.print);
		}else if(printStmt.getExpr().struct == Tab.charType){
			Code.loadConst(1);
			Code.put(Code.bprint);
		}else {
			if(printStmt.getExpr().struct.getElemType()==Tab.intType || printStmt.getExpr().struct.getElemType()==Tab.charType) {//estack: adr
				int cnt=0;
				Code.loadConst(cnt); Code.put(Code.putstatic); Code.put2(nVars-1);//postavimo brojac na 0
				int jumpBack=Code.pc;//za iteriranje, ESTACK: adr
				Code.put(Code.dup);//adr adr
				Code.put(Code.arraylength);//adr n
				Code.put(Code.getstatic); Code.put2(nVars-1);//adr n cnt
				Code.putFalseJump(Code.ne, 0);//jeq dalje, estek: adr
				int jmpToEnd = Code.pc-2;
				Code.put(Code.dup);//adr adr
				Code.put(Code.getstatic); Code.put2(nVars-1);//adr adr cnt
//				Code.put(Code.aload);//adr val
				Code.put(printStmt.getExpr().struct.getElemType()==Tab.intType ? Code.aload : Code.baload);//adr val
				Code.loadConst(printStmt.getExpr().struct.getElemType()==Tab.intType ? 5 : 1);//adr val 5/1
				Code.put(printStmt.getExpr().struct.getElemType()==Tab.intType ? Code.print : Code.bprint);//adr val 5/1 (ostavlja samo adr)
				Code.put(Code.getstatic); Code.put2(nVars-1);//adr cnt
				Code.loadConst(1);//adr cnt 1
				Code.put(Code.add);//adr cnt
				Code.put(Code.putstatic); Code.put2(nVars-1);//adr (treba za sledecu iter a treba i da ostane na kraju)
				Code.putJump(jumpBack);
				Code.fixup(jmpToEnd);
				Code.put(Code.pop);//da sklonimo adr
			}
		}
	}
	public void visit(PrintStmtAdv printStmt){//G
		if(printStmt.getExpr().struct == Tab.intType){
			Code.loadConst(printStmt.getN2());
			Code.put(Code.print);
		}else if(printStmt.getExpr().struct == Tab.charType){
			Code.loadConst(printStmt.getN2());
			Code.put(Code.bprint);
		}
		else {
			if(printStmt.getExpr().struct.getElemType()==Tab.intType || printStmt.getExpr().struct.getElemType()==Tab.charType) {//estack: adr
				int cnt=0;
				Code.loadConst(cnt); Code.put(Code.putstatic); Code.put2(nVars-1);//postavimo brojac na 0
				int jumpBack=Code.pc;//za iteriranje, ESTACK: adr
				Code.put(Code.dup);//adr adr
				Code.put(Code.arraylength);//adr n
				Code.put(Code.getstatic); Code.put2(nVars-1);//adr n cnt
				Code.putFalseJump(Code.ne, 0);//jeq dalje, estek: adr
				int jmpToEnd = Code.pc-2;
				Code.put(Code.dup);//adr adr
				Code.put(Code.getstatic); Code.put2(nVars-1);//adr adr cnt
				Code.put(Code.aload);//adr val
				Code.loadConst(printStmt.getN2());//adr val n2
				Code.put(printStmt.getExpr().struct.getElemType()==Tab.intType ? Code.print : Code.bprint);//adr val n2 (ostavlja samo adr)
				Code.put(Code.getstatic); Code.put2(nVars-1);//adr cnt
				Code.loadConst(1);//adr cnt 1
				Code.put(Code.add);//adr cnt
				Code.put(Code.putstatic); Code.put2(nVars-1);//adr (treba za sledecu iter a treba i da ostane na kraju)
				Code.putJump(jumpBack);
				Code.fixup(jmpToEnd);
				Code.put(Code.pop);//da sklonimo adr
			}
		}
	}
	public void visit(ReadStmt readStmt){//SHBG
		Obj o = readStmt.getDesignator().obj;
//		Code.put(Code.pop);//!!!!!!!!!!!----ILI IZBACITI IZ DSGNTR----!!!!!!!!!!!!!!!!!
		if(o.getType() != Tab.charType){
			Code.put(Code.read);
		}else{
			Code.put(Code.bread);
		}
		Code.store(o);
	}
	public void visit(LabelStmt stmt) {
    	gotolabels.put(stmt.getI1(), Code.pc);
    	if(unpairedGoTos.containsKey(stmt.getI1())) {
    		ArrayList<Integer> arrL=unpairedGoTos.get(stmt.getI1());
    		arrL.forEach(a->{
    			Code.fixup(a);
    		});
    	}
    }
    public void visit(GoToStmt stmt) {
    	if(gotolabels.containsKey(stmt.getI1())) {
    		Code.putJump(gotolabels.get(stmt.getI1()));
    	}
    	else {
    		Code.putJump(0);
    		if(unpairedGoTos.containsKey(stmt.getI1())) {//vec je neko trazio ovu labelu
    			unpairedGoTos.get(stmt.getI1()).add(Code.pc-2);
    		}
    		else{
    			ArrayList<Integer> patches = new ArrayList<Integer>();
    			patches.add(Code.pc-2);
        		unpairedGoTos.put(stmt.getI1(), patches);
    		}
    	}
    }
	//-----------CONSTS-------------------
//	public void visit(Const cnst){
//		Obj con = Tab.insert(Obj.Con, "$", cnst.struct);
//		con.setLevel(0);
//		con.setAdr(cnst.getN1());
//		
//		Code.load(con);
//	}
	public void visit(ConstTermOneNum cnst){//SHBG
//		Obj con = Tab.find(cnst.getName());
//		if(con!=Tab.noObj)
//			Code.load(con);
//		report_info("Konstanta je "+cnst.getN(), cnst);
		Code.loadConst(cnst.getN());
	}
	public void visit(ConstTermOneChar cnst){//SHBG
//		Obj con = Tab.find(cnst.getName());
//		if(con!=null)
//			Code.load(con);
//		report_info("Konstanta je "+cnst.getC(), cnst);
		Code.loadConst(cnst.getC());
	}
	public void visit(ConstTermOneBool cnst){//SHBG
//		Obj con = Tab.find(cnst.getName());
//		if(con!=null)
//			Code.load(con);
//		report_info("Konstanta je "+cnst.getB(), cnst);
		Code.loadConst(cnst.getB());
	}
	public void visit(ConstTermListDeclNum cnst){//SHBG
//		Obj con = Tab.find(cnst.getName());
//		if(con!=null)
//			Code.load(con);
		Code.loadConst(cnst.getN());
	}
	public void visit(ConstTermListDeclChar cnst){//SHBG
//		Obj con = Tab.find(cnst.getName());
//		if(con!=null)
//			Code.load(con);
		Code.loadConst(cnst.getC());
	}
	public void visit(ConstTermListDeclBool cnst){//SHBG
//		Obj con = Tab.find(cnst.getName());
//		if(con!=null)
//			Code.load(con);
		Code.loadConst(cnst.getB());
	}
	//-----------------------------------------
	
	//--------------NAMESPACES:----------------
	public void visit(NamespaceName nmspcName) {
    	currNamespace = nmspcName.getNsName();
    }
    public void visit(Namespace nmspc) {
    	currNamespace="";
    }
	//----------------------------------------------
	
	//------------------METHODS:--------------------
	public void visit(VoidMethodTypeName methodTypeName){//SHBG
		if("main".equalsIgnoreCase(methodTypeName.getMethName())){
			mainPc = Code.pc;
		}
		methodTypeName.obj.setAdr(Code.pc);
		if(currNamespace!="") {
			nmspc_func_hm.put(methodTypeName.getMethName(), Code.pc);
		}
		SyntaxNode methodNode = methodTypeName.getParent();
		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);
		FormParamCounter fpCnt = new FormParamCounter();
		methodNode.traverseTopDown(fpCnt);
		// Generate the entry
		Code.put(Code.enter);
		Code.put(fpCnt.getCount());
		Code.put(fpCnt.getCount() + varCnt.getCount());
//		report_info("METHOD DECL: "+methodTypeName.getMethName()+" ADR: "+methodTypeName.obj.getAdr(), methodTypeName);
		//ALT:
		//Code.put(fpCnt.getCount()); just level
		//Code.put(fpCnt.getCount() + varCnt.getCount()); just locals cnt
	
	}
	public void visit(MethodTypeNameAny methodTypeName){//SHBG
//		if("main".equalsIgnoreCase(methodTypeName.getMethName())){
//			mainPc = Code.pc;
//		}
		if(currNamespace!="") {
			nmspc_func_hm.put(methodTypeName.getMethName(), Code.pc);
		}
		methodTypeName.obj.setAdr(Code.pc);
		SyntaxNode methodNode = methodTypeName.getParent();
		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);
		FormParamCounter fpCnt = new FormParamCounter();
		methodNode.traverseTopDown(fpCnt);
		
		// Generate the entry
		Code.put(Code.enter);
		Code.put(fpCnt.getCount());
		Code.put(fpCnt.getCount() + varCnt.getCount());
//		report_info("METHOD DECL: "+methodTypeName.getMethName()+" ADR: "+methodTypeName.obj.getAdr(), methodTypeName);
	}
	public void visit(MethodDecl methodDecl){//G
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	//---------------------------------------------
	
	
	//---------------FACTORCONST:------------------
	public void visit(FactorNumConst fc) {//G
		Code.loadConst(fc.getN1());
	}
	public void visit(FactorCharConst fc) {//G
		Code.loadConst(fc.getC1());
	}
	public void visit(FactorBoolConst fc) {//G
		Code.loadConst(fc.getB1());
	}
	//---------------------------------------------
	
	
	
	
	
	
	
	//-------------DESIGNATOR++:-------------------
	public void visit(DesignatorName dn) {
		SyntaxNode parent = dn.getParent().getParent();
		
		//da li i ReadStmt, DesignatorInc / Dec?OneDesignatorArr.class != parent.getClass() && 
		if(DesignatorInc.class != parent.getClass() && DesignatorDec.class != parent.getClass() &&
			DesignatorAssign.class != parent.getClass() && FactorFuncCall.class != parent.getClass() && DesignatorProc.class != parent.getClass()){
				Code.load(dn.obj);
		}
	}
	public void visit(DesignatorAssign designAssignment){//SHBG
		if(designAssignment.getDesignator() instanceof OneDesignatorArr) {//treba da bude adr idex val
			Code.put(Code.dup_x2);//val adr idex val
			Code.put(Code.pop); //val adr idex
			Code.put(Code.dup_x1); //val idex adr idex
			Code.put(Code.pop); //val idex adr
			Code.put(Code.dup); //val idex adr adr
			Code.put(Code.arraylength);//val idex adr len
			Code.put(Code.dup_x1);//val idex len adr len
			Code.put(Code.pop);//val idex len adr
			Code.put(Code.dup_x2);//val adr idex len adr
			Code.put(Code.pop);//val adr idex len
			Code.put(Code.dup_x1);//val adr len idex len
			Code.put(Code.pop);//val adr len idex
			Code.put(Code.dup);//val adr len idex idex
			Code.put(Code.dup_x2);//val adr idex len idex idex
			Code.put(Code.pop);//val adr idex len idex
			Code.putFalseJump(Code.gt, trapAdr);//val adr idex jump if len<=idex
			Code.put(Code.dup_x2);//idex val adr idex
			Code.put(Code.pop);//idex val adr
			Code.put(Code.dup_x2);//adr idex val adr
			Code.put(Code.pop);//adr idex val
		}//adr idex val treba
			Code.store(designAssignment.getDesignator().obj); //ovo treba ovako
	}
//	public void visit(DesignatorList designator){
//		SyntaxNode parent = designator.getParent();
//		
//		//da li i ReadStmt, DesignatorInc / Dec?OneDesignatorArr.class != parent.getClass() && 
//		if(DesignatorInc.class != parent.getClass() && DesignatorDec.class != parent.getClass() &&
//			DesignatorAssign.class != parent.getClass() && FactorFuncCall.class != parent.getClass() && DesignatorProc.class != parent.getClass()){
//				Code.load(designator.obj);
//		}
//	}
//	public void visit(DesignatorListNmspc designator){
//		SyntaxNode parent = designator.getParent();
//		
//		//da li i ReadStmt, DesignatorInc / Dec?
//		if(DesignatorInc.class != parent.getClass() && DesignatorDec.class != parent.getClass() &&
//			DesignatorAssign.class != parent.getClass() && FactorFuncCall.class != parent.getClass() && DesignatorProc.class != parent.getClass()){
//				Code.load(designator.obj);
//		}
//	}
	public void visit(OneDesignatorArr designator) {
		SyntaxNode parent = designator.getParent();
		if(DesignatorAssign.class == parent.getClass()) {
			
		}
		if(DesignatorArr.class != parent.getClass() && DesignatorInc.class != parent.getClass() && DesignatorDec.class != parent.getClass() &&
			DesignatorAssign.class != parent.getClass() && FactorFuncCall.class != parent.getClass() && DesignatorProc.class != parent.getClass()){
//				report_info("S** BO-BOMB!: "+designator.getDesignator().obj.getType().getKind(), designator);
				boolean isRead=false;
				while(parent.getParent()!=null) {
					if(parent.getClass()==ReadStmt.class) isRead=true;
					parent=parent.getParent();
				}//provera indeksa, adr i idex na stacku:
				Code.put(Code.dup_x1); //idex adr idex
				Code.put(Code.pop); //idex adr
				Code.put(Code.dup); //idex adr adr
				Code.put(Code.arraylength);//idex adr len
				Code.put(Code.dup_x1);//idex len adr len
				Code.put(Code.pop);//idex len adr
				Code.put(Code.dup_x2);//adr idex len adr
				Code.put(Code.pop);//adr idex len
				Code.put(Code.dup_x1);//adr len idex len
				Code.put(Code.pop);//adr len idex
				Code.put(Code.dup);//adr len idex idex
				Code.put(Code.dup_x2);//adr idex len idex idex
				Code.put(Code.pop);//adr idex len idex
				Code.putFalseJump(Code.gt, trapAdr);//adr idex jump if len<=idex
				//
				if(!isRead)//na expr stacku je adr od dsgntr i expr za idex
				{
					Code.put(designator.getDesignator().obj.getType().getElemType().getKind()==2 ? Code.baload : Code.aload);
				}
//				Code.aload(designator.obj);
		}
	}
	//E:\FAJL\PP1\PROJEKAT_J_F\workspace\MJCompiler>java -cp .;lib/mj-runtime.jar rs.etf.pp1.mj.runtime.Run -debug test/program.obj
	// ili ne -debug < ime read txt fajls
	
	
	
	
	
	
	public void visit(FactorFuncCall funcCall){//SHBG
		Obj functionObj = funcCall.getDesignator().obj;
//		report_info(functionObj.getName()+" F ADR: "+functionObj.getAdr(), funcCall);
		if(currNamespace!=""&&nmspc_func_hm.containsKey(functionObj.getName())){
			functionObj.setAdr(nmspc_func_hm.get(functionObj.getName()));
//			report_info(functionObj.getName()+" FFFFFF ADR: "+functionObj.getAdr()+",\t"+nmspc_func_hm.get(functionObj.getName()), funcCall);
		}
		int offset = functionObj.getAdr() - Code.pc;
		Code.put(Code.call);
		
		Code.put2(offset);
	}
	
	public void visit(DesignatorProc procCall){//SHBG
		Obj functionObj = procCall.getDesignator().obj;
//		report_info(functionObj.getName()+" P ADR: "+functionObj.getAdr(), procCall);
		if(currNamespace!=""&&nmspc_func_hm.containsKey(functionObj.getName())){
			functionObj.setAdr(nmspc_func_hm.get(functionObj.getName()));
//			report_info(functionObj.getName()+" PPPPPP ADR: "+functionObj.getAdr()+",\t"+nmspc_func_hm.get(functionObj.getName()), procCall);
		}
		int offset = functionObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
		if(procCall.getDesignator().obj.getType() != Tab.noType){
			Code.put(Code.pop);
		}
	}
	public void visit(DesignatorInc di){//G
		if(di.getDesignator().obj.getKind()==Obj.Elem) Code.put(Code.dup2);
		Code.load(di.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(di.getDesignator().obj);
	}
	public void visit(DesignatorDec dd){//G
		if(dd.getDesignator().obj.getKind()==Obj.Elem) Code.put(Code.dup2);
		Code.load(dd.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(dd.getDesignator().obj);
	}
	public void visit(DesignatorBrackets dd){//
//		Code.put(Code.pop);
//		Code.put(Code.pop);
		int assignCnt=-1; weirdArrCnt--;
		Code.loadConst(weirdArrCnt);Code.put(Code.putstatic); Code.put2(nVars-2);
		Code.loadConst(assignCnt);Code.put(Code.putstatic); Code.put2(nVars-1);
		//sada je i adresa desnog designatora na expr stacku ali nam treba prvo index za *dsgntr (a i provera)
		int jumpAdr=Code.pc;
		Code.put(Code.arraylength);//skida adr i stavlja length
		Code.put(Code.getstatic); Code.put2(nVars-2);//-ovo
		Code.loadConst(1);//----------------------------je
		Code.put(Code.add);//---------------------------sve
		Code.put(Code.putstatic); Code.put2(nVars-2);//-za counter
		Code.put(Code.getstatic); Code.put2(nVars-2);//-helper1 promenljivu
		Code.putFalseJump(Code.gt, 0);//jle dalje
		int daljePatch=Code.pc-2;
		Code.put(Code.dup);//trebace nam za sledecu iter
		Code.put(Code.getstatic); Code.put2(nVars-1);//-ovo
		Code.loadConst(1);//----------------------------je
		Code.put(Code.add);//---------------------------sve
		Code.put(Code.putstatic); Code.put2(nVars-1);//-za counter
		Code.put(Code.getstatic); Code.put2(nVars-1);//-helper2 promenljivu
		//sad ponovo stavljamo adr desnog dsgntr i index
		Code.load(dd.getDesignator().obj);
		Code.put(Code.getstatic); Code.put2(nVars-2);
		Code.put(Code.aload);//sad su na expr stacku adr idex i val za levi *dsgntr
		//provera perkoracenja indexa levog (kopirana iz assigna):
		Code.put(Code.dup_x2);//val adr idex val
		Code.put(Code.pop); //val adr idex
		Code.put(Code.dup_x1); //val idex adr idex
		Code.put(Code.pop); //val idex adr
		Code.put(Code.dup); //val idex adr adr
		Code.put(Code.arraylength);//val idex adr len
		Code.put(Code.dup_x1);//val idex len adr len
		Code.put(Code.pop);//val idex len adr
		Code.put(Code.dup_x2);//val adr idex len adr
		Code.put(Code.pop);//val adr idex len
		Code.put(Code.dup_x1);//val adr len idex len
		Code.put(Code.pop);//val adr len idex
		Code.put(Code.dup);//val adr len idex idex
		Code.put(Code.dup_x2);//val adr idex len idex idex
		Code.put(Code.pop);//val adr idex len idex
		Code.putFalseJump(Code.gt, trapAdr);//val adr idex jump if len<=idex
		Code.put(Code.dup_x2);//idex val adr idex
		Code.put(Code.pop);//idex val adr
		Code.put(Code.dup_x2);//adr idex val adr
		Code.put(Code.pop);//adr idex val
		//
		Code.put(Code.astore);
		Code.load(dd.getDesignator().obj);//povratak na pocetno stanje i idemo na jump nazad
		Code.putJump(jumpAdr);
		
		Code.fixup(daljePatch);
		Code.put(Code.pop);//ako smo skocili samo skinemo sa steka adr levog *dsgntr
		weirdArr=new ArrayList<Obj>();
		weirdArrCnt=0;
	}
	public void visit(DesignatorHelper dd){//
		//adresa *design je vec na expr stacku
	}
	public void visit(DesignatorArr dd){//
		weirdArr.add(dd.getDesignator().obj);
		
		SyntaxNode parentBrackets = dd.getParent();
		while(parentBrackets.getClass()!=DesignatorBrackets.class) parentBrackets=parentBrackets.getParent();
		DesignatorBrackets db = (DesignatorBrackets)parentBrackets;
		
		boolean isChar = db.getDesignator().obj.getType().getElemType()==Tab.charType;
		//pre svega ovoga na expr stacku se nalazi adr (i eventualno index) levog designatora
		Code.load(db.getDesignator().obj);//adr desnog arr
		Code.put(Code.dup);//-----------------ovo
		Code.put(Code.arraylength);//---------sve
		Code.loadConst(weirdArrCnt);//--------proverava
		Code.putFalseJump(Code.gt, trapAdr);//duzinu i radi trap ako ne valja
		Code.loadConst(weirdArrCnt);//index
		Code.put(isChar ? Code.baload : Code.aload);//load tog elementa, sad je na expr stacku samo njegov val a adr i index su skinuti
		Code.store(dd.getDesignator().obj);

		if(!(dd.getDesignator() instanceof OneDesignatorArr)) {
			Code.put(Code.pop);
		}
		++weirdArrCnt;
	}
	public void visit(CommaDesignatorArr dd){//
		weirdArr.add(Tab.noObj);
		++weirdArrCnt;
	}
	//--------------------------------------------
	
	//--------------EXPR, FACTOR ETC.-------------------------

//	public void visit(FactorVar fv) {
//		Code.load(fv.getDesignator().obj);
//	}
	public void visit(RetStmt returnExpr){//SHBG
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	public void visit(AddExpr addExpr){//G
		if(addExpr.getAddOp() instanceof AddOper) {
			Code.put(Code.add);
		}
		else if(addExpr.getAddOp() instanceof SubOper) {
			Code.put(Code.sub);
		}
	}
	public void visit(MinusTermExpr minusTerm){//G
		Code.put(Code.neg);
	}
	public void visit(TermList tl){//G
		if(tl.getMulOp() instanceof MULTOp) {
			Code.put(Code.mul);
		}
		else if(tl.getMulOp() instanceof DIVOp) {
			Code.put(Code.div);
		}
		else if(tl.getMulOp() instanceof MODOp) {
			Code.put(Code.rem);
		}
		else if(tl.getMulOp() instanceof SQOp) {
			Code.put(Code.add);//x, y -> x+y
			Code.put(Code.dup);//x+y, x+y
			Code.put(Code.mul);//x+y * x+y
		}
	}
	public void visit(NewArr na) {//SHBG
		Code.put(Code.newarray);
//		report_info(""+na.struct.getElemType().getKind(), na);
		Code.put(na.struct.getElemType()==Tab.charType ? 0 : 1);
	}
	public void visit(FactorRange fr) {
		//na esteku je broj n, tj. velicina niza
		int cnt=1;
		Code.put(Code.newarray);
		Code.put(1);//jer je int sad je na esteku adr
		Code.loadConst(cnt); Code.put(Code.putstatic); Code.put2(nVars-1);//postavimo brojac na 1
		int jumpBack=Code.pc;//za iteriranje, ESTACK: adr
		Code.put(Code.dup);//adr adr
		Code.put(Code.arraylength);//adr n
		Code.put(Code.getstatic); Code.put2(nVars-1);//adr n cnt
		Code.putFalseJump(Code.ne, 0);//jeq dalje, estek: adr
		int jmpToEnd = Code.pc-2;
		Code.put(Code.dup);//adr adr
		Code.put(Code.getstatic); Code.put2(nVars-1);//adr adr cnt
		Code.put(Code.dup);//adr adr cnt cnt (niz[cnt]:=cnt)
		Code.put(Code.astore);//adr
		Code.put(Code.getstatic); Code.put2(nVars-1);//adr cnt
		Code.loadConst(1);//adr cnt 1
		Code.put(Code.add);//adr cnt
		Code.put(Code.putstatic); Code.put2(nVars-1);//adr (treba za sledecu iter a treba i da ostane na kraju)
		Code.putJump(jumpBack);
		Code.fixup(jmpToEnd);
	}
	//----------------------------------------------------
	
	//---------------CONDITIONS & JUMPS:------------------
	private int relops(RelOp ro) {
		if(ro instanceof GTOp) {
			return Code.gt;
		}if(ro instanceof GEQOp) {
			return Code.ge;
		}if(ro instanceof LTOp) {
			return Code.lt;
		}if(ro instanceof LEQOp) {
			return Code.le;
		}if(ro instanceof EQOp) {
			return Code.eq;
		}if(ro instanceof NEQOp) {
			return Code.ne;
		}
		return -1;
	}
	
	public void visit(OneCondFact c) {
//		if(condfacts.isEmpty())
			Code.loadConst(1);
		Code.putFalseJump(Code.eq, 0);
		condfacts.add(Code.pc-2);
	}
	public void visit(TwoCondFact c) {
		Code.putFalseJump(relops(c.getRelOp()), 0);
		condfacts.add(Code.pc-2);
	}
//	public void visit(ConditionOneFact c) {
//		Code.putJump(0);
//		conds.add(Code.pc-2);
//		for(;!condfacts.isEmpty();) {
//			Code.fixup(condfacts.remove(condfacts.size()-1));
//		}
//	}
//	public void visit(ConditionTermList c) {
//		Code.putJump(0);
//		conds.add(Code.pc-2);
//		for(;!condfacts.isEmpty();) {
//			Code.fixup(condfacts.remove(condfacts.size()-1));
//		}
//	}
	public void visit(ConditionTermHelper c) {
		Code.putJump(0);
		conds.add(Code.pc-2);
		for(;!condfacts.isEmpty();) {
			Code.fixup(condfacts.remove(condfacts.size()-1));
		}
	}
//	public void visit(ConditionOneTerm c) {
//		Code.putJump(0);
//		thencs.add(Code.pc-2);
//		for(;!conds.isEmpty();) {
//			Code.fixup(conds.remove(conds.size()-1));
//		}
//	}
//	public void visit(ConditionList c) {
//		Code.putJump(0);
//		thencs.add(Code.pc-2);
//		for(;!conds.isEmpty();) {
//			Code.fixup(conds.remove(conds.size()-1));
//		}
//	}
	public void visit(ConditionHelper c) {
		Code.putJump(0);
		thencs.add(Code.pc-2);
		for(;!conds.isEmpty();) {
			Code.fixup(conds.remove(conds.size()-1));
		}
	}
	public void visit(IfStmt stmt) {
		Code.fixup(thencs.remove(thencs.size()-1));
	}
	public void visit(IfStmtElse stmt) {
		Code.fixup(elsecs.remove(elsecs.size()-1));
	}
	public void visit(ThereIsElse e) {
		Code.putJump(0);
		elsecs.add(Code.pc-2);
		Code.fixup(thencs.remove(thencs.size()-1));
	}
	//-------------------------------------------------
	
	//----------------------FOR:-------------------------
	public void visit(ForInitYes c) {
		jumpFC.add(Code.pc);
//		report_info(""+c.getClass().getName()+" jFC:"+jumpFC.get(0)+" "+iter++, c);
	}
	public void visit(NoForInit c) {
		jumpFC.add(Code.pc);
//		report_info(""+c.getClass().getName()+" "+iter++, c);
	}
	public void visit(OneCondFactForFor c) {
//		if(condfacts.isEmpty())
			Code.loadConst(1);
		Code.putFalseJump(Code.eq, 0);
		condfactsForSKIP.add(Code.pc-2);
		Code.putJump(0);
		condfactsForGO.add(Code.pc-2);
		jumpFA.add(Code.pc);
//		report_info(""+c.getClass().getName()+" "+iter++, c);
	}
	public void visit(TwoCondFactForFor c) {
		Code.putFalseJump(relops(c.getRelOp()), 0);
		condfactsForSKIP.add(Code.pc-2);
		Code.putJump(0);
		condfactsForGO.add(Code.pc-2);
		jumpFA.add(Code.pc);
//		report_info(""+c.getClass().getName()+" SKIP: "+condfactsForSKIP.get(0)+" GO:"+condfactsForGO.get(0)+" jFA:"+jumpFA.get(0)+" "+iter++, c);
	}
	public void visit(ForCondFactor c) {
		continueFixup.add(Code.pc);
//		report_info(""+c.getClass().getName()+" "+iter++, c);
	}
	public void visit(NoForCondFact c) {
		Code.putJump(0);
		condfactsForGO.add(Code.pc-2);
		jumpFA.add(Code.pc);
//		report_info(""+c.getClass().getName()+" "+iter++, c);
	}
	public void visit(ForAfterYes c) {
		if(!jumpFC.isEmpty())
			Code.putJump(jumpFC.remove(jumpFC.size()-1));
		int gopc=Code.pc;
		if(!condfactsForGO.isEmpty())
			Code.fixup(condfactsForGO.remove(condfactsForGO.size()-1));
		++inFor;
//		report_info(""+c.getClass().getName()+" fixup GO:"+gopc+" "+iter++, c);
	}
	public void visit(NoForAfter c) {
		if(!jumpFC.isEmpty())
			Code.putJump(jumpFC.remove(jumpFC.size()-1));
		if(!condfactsForGO.isEmpty())
			Code.fixup(condfactsForGO.remove(condfactsForGO.size()-1));
		++inFor;
//		report_info(""+c.getClass().getName()+" "+iter++, c);
	}
	public void visit(ForStmt stmt) {
		if(!jumpFA.isEmpty())
			Code.putJump(jumpFA.remove(jumpFA.size()-1));
		int skippc=Code.pc;
		if(!condfactsForSKIP.isEmpty())
			Code.fixup(condfactsForSKIP.remove(condfactsForSKIP.size()-1));
		if(!breakFixupLevelInFor.isEmpty()) {
			while(breakFixupLevelInFor.get(breakFixupLevelInFor.size()-1)==inFor) {
				Code.fixup(breakFixup.remove(breakFixup.size()-1));
				breakFixupLevelInFor.remove(breakFixupLevelInFor.size()-1);
				if(breakFixupLevelInFor.isEmpty()) break;
			}
		}
		--inFor;
		if(!continueFixup.isEmpty()) continueFixup.remove(continueFixup.size()-1);
//		report_info(""+stmt.getClass().getName()+" fixup SKIP:"+skippc+" "+iter++, stmt);
	}
	public void visit(ContinueStmt stmt) {
		if(!continueFixup.isEmpty())
			Code.putJump(continueFixup.get(continueFixup.size()-1));
	}
	public void visit(BreakStmt stmt) {
		Code.putJump(0);
		breakFixup.add(Code.pc-2);
		breakFixupLevelInFor.add(inFor);
	}
	//------------------------

	public void visit(Program p) {
//		report_info(" \n\n\n\n\n\n\n\n\n", null);
		
	}
	//-------------------------------------
}
