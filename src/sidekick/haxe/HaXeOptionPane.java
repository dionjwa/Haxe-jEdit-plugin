package sidekick.haxe;

import javax.swing.JTextField;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;

public class HaXeOptionPane extends AbstractOptionPane
{

//    private JCheckBox ckShowGutter;
//    private JCheckBox ckShowNumbers;
//    private JCheckBox ckUseCSS;
    private JTextField compilerLocation;
    private JTextField launchCommand;

    public HaXeOptionPane ()
    {
        super("haxe");
    }

   public void _init() {

       // use a style sheet rather than in-line style
//       ckUseCSS = new JCheckBox(
//           jEdit.getProperty("options.code2html.use-css"),
//           jEdit.getBooleanProperty("code2html.use-css", false));
//       addComponent(ckUseCSS);
//
//       // show the gutter as part of the output
//       ckShowGutter = new JCheckBox(
//           jEdit.getProperty("options.code2html.show-gutter"),
//           jEdit.getBooleanProperty("code2html.show-gutter", false));
//       addComponent(ckShowGutter);

       // show the line numbers as part of the output
       // TODO: doesn't showing the gutter take care of this?
       // TODO: this isn't used anywhere
       /*
       ckShowNumbers = new JCheckBox(
           jEdit.getProperty("options.code2html.show-numbers"),
           jEdit.getBooleanProperty("code2html.show-numbers", true));
       addComponent(ckShowNumbers);
       */

       // set a line wrap width, this might be necessary for printing
//       int wrap = jEdit.getIntegerProperty("code2html.wrap", 0);
//       if (wrap < 0) {
//           wrap = 0;
//       }
       compilerLocation = new JTextField(jEdit.getProperty("options.haxe.compilerLocation"));
//       tfWrap.setMinValue(0);
       addComponent(jEdit.getProperty("options.haxe.compilerLocation.label"), compilerLocation);

       launchCommand = new JTextField(jEdit.getProperty("options.haxe.launchCommand"));
//     tfWrap.setMinValue(0);
     addComponent(jEdit.getProperty("options.haxe.launchCommand.label"), launchCommand);

       // set the character to use as the gutter divider
       // TODO: this isn't used anywhere
       /*
       tfDivider = new JTextField(4);
       String divider = jEdit.getProperty("code2html.gutter-divider", ":");
       tfDivider.setText(divider);
       addComponent(
           jEdit.getProperty("options.code2html.gutter-divider"),
           tfDivider);
       */

       // Custom <pre> and <body>
       // TODO: complete this, should be able to set custom style, pre, and body
       // definitions
       /*
       customStylePanel = new JPanel(new GridLayout(2, 1));
       customStylePanel.setBorder(
           new TitledBorder(
           new EtchedBorder(EtchedBorder.LOWERED),
           jEdit.getProperty("options.code2html.custom.styles.1")));
       customStylePanel.setPreferredSize(new Dimension(500, 500));
       customStylePanel.setMinimumSize(new Dimension(300, 300));
       customStylePanel.setToolTipText(
           jEdit.getProperty("options.code2html.custom.styles.2"));

       customBODY = new JPanel(new BorderLayout());
       customStylePanel.add(customBODY);

       customPRE = new JPanel(new BorderLayout());
       customStylePanel.add(customPRE);

       customBODYHtmlValue = new JTextField(
           jEdit.getProperty("options.code2html.body.html.value"), 80);
       customBODYCssValue = new JTextArea(
           jEdit.getProperty("options.code2html.body.style.value"), 10, 80);
       customBODYCssValue.addKeyListener(
           new KeyAdapter() {// prevent jEdit stealing the ENTER strokes
               public void keyPressed(KeyEvent e) {
                   if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                       customBODYCssValue.append(
                           System.getProperty("line.separator"));
                       e.consume();
                   }
               }
           });

       customPREHtmlValue = new JTextField(
           jEdit.getProperty("options.code2html.pre.html.value"), 80);
       customPRECssValue = new JTextArea(
           jEdit.getProperty("options.code2html.pre.style.value"), 10, 80);
       customPRECssValue.addKeyListener(
           new KeyAdapter() {// prevent jEdit stealing the ENTER strokes
               public void keyPressed(KeyEvent e) {
                   if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                       customPRECssValue.append(
                           System.getProperty("line.separator"));
                       e.consume();
                   }
               }
           });

       customBODYHtml = new JPanel(new BorderLayout());
       customBODYHtml.add(
           new JLabel(jEdit.getProperty("options.code2html.body.html.open")),
           BorderLayout.WEST);
       customBODYHtml.add(customBODYHtmlValue, BorderLayout.CENTER);
       customBODYHtml.add(
           new JLabel(jEdit.getProperty("options.code2html.body.html.close")),
           BorderLayout.EAST);

       customBODYCss = new JPanel(new BorderLayout());
       customBODYCss.add(
           new JLabel(jEdit.getProperty("options.code2html.body.style.open")),
           BorderLayout.NORTH);
       customBODYCss.add(new JScrollPane(customBODYCssValue));
       customBODYCss.add(
           new JLabel(jEdit.getProperty("options.code2html.body.style.close")),
           BorderLayout.SOUTH);

       customBODY.add(customBODYHtml, BorderLayout.NORTH);
       customBODY.add(customBODYCss, BorderLayout.CENTER);

       customPREHtml = new JPanel(new BorderLayout());
       customPREHtml.add(
           new JLabel(jEdit.getProperty("options.code2html.pre.html.open")),
           BorderLayout.WEST);
       customPREHtml.add(customPREHtmlValue, BorderLayout.CENTER);
       customPREHtml.add(
           new JLabel(jEdit.getProperty("options.code2html.pre.html.close")),
           BorderLayout.EAST);

       customPRECss = new JPanel(new BorderLayout());
       customPRECss.add(
           new JLabel(jEdit.getProperty("options.code2html.pre.style.open")),
           BorderLayout.NORTH);
       customPRECss.add(new JScrollPane(customPRECssValue));
       customPRECss.add(
           new JLabel(jEdit.getProperty("options.code2html.pre.style.close")),
           BorderLayout.SOUTH);

       customPRE.add(customPREHtml, BorderLayout.NORTH);
       customPRE.add(customPRECss, BorderLayout.CENTER);

       addComponent(customStylePanel);
       */
       //Component c = get

       //getFrame().pack();
       revalidate();
   }


   /**
    *  Save he properties that have been set in the GUI
    */
   public void _save() {
//       jEdit.setBooleanProperty("code2html.use-css",
//           ckUseCSS.isSelected());
//
//       jEdit.setBooleanProperty("code2html.show-gutter",
//           ckShowGutter.isSelected());

       //jEdit.setBooleanProperty("code2html.show-numbers",
       //    ckShowNumbers.isSelected());

       jEdit.setProperty("options.haxe.compilerLocation", compilerLocation.getText());
       jEdit.setProperty("options.haxe.launchCommand", launchCommand.getText());

       //jEdit.setProperty("code2html.gutter-divider", tfDivider.getText());

       // save custom style tags values
       /* this is not complete
       jEdit.setProperty("options.code2html.body.html.value",
           customBODYHtmlValue.getText());
       jEdit.setProperty("options.code2html.body.style.value",
           customBODYCssValue.getText());
       jEdit.setProperty("options.code2html.pre.html.value",
           customPREHtmlValue.getText());
       jEdit.setProperty("options.code2html.pre.style.value",
           customPRECssValue.getText());
       */
   }

}
