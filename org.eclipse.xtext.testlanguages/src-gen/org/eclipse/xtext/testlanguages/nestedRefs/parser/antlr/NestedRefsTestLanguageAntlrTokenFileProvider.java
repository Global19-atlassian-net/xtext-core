/*
 * generated by Xtext
 */
package org.eclipse.xtext.testlanguages.nestedRefs.parser.antlr;

import java.io.InputStream;
import org.eclipse.xtext.parser.antlr.IAntlrTokenFileProvider;

public class NestedRefsTestLanguageAntlrTokenFileProvider implements IAntlrTokenFileProvider {

	@Override
	public InputStream getAntlrTokenFile() {
		ClassLoader classLoader = getClass().getClassLoader();
		return classLoader.getResourceAsStream("org/eclipse/xtext/testlanguages/nestedRefs/parser/antlr/internal/InternalNestedRefsTestLanguage.tokens");
	}
}
