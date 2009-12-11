package net.sf.jsignpdf.gui;

import java.awt.Component;

import junit.framework.TestCase;
import net.sf.jsignpdf.StringUtils;

import com.jeta.forms.components.panel.FormPanel;

/**
 * TestCase for {@link GuiConstants} class
 * 
 * @author Josef Cacek
 */
public class GuiConstantsTest extends TestCase {

	/**
	 * Test if default form definition file exists
	 */
	public void testDefaultFormFile() {
		assertNotNull(GuiConstants.class.getClassLoader().getResourceAsStream(GuiConstants.DEFAULT_FORM_PATH));
	}

	/**
	 * Tests if COMPONENT_SET contains component names
	 */
	public void testComponentSet() {
		assertTrue(GuiConstants.COMPONENT_SET.contains(GuiConstants.Components.INPUT_HEADER));
		assertTrue(GuiConstants.COMPONENT_SET.contains(GuiConstants.Components.INPUT_PDFPATH_LABEL));
		assertTrue(GuiConstants.COMPONENT_SET.contains(GuiConstants.Components.INPUT_PDFPATH_TEXTFIELD));
		assertFalse(GuiConstants.COMPONENT_SET.contains("dummy.test.name"));
	}

	/**
	 * Test if components are present on default form
	 */
	public void testComponentsPresent() {
		FormPanel tmpPanel = new FormPanel(GuiConstants.DEFAULT_FORM_PATH);
		for (String tmpName : GuiConstants.COMPONENT_SET) {
			Component tmpComp = tmpPanel.getComponentByName(tmpName);
			assertNotNull(tmpComp);
		}

		for (Component tmpComp : tmpPanel.getComponents()) {
			String tmpName = tmpComp.getName();
			if (StringUtils.hasLength(tmpName)) {
				assertTrue(GuiConstants.COMPONENT_SET.contains(tmpName));
			}
		}
	}

}
