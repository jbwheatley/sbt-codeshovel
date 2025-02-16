package com.felixgrund.codeshovel.parser.impl;

import com.felixgrund.codeshovel.entities.Yexceptions;
import com.felixgrund.codeshovel.entities.Ymodifiers;
import com.felixgrund.codeshovel.entities.Yparameter;
import com.felixgrund.codeshovel.parser.AbstractFunction;
import com.felixgrund.codeshovel.parser.Yfunction;
import com.felixgrund.codeshovel.util.Utl;
import com.felixgrund.codeshovel.wrappers.Commit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaFunction extends AbstractFunction<MethodDeclaration> implements Yfunction {

	public JavaFunction(MethodDeclaration method, Commit commit, String sourceFilePath, String sourceFileContent) {
		super(method, commit, sourceFilePath, sourceFileContent);
	}

	private Map<String,String> createParameterMetadataMap(Parameter parameterElement) {
		Map<String, String> metadata = new HashMap<>();
		String modifiersString = createParameterModifiersString(parameterElement);
		if (modifiersString != null) {
			metadata.put("modifiers", modifiersString);
		}
		String annotationsString = createParameterAnnotationsString(parameterElement);
		if (annotationsString != null) {
			metadata.put("annotations", annotationsString);
		}
		return metadata;
	}

	private String createParameterModifiersString(Parameter parameterElement) {
		String ret = null;
		List<String> modifiers = new ArrayList<String>();
		for (Modifier modifier : parameterElement.getModifiers()) {
			modifiers.add(modifier.getKeyword().asString());
		}
		if (modifiers.size() > 0) {
			ret = StringUtils.join(modifiers, "-");
		}
		return ret;
	}

	private String createParameterAnnotationsString(Parameter parameterElement) {
		String ret = null;
		List<String> annotations = new ArrayList<String>();
		for (Node node : parameterElement.getAnnotations()) {
			annotations.add(node.toString());
		}
		if (annotations.size() > 0) {
			ret = StringUtils.join(annotations, "-");
		}
		return ret;
	}

	@Override
	protected String getInitialName(MethodDeclaration method) {
		return method.getNameAsString();
	}

	@Override
	protected String getInitialType(MethodDeclaration method) {
		return method.getTypeAsString();
	}

	@Override
	protected Ymodifiers getInitialModifiers(MethodDeclaration method) {
		List<String> modifiers = new ArrayList<>();
		for (Modifier modifier : method.getModifiers()) {
			modifiers.add(modifier.getKeyword().asString());
		}
		return new Ymodifiers(modifiers);
	}

	@Override
	protected Yexceptions getInitialExceptions(MethodDeclaration method) {
		List<String> exceptions = new ArrayList<>();
		for (ReferenceType type : method.getThrownExceptions()) {
			exceptions.add(type.asString());
		}
		return new Yexceptions(exceptions);
	}

	@Override
	protected List<Yparameter> getInitialParameters(MethodDeclaration method) {
		List<Yparameter> parametersList = new ArrayList<>();
		List<Parameter> parameterElements = method.getParameters();
		for (Parameter parameterElement : parameterElements) {
			Yparameter parameter = new Yparameter(parameterElement.getNameAsString(), parameterElement.getTypeAsString());
			Map<String, String> metadata = createParameterMetadataMap(parameterElement);
			parameter.setMetadata(metadata);
			parametersList.add(parameter);
		}
		return parametersList;
	}

	@Override
	protected String getInitialBody(MethodDeclaration method) {
		String body = null;
		if (method.getBody().isPresent()) {
			body = method.getBody().get().toString(new PrettyPrinterConfiguration().setPrintComments(true));
		}
		return body;
	}

	@Override
	protected int getInitialBeginLine(MethodDeclaration method) {
		int beginLine = 0;
		if (method.getName().getBegin().isPresent()) {
			beginLine = method.getName().getBegin().get().line;
		}
		return beginLine;
	}

	@Override
	protected int getInitialEndLine(MethodDeclaration method) {
		int endLine = 0;
		if (method.getEnd().isPresent()) {
			endLine = method.getEnd().get().line;
		}
		return endLine;
	}

	@Override
	protected String getInitialParentName(MethodDeclaration method) {
		String parentName = null;
		if (method.getParentNode().isPresent()) {
			Node node = method.getParentNode().get();
			if (node instanceof NodeWithName) {
				parentName = ((NodeWithName) method.getParentNode().get()).getNameAsString();
			} else if (node instanceof NodeWithSimpleName) {
				parentName = ((NodeWithSimpleName) method.getParentNode().get()).getNameAsString();
			}
		}
		return parentName;
	}

	@Override
	protected String getInitialFunctionPath(MethodDeclaration method) {
		return null;
	}

	@Override
	protected String getInitialId(MethodDeclaration method) {
		String ident = getName();
		if (isNestedMethod(method)) {
			ident = "$" + ident;
		}
		String idParameterString = super.getIdParameterString();
		if (StringUtils.isNotBlank(idParameterString)) {
			ident += "___" + idParameterString;
		}
		return Utl.sanitizeFunctionId(ident);
	}

	@Override
	protected String getInitialSourceFragment(MethodDeclaration method) {
		return method.toString(new PrettyPrinterConfiguration().setPrintComments(false));
	}

	/**
	 * @return all the annotation of a method
	 * Source Code Example:
	 * public void foo() {}
	 * This will return "@Override,@Test" since foo has two annotations @Test and @Override
	 * */
	@Override
	protected String getInitialAnnotation(MethodDeclaration method) {
		List<String> annotationsList = new ArrayList<>();
		for(AnnotationExpr annotation: method.getAnnotations()) {
			annotationsList.add(annotation.toString());
		}
		return StringUtils.join(annotationsList, ",");
	}

	private boolean isNestedMethod(MethodDeclaration method) {
		if (method.getParentNode().isPresent()) {
			Node parentNode = method.getParentNode().get();
			if (parentNode.getParentNode().isPresent()) {
				Node grandParentNode = parentNode.getParentNode().get();
				return grandParentNode instanceof NodeWithSimpleName;
			}
		}
		return false;
	}

	@Override
	protected String getInitialDoc(MethodDeclaration method) {
		return method.hasJavaDocComment() ? method.getJavadoc().get().toText() : "" ;
	}

	@Override
	protected String getInitialUnformattedBody(MethodDeclaration method) {
		if (method.getBody().isPresent()) {
			LexicalPreservingPrinter.setup(method);
			return LexicalPreservingPrinter.print(method.getBody().get());
		} else {
			return null;
		}
	}
}
