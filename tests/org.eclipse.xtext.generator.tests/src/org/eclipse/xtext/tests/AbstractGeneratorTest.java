/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.tests;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import junit.framework.TestCase;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.m2t.type.emf.EmfRegistryMetaModel;
import org.eclipse.xtext.GenerateAllTestGrammars;
import org.eclipse.xtext.XtextStandaloneSetup;
import org.eclipse.xtext.parser.IElementFactory;
import org.eclipse.xtext.parser.IParser;
import org.eclipse.xtext.parsetree.CompositeNode;
import org.eclipse.xtext.parsetree.IParseTreeConstructor;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.service.ILanguageDescriptor;
import org.eclipse.xtext.service.ServiceRegistry;
import org.eclipse.xtext.util.StringInputStream;
import org.openarchitectureware.expression.ExecutionContextImpl;
import org.openarchitectureware.xtend.XtendFacade;

/**
 * @author Sven Efftinge - Initial contribution and API
 * 
 */
public abstract class AbstractGeneratorTest extends TestCase {

	static {
		XtextStandaloneSetup.doSetup();
		for (Class<?> testClass : GenerateAllTestGrammars.testclasses) {
			try {
				String standaloneSetupClassName = testClass.getName() + "StandaloneSetup";
				Class<?> standaloneSetupClass = Class.forName(standaloneSetupClassName);
				Method doSetupMethod = standaloneSetupClass.getMethod("doSetup");
				doSetupMethod.invoke(null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
	

	private ILanguageDescriptor currentLanguageDescriptor;
	
	public ILanguageDescriptor getCurrentLanguageDescriptor() {
		return currentLanguageDescriptor;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		currentLanguageDescriptor = null;
	}

	/**
	 * call this to set the language class to be used in the current test.
	 */
	protected void with(Class<?> standaloneSetup) throws Exception {
		Method getLangDescMethod = standaloneSetup.getMethod("getLanguageDescriptor");
		currentLanguageDescriptor = (ILanguageDescriptor) getLangDescMethod.invoke(null);
		assert currentLanguageDescriptor != null;
	}

	protected IParser getParser() {
		return ServiceRegistry.getService(currentLanguageDescriptor, IParser.class);
	}

	protected IElementFactory getASTFactory() throws Exception {
		return ServiceRegistry.getService(currentLanguageDescriptor, IElementFactory.class);
	}

	protected IParseTreeConstructor getParseTreeConstructor() throws Exception {
		return ServiceRegistry.getService(currentLanguageDescriptor, IParseTreeConstructor.class);
	}
	
	protected IResourceFactory getResourceFactory() throws Exception {
		return ServiceRegistry.getService(currentLanguageDescriptor, IResourceFactory.class);
	}

	// parse methods

	public EObject getModel(String model) throws Exception {
		return getModel(new org.eclipse.xtext.util.StringInputStream(model));
	}

	public EObject getModel(InputStream model) throws Exception {
		XtextResource resource = getResource(model);
		return resource.getParseResult().getRootASTElement();
	}

	protected XtextResource getResource(InputStream in) throws Exception {
		ResourceSet rs = new XtextResourceSet();
		XtextResource resource = (XtextResource) rs.createResource(URI.createURI("mytestmodel."+getResourceFactory().getModelFileExtensions()[0]));
		resource.load(in, null);
		return resource;
	}

	protected CompositeNode getRootNode(InputStream model) throws Exception {
		XtextResource resource = getResource(model);
		return resource.getParseResult().getRootNode();
	}

	protected CompositeNode getRootNode(String model2) throws Exception {
		return getRootNode(new StringInputStream(model2));
	}

	// Xtend helper methods

	protected void assertWithXtend(String left, String right, Object _this) {
		assertWithXtend(left + " != " + right, left, right, _this);
	}

	protected Object invokeWithXtend(String expression, Object _this) {
		XtendFacade f = getXtendFacade();
		f = f.cloneWithExtensions(getImportDeclarations() + "invoke(Object this) : " + expression + ";");
		return f.call("invoke", _this);
	}

	protected String[] importedExtensions() {
		return new String[0];
	}

	protected void assertWithXtend(String message, String left, String right, Object _this) {
		XtendFacade f = getXtendFacade();
		StringBuffer code = getImportDeclarations();
		code.append("__compare(Object this) : __left(this) == __right(this);__left(Object this) : " + left
				+ "; __right(Object this) :" + right + ";");
		f = f.cloneWithExtensions(code.toString());
		Boolean result = (Boolean) f.call("__compare", _this);
		if (!result) {
			Object leftResult = f.call("__left", _this);
			Object rightResult = f.call("__right", _this);
			fail(message + " was : " + leftResult + "("
					+ (leftResult != null ? leftResult.getClass().getSimpleName() : "") + ") != " + rightResult + "("
					+ (leftResult != null ? leftResult.getClass().getSimpleName() : "") + ")");
		}
	}

	private StringBuffer getImportDeclarations() {
		StringBuffer code = new StringBuffer();
		for (String _import : importedExtensions()) {
			code.append("extension ").append(_import).append(";");
		}
		return code;
	}

	protected XtendFacade getXtendFacade() {
		ExecutionContextImpl ctx = new ExecutionContextImpl();
		ctx.registerMetaModel(new EmfRegistryMetaModel());
		return XtendFacade.create(ctx);
	}

	protected String readFileIntoString(String filePath) throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream resourceAsStream = classLoader.getResourceAsStream(filePath);
		byte[] buffer = new byte[2048];
		int bytesRead = 0;
		StringBuffer b = new StringBuffer();
		do {
			bytesRead = resourceAsStream.read(buffer);
			if (bytesRead != -1)
				b.append(new String(buffer, 0, bytesRead));
		} while (bytesRead != -1);
		String model = b.toString();
		return model;
	}

}
