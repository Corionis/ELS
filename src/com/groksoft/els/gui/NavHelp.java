package com.groksoft.els.gui;

import java.awt.event.*;
import com.groksoft.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.common.util.io.IoUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/**
 * ELS Help dialog
 */
public class NavHelp extends JDialog
{
    private transient Logger logger = LogManager.getLogger("applog");
    GuiContext guiContext;
    Component previous;

    public NavHelp(Window owner, Component prev, GuiContext ctxt, String title, String resourceFilename) {
        super(owner);
        previous = prev;
        guiContext = ctxt;

        initComponents();
        this.setTitle(title);

        okButton.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                setVisible(false);
                previous.requestFocus();
            }
        });

        okButton.addKeyListener(new KeyListener()
        {
            @Override
            public void keyPressed(KeyEvent keyEvent)
            {
            }

            @Override
            public void keyReleased(KeyEvent keyEvent)
            {
            }

            @Override
            public void keyTyped(KeyEvent keyEvent)
            {
                if (keyEvent.getKeyChar() == KeyEvent.VK_ENTER || keyEvent.getKeyChar() == KeyEvent.VK_ESCAPE)
                {
                    okButton.doClick();
                }
            }
        });

        load(resourceFilename);
    }

    public void buttonFocus()
    {
        okButton.requestFocus();
    }

    private void load(String resourceFilename)
    {
        String text = "";
        try
        {
            URL url = Thread.currentThread().getContextClassLoader().getResource(resourceFilename);
            List<String> lines = IoUtils.readAllLines(url);
            for (int i = 0; i < lines.size(); ++i)
            {
                text += lines.get(i) + "\n";
            }
            helpText.setText(text);

            // scroll to the top
            javax.swing.SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    JScrollBar bar = scrollPane.getVerticalScrollBar();
                    bar.setValue(bar.getMinimum());
                }
            });
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            JOptionPane.showMessageDialog(this.getOwner(), guiContext.cfg.gs("NavHelp.error.opening.help.file") + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void thisWindowActivated(WindowEvent e)
    {
        toFront();
        buttonFocus();
    }

    private void thisWindowClosed(WindowEvent e)
    {
        previous.requestFocus();
    }

    private void thisWindowClosing(WindowEvent e)
    {
        previous.requestFocus();
    }

    private void initComponents()
    {
        // <editor-fold desc="Generated component code (Fold)">
        // @formatter:off
        //
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        scrollPane = new JScrollPane();
        helpText = new JEditorPane();
        buttonBar = new JPanel();
        okButton = new JButton();

        //======== this ========
        setName(guiContext.cfg.gs("NavHelp.name"));
        setTitle(guiContext.cfg.gs("NavHelp.title"));
        setMinimumSize(new Dimension(100, 50));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                thisWindowActivated(e);
            }
            @Override
            public void windowClosed(WindowEvent e) {
                thisWindowClosed(e);
            }
            @Override
            public void windowClosing(WindowEvent e) {
                thisWindowClosing(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(570, 470));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

                //======== scrollPane ========
                {

                    //---- helpText ----
                    helpText.setEditable(false);
                    helpText.setContentType("text/html");
                    scrollPane.setViewportView(helpText);
                }
                contentPanel.add(scrollPane);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0};

                //---- okButton ----
                okButton.setText(guiContext.cfg.gs("NavHelp.button.Ok.text"));
                okButton.setActionCommand(guiContext.cfg.gs("NavHelp.button.Ok.text"));
                okButton.setMnemonic(guiContext.cfg.gs("NavHelp.button.Ok.mnemonic").charAt(0));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
        //
        // @formatter:on
        // </editor-fold>
    }

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off
    //
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    public JScrollPane scrollPane;
    private JEditorPane helpText;
    private JPanel buttonBar;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>
}
