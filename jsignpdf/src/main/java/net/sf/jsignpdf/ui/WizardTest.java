package net.sf.jsignpdf.ui;

import com.github.cjwizard.*;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cjwizard.pagetemplates.DefaultPageTemplate;
import com.github.cjwizard.pagetemplates.TitledPageTemplate;

/**
 * This demo class uses a JDialog to hold the wizard.
 */
public class WizardTest extends JDialog {

   /**
    * Commons logging log instance
    */
   private final Logger log = LoggerFactory.getLogger(WizardTest.class);


   public WizardTest(){
      // first, build the wizard.  The TestFactory defines the
      // wizard content and behavior.
      final WizardContainer wc =
         new WizardContainer(new TestFactory(),
                             new TitledPageTemplate(),
                             new StackWizardSettings());

      //do you want to store previously visited path and repeat it if you hit back
      //and then go forward a second time?
      //this options makes sense if you have a conditional path where depending on choice of a page
      // you can visit one of two other pages.
      wc.setForgetTraversedPath(true);

      // add a wizard listener to update the dialog titles and notify the
      // surrounding application of the state of the wizard:
      wc.addWizardListener(new WizardListener(){
         @Override
         public void onCanceled(List<WizardPage> path, WizardSettings settings) {
            log.debug("settings: "+wc.getSettings());
            WizardTest.this.dispose();
         }

         @Override
         public void onFinished(List<WizardPage> path, WizardSettings settings) {
            log.debug("settings: "+wc.getSettings());
            WizardTest.this.dispose();
         }

         @Override
         public void onPageChanged(WizardPage newPage, List<WizardPage> path) {
            log.debug("settings: "+wc.getSettings());
            // Set the dialog title to match the description of the new page:
            WizardTest.this.setTitle(newPage.getDescription());
         }

        @Override
        public void onPageChanging(WizardPage newPage, List<WizardPage> path) {
            log.debug("settings: "+wc.getSettings());
        }
      });

      // Set up the standard bookkeeping stuff for a dialog, and
      // add the wizard to the JDialog:
      this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      this.getContentPane().add(wc);
      this.pack();
   }

   public static void main(String[] args) {
       new WizardTest().setVisible(true);
   }

   /**
    * Implementation of PageFactory to generate the wizard pages needed
    * for the wizard.
    */
   private class TestFactory extends AbstractPageFactory{

      // To keep things simple, we'll just create an array of wizard pages:
      private final WizardPage[] pages = {
            new WizardPage("One", "First Page"){
               // this is an instance initializer -- it's a constructor for
               // an anonymous class.  WizardPages don't need to be anonymous,
               // of course.  It just makes the demo fit in one file if we do it
               // this way:
               {
                  JTextField field = new JTextField();
                  // set a name on any component that you want to collect values
                  // from.  Be sure to do this *before* adding the component to
                  // the WizardPage.
                  field.setName("testField");
                  field.setMinimumSize(new Dimension(50, 20));
                  field.setPreferredSize(new Dimension(50, 20));
                  add(new JLabel("One!"));
                  add(field);
               }
            },
            new WizardPage("Two", "Second Page"){
               {
                  JCheckBox box = new JCheckBox("testBox");
                  box.setName("box");
                  add(new JLabel("Two!"));
                  add(box);
               }

               /* (non-Javadoc)
                * @see com.github.cjwizard.WizardPage#updateSettings(com.github.cjwizard.WizardSettings)
                */
               @Override
               public void updateSettings(WizardSettings settings) {
                  super.updateSettings(settings);

                  // This is called when the user clicks next, so we could do
                  // some longer-running processing here if we wanted to, and
                  // pop up progress bars, etc.  Once this method returns, the
                  // wizard will continue.  Beware though, this runs in the
                  // event dispatch thread (EDT), and may render the UI
                  // unresponsive if you don't issue a new thread for any long
                  // running ops.  Future versions will offer a better way of
                  // doing this.
               }

            },
            new WizardPage("Three", "Third Page"){
               {
                  add(new JLabel("Three!"));
                  setBackground(Color.green);
               }

               /**
                * This is the last page in the wizard, so we will enable the finish
                * button and disable the "Next >" button just before the page is
                * displayed:
                */
               @Override
            public void rendering(List<WizardPage> path, WizardSettings settings) {
                  super.rendering(path, settings);
                  setFinishEnabled(true);
                  setNextEnabled(false);
               }
            }
      };


      /* (non-Javadoc)
       * @see com.github.cjwizard.PageFactory#createPage(java.util.List, com.github.cjwizard.WizardSettings)
       */
      @Override
      public WizardPage createPage(List<WizardPage> path,
            WizardSettings settings) {
         log.debug("creating page "+path.size());

         // Get the next page to display.  The path is the list of all wizard
         // pages that the user has proceeded through from the start of the
         // wizard, so we can easily see which step the user is on by taking
         // the length of the path.  This makes it trivial to return the next
         // WizardPage:
         WizardPage page = pages[path.size()];

         // if we wanted to, we could use the WizardSettings object like a
         // Map<String, Object> to change the flow of the wizard pages.
         // In fact, we can do arbitrarily complex computation to determine
         // the next wizard page.

         log.debug("Returning page: "+page);
         return page;
      }

   }
}