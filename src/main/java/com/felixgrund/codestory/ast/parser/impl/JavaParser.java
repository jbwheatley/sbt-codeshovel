package com.felixgrund.codestory.ast.parser.impl;import com.felixgrund.codestory.ast.entities.Yfunction;import com.felixgrund.codestory.ast.entities.Yparameter;import com.felixgrund.codestory.ast.entities.Yreturn;import com.felixgrund.codestory.ast.exceptions.ParseException;import com.felixgrund.codestory.ast.parser.AbstractParser;import com.felixgrund.codestory.ast.parser.Yparser;import com.github.javaparser.ast.CompilationUnit;import com.github.javaparser.ast.body.MethodDeclaration;import com.github.javaparser.ast.body.Parameter;import com.github.javaparser.ast.visitor.VoidVisitorAdapter;import java.util.ArrayList;import java.util.List;public class JavaParser extends AbstractParser implements Yparser {	private CompilationUnit rootCompilationUnit;	public JavaParser(String fileName, String fileContent) {		super(fileName, fileContent);	}	@Override	public Yfunction findFunctionByNameAndLine(String name, int line) {		Yfunction ret = null;		MethodDeclaration method = findMethod(new MethodVisitor() {			@Override			public boolean methodMatches(MethodDeclaration method) {				String methodName = method.getNameAsString();				int methodLineNumber = method.getName().getBegin().get().line; // TODO get() ?				return name.equals(methodName) && line == methodLineNumber;			}		});		if (method != null) {			ret = new Yfunction(name, getMethodBody(method), getMethodParameters(method), getMethodReturn(method), line);		}		return ret;	}	@Override	public List<Yfunction> findFunctionsByOtherFunction(Yfunction otherMethod) {		List<Yfunction> functions = new ArrayList<>();		String methodNameOther = otherMethod.getName();		List<Yparameter> parametersOther = otherMethod.getParameters();		List<MethodDeclaration> matchedMethods = findAllMethods(new MethodVisitor() {			@Override			public boolean methodMatches(MethodDeclaration method) {				String methodNameThis = method.getNameAsString();				List<Yparameter> parametersThis = getMethodParameters(method);				boolean methodNameMatches = methodNameOther.equals(methodNameThis);				boolean parametersMatch = parametersOther.equals(parametersThis);				return methodNameMatches && parametersMatch;			}		});		for (MethodDeclaration method : matchedMethods) {			String name = method.getName().toString();			String body = getMethodBody(method);			int methodNameLineNumber = method.getName().getBegin().get().line;			List<Yparameter> parameters = getMethodParameters(method);			Yreturn yreturn = getMethodReturn(method);			functions.add(new Yfunction(name, body, parameters, yreturn, methodNameLineNumber));		}		return functions;	}	@Override	public void parse() throws ParseException {		this.rootCompilationUnit = com.github.javaparser.JavaParser.parse(this.fileContent);		if (this.rootCompilationUnit == null) {			throw new ParseException("Could not parse root compilation unit", this.fileName, this.fileContent);		}	}	private Yreturn getMethodReturn(MethodDeclaration method) {		return new Yreturn(method.getTypeAsString());	}	private String getMethodBody(MethodDeclaration method) {		return method.getBody().get().toString(); // TODO get() ?	}	private List<Yparameter> getMethodParameters(MethodDeclaration method) {		List<Yparameter> parameters = new ArrayList<>();		List<Parameter> parameterElements = method.getParameters();		for (Parameter parameterElement : parameterElements) {			Yparameter parameter = new Yparameter(parameterElement.getNameAsString(), parameterElement.getTypeAsString());			parameters.add(parameter);		}		return parameters;	}	private MethodDeclaration findMethod(MethodVisitor visitor) {		MethodDeclaration ret = null;		List<MethodDeclaration> matchedNodes = findAllMethods(visitor);		if (matchedNodes.size() > 0) {			ret = matchedNodes.get(0);		}		return ret;	}	private List<MethodDeclaration> findAllMethods(MethodVisitor visitor) {		this.rootCompilationUnit.accept(visitor, null);		return visitor.getMatchedNodes();	}	private abstract class MethodVisitor extends VoidVisitorAdapter<Void> {		private List<MethodDeclaration> matchedNodes = new ArrayList<>();		public abstract boolean methodMatches(MethodDeclaration method);		@Override		public void visit(MethodDeclaration method, Void arg) {			super.visit(method, arg);			if (methodMatches(method)) {				matchedNodes.add(method);			}		}		public List<MethodDeclaration> getMatchedNodes() {			return matchedNodes;		}	}}