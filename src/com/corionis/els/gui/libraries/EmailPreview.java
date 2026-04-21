package com.corionis.els.gui.libraries;

import com.corionis.els.Context;

import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.URL;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class EmailPreview extends JDialog 
{
    private String content;
    private Context context;
    private String format;
    private int x;
    private int y;
    private int width;
    private int height;

    public EmailPreview(Window owner, Context context, String content, String format, int x, int y, int width, int height)
    {
        super(owner);
        this.context = context;
        this.content = content;
        this.format = format;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        initComponents();
        initialize();
    }

    private void actionOkClicked(ActionEvent e)
    {
        setVisible(false);
    }

    private void initialize()
    {
        // position & size
        this.setLocation(x, y);
        Dimension dim = new Dimension(width, height);
        this.setSize(dim);

        // Escape key
        ActionListener escListener = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                okButton.doClick();
            }
        };
        getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

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
                if (keyEvent.getKeyChar() == KeyEvent.VK_ENTER)
                {
                    okButton.doClick();
                }
            }
        });

        if (format.equalsIgnoreCase("html"))
            editorPane.setContentType("text/html");
        else
            editorPane.setContentType("text/plain");

        editorPane.setText(content);
        editorPane.addHyperlinkListener(new HyperLink());
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
            else if (type == HyperlinkEvent.EventType.ENTERED)
            {
                String url = hyperlinkEvent.getURL().toString();
                labelStatus.setText(url);
            }
            else if (type == HyperlinkEvent.EventType.EXITED)
            {
                labelStatus.setText("  ");
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        scrollPaneEditor = new JScrollPane();
        editorPane = new JEditorPane();
        buttonBar = new JPanel();
        labelStatus = new JLabel();
        okButton = new JButton();

        //======== this ========
        setTitle(context.cfg.gs("EmailPreview.this.title"));
        setModal(true);
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(460, 320));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout());

                //======== scrollPaneEditor ========
                {

                    //---- editorPane ----
                    editorPane.setEditable(false);
                    editorPane.setBackground(Color.white);
                    editorPane.setForeground(Color.black);
                    scrollPaneEditor.setViewportView(editorPane);
                }
                contentPanel.add(scrollPaneEditor, BorderLayout.CENTER);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};
                buttonBar.add(labelStatus, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 8, 0, 5), 0, 0));

                //---- okButton ----
                okButton.setText(context.cfg.gs("Z.ok"));
                okButton.addActionListener(e -> actionOkClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JScrollPane scrollPaneEditor;
    private JEditorPane editorPane;
    private JPanel buttonBar;
    private JLabel labelStatus;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
