package com.corionis.els.gui;

import java.awt.event.*;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.common.util.io.IoUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * ELS Help dialog
 */
public class NavHelp extends JDialog
{
    private transient Logger logger = LogManager.getLogger("applog");
    private Context context;
    public boolean fault = false;
    private Component previous;

    // hide constructor
    private NavHelp()
    {
    }

    /**
     * Display help or information
     *
     * @param owner Owner of this dialog
     * @param prev Previous focused component
     * @param context The Context
     * @param title Title for dialog
     * @param resourceFilename Internal resource filename or Internet URL
     */
    public NavHelp(Window owner, Component prev, Context context, String title, String resourceFilename, boolean modal)
    {
        super(owner);
        previous = prev;
        this.context = context;

        initComponents();
        this.setTitle(title);

        if (!resourceFilename.startsWith("gettingstarted_"))
            showCheckBox.setVisible(false);
        else
            showCheckBox.setSelected(context.preferences.isShowGettingStarted());

        okButton.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                savePreferences();
                setVisible(false);
                if (previous == null)
                    previous = context.mainFrame;
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

        if (context.preferences.getHelpXpos() != -1 && Utils.isOnScreen(context.preferences.getHelpXpos(), context.preferences.getHelpYpos()))
        {
            setLocation(context.preferences.getHelpXpos(), context.preferences.getHelpYpos());
            if (context.preferences.getHelpWidth() > 0)
            {
                setSize(context.preferences.getHelpWidth(), context.preferences.getHelpHeight());
            }

        }
        else
        {
            setSize(600, 550);
            Point position = owner.getLocation();
            position.x += 32;
            position.y += 32;
            setLocation(position);
        }

        load(resourceFilename);

        setModal(modal);
        setVisible(true);
        buttonFocus();
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
            if (resourceFilename.startsWith("http"))
            {
                URL url = new URL(resourceFilename);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
                String buf;
                while ((buf = bufferedReader.readLine()) != null)
                {
                    text += buf.trim() + "\n";
                }
                bufferedReader.close();
            }
            else
            {
                URL url = Thread.currentThread().getContextClassLoader().getResource(resourceFilename);
                List<String> lines = IoUtils.readAllLines(url);
                for (int i = 0; i < lines.size(); ++i)
                {
                    text += lines.get(i) + "\n";
                }
            }

            helpText.setText(text);
            helpText.addHyperlinkListener(new HyperLink());

            // scroll to the top
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    JScrollBar bar = scrollPane.getVerticalScrollBar();
                    bar.setValue(bar.getMinimum());
                    buttonFocus();
                }
            });
        }
        catch (Exception e)
        {
            fault = true;
            logger.error(Utils.getStackTrace(e));
            JOptionPane.showMessageDialog(this.getOwner(), context.cfg.gs("NavHelp.error.opening.help.file") + resourceFilename + ", " + e.getMessage(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void savePreferences()
    {
        context.preferences.setShowGettingStarted(showCheckBox.isSelected());
        context.preferences.setHelpHeight(this.getHeight());
        context.preferences.setHelpWidth(this.getWidth());
        Point location = this.getLocation();
        context.preferences.setHelpXpos(location.x);
        context.preferences.setHelpYpos(location.y);
    }

    private void thisWindowClosed(WindowEvent e)
    {
        if (previous == null)
            previous = context.mainFrame;
        previous.requestFocus();
    }

    private void thisWindowClosing(WindowEvent e)
    {
        if (previous == null)
            previous = context.mainFrame;
        previous.requestFocus();
    }

    public class HyperLink implements HyperlinkListener
    {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent)
        {
            HyperlinkEvent.EventType type = hyperlinkEvent.getEventType();
            if (type == HyperlinkEvent.EventType.ACTIVATED)
            {
                try
                {
                    URL url = hyperlinkEvent.getURL();
                    URI uri = url.toURI();
                    Desktop.getDesktop().browse(uri);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.error.launching.browser"), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    // <editor-fold desc="Generated">
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
        showCheckBox = new JCheckBox();
        okButton = new JButton();

        //======== this ========
        setName(context.cfg.gs("NavHelp.name"));
        setMinimumSize(new Dimension(100, 50));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                thisWindowClosed(e);
            }
            @Override
            public void windowClosing(WindowEvent e) {
                thisWindowClosing(e);
            }
        });
        var contentPane = getContentPane();
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

                //---- showCheckBox ----
                showCheckBox.setText(context.cfg.gs("NavHelp.showCheckBox.text"));
                buttonBar.add(showCheckBox, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- okButton ----
                okButton.setText(context.cfg.gs("Z.ok"));
                okButton.setActionCommand(context.cfg.gs("Z.ok"));
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
    private JCheckBox showCheckBox;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

    // </editor-fold>

}
