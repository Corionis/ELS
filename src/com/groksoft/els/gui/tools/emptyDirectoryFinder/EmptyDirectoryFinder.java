package com.groksoft.els.gui.tools.emptyDirectoryFinder;

import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.tools.junkremover.JunkRemoverTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

public class EmptyDirectoryFinder extends JDialog 
{
    private boolean autoDelete = false;
    private ArrayList<JunkRemoverTool> deletedTools;
    private GuiContext guiContext;
    private Logger logger = LogManager.getLogger("applog");
    private NavHelp helpDialog;
    private SwingWorker<Void, Void> worker;
//    private JunkRemoverTool workerJrt = null;
//    private boolean workerRunning = false;

    private EmptyDirectoryFinder()
    {
        // hide default constructor
    }

    public EmptyDirectoryFinder(Window owner, GuiContext guiContext)
    {
        super(owner);
        this.guiContext = guiContext;

        initComponents();

        // scale the help icon
        Icon icon = labelHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        labelHelp.setIcon(replacement);

        // position, size & divider
        if (guiContext.preferences.getToolsEmptyDirectoryFinderXpos() > 0)
        {
            this.setLocation(guiContext.preferences.getToolsEmptyDirectoryFinderXpos(), guiContext.preferences.getToolsEmptyDirectoryFinderYpos());
            Dimension dim = new Dimension(guiContext.preferences.getToolsEmptyDirectoryFinderWidth(), guiContext.preferences.getToolsEmptyDirectoryFinderHeight());
            this.setSize(dim);
        }
        else
        {
            Point parentPos = this.getParent().getLocation();
            Dimension parentSize = this.getParent().getSize();
            Dimension mySize = this.getSize();
            Point myPos = new Point(parentPos.x + (parentSize.width / 2 - mySize.width / 2),
                    parentPos.y + (parentSize.height / 2 - mySize.height / 2));
            this.setLocation(myPos);
        }

        // Escape key
        ActionListener escListener = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                cancelButton.doClick();
            }
        };
        getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        deletedTools = new ArrayList<JunkRemoverTool>();
    }

    private void actionAutoDeleteClicked(ActionEvent e)
    {
        if (e.getActionCommand() != null)
        {
            if (e.getActionCommand().equals("autoDeleteChanged"))
            {
                autoDelete = checkBoxAutoDelete.isSelected();
            }
        }
    }

    private void actionCancelClicked(ActionEvent e)
    {
        setVisible(false);
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, this, guiContext, guiContext.cfg.gs("EmptyDirectoryFinder.help"), "emptydirectoryfinder_" + guiContext.preferences.getLocale() + ".html");
        }
        if (!helpDialog.isVisible())
        {
            helpDialog.setVisible(true);
            // offset the help dialog from the parent dialog
            Point loc = this.getLocation();
            loc.x = loc.x + 32;
            loc.y = loc.y + 32;
            helpDialog.setLocation(loc);
        }
        else
        {
            helpDialog.toFront();
        }
    }

    private void actionOkClicked(ActionEvent e)
    {
        savePreferences();
        setVisible(false);
    }

    private void actionRunClicked(ActionEvent e)
    {
    }

    private void savePreferences()
    {
        guiContext.preferences.setToolsEmptyDirectoryFinderHeight(this.getHeight());
        guiContext.preferences.setToolsEmptyDirectoryFinderWidth(this.getWidth());
        Point location = this.getLocation();
        guiContext.preferences.setToolsEmptyDirectoryFinderXpos(location.x);
        guiContext.preferences.setToolsEmptyDirectoryFinderYpos(location.y);
    }

    private void windowClosing(WindowEvent e)
    {
        cancelButton.doClick();
    }

    // ================================================================================================================

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        panelTop = new JPanel();
        panelTopButtons = new JPanel();
        checkBoxAutoDelete = new JCheckBox();
        hSpacerBeforeRun = new JPanel(null);
        buttonRun = new JButton();
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        scrollPaneEmpties = new JScrollPane();
        tableEmpties = new JTable();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(guiContext.cfg.gs("EmptyDirectoryFinder.this.title"));
        setMinimumSize(new Dimension(150, 126));
        setName("dialogEmptyDirectoryUI");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                EmptyDirectoryFinder.this.windowClosing(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout());

                //======== panelTop ========
                {
                    panelTop.setMinimumSize(new Dimension(140, 38));
                    panelTop.setPreferredSize(new Dimension(570, 38));
                    panelTop.setLayout(new BorderLayout());

                    //======== panelTopButtons ========
                    {
                        panelTopButtons.setMinimumSize(new Dimension(140, 38));
                        panelTopButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                        //---- checkBoxAutoDelete ----
                        checkBoxAutoDelete.setText(guiContext.cfg.gs("EmptyDirectoryFinder.checkBoxAutoDelete.text"));
                        checkBoxAutoDelete.setToolTipText(guiContext.cfg.gs("EmptyDirectoryFinder.checkBoxAutoDelete.toolTipText"));
                        checkBoxAutoDelete.setActionCommand("autoDeleteChanged");
                        checkBoxAutoDelete.addActionListener(e -> actionAutoDeleteClicked(e));
                        panelTopButtons.add(checkBoxAutoDelete);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonRun ----
                        buttonRun.setText(guiContext.cfg.gs("EmptyDirectoryFinder.buttonRun.text"));
                        buttonRun.setMnemonic(guiContext.cfg.gs("EmptyDirectoryFinder.buttonRun.mnemonic").charAt(0));
                        buttonRun.setToolTipText(guiContext.cfg.gs("EmptyDirectoryFinder.buttonRun.toolTipText"));
                        buttonRun.addActionListener(e -> actionRunClicked(e));
                        panelTopButtons.add(buttonRun);
                    }
                    panelTop.add(panelTopButtons, BorderLayout.WEST);

                    //======== panelHelp ========
                    {
                        panelHelp.setPreferredSize(new Dimension(40, 38));
                        panelHelp.setMinimumSize(new Dimension(0, 38));
                        panelHelp.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 4));

                        //---- labelHelp ----
                        labelHelp.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
                        labelHelp.setPreferredSize(new Dimension(32, 30));
                        labelHelp.setMinimumSize(new Dimension(32, 30));
                        labelHelp.setMaximumSize(new Dimension(32, 30));
                        labelHelp.setToolTipText(guiContext.cfg.gs("EmptyDirectoryFinder.labelHelp.toolTipText"));
                        labelHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        labelHelp.setIconTextGap(0);
                        labelHelp.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                actionHelpClicked(e);
                            }
                        });
                        panelHelp.add(labelHelp);
                    }
                    panelTop.add(panelHelp, BorderLayout.CENTER);
                }
                contentPanel.add(panelTop, BorderLayout.NORTH);

                //======== scrollPaneEmpties ========
                {

                    //---- tableEmpties ----
                    tableEmpties.setFillsViewportHeight(true);
                    tableEmpties.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    scrollPaneEmpties.setViewportView(tableEmpties);
                }
                contentPanel.add(scrollPaneEmpties, BorderLayout.CENTER);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText(guiContext.cfg.gs("EmptyDirectoryFinder.okButton.text"));
                okButton.setToolTipText(guiContext.cfg.gs("Z.save.changes.toolTipText"));
                okButton.addActionListener(e -> actionOkClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(guiContext.cfg.gs("EmptyDirectoryFinder.cancelButton.text"));
                cancelButton.setToolTipText(guiContext.cfg.gs("Z.cancel.changes.toolTipText"));
                cancelButton.addActionListener(e -> actionCancelClicked(e));
                buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
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
    private JPanel panelTop;
    private JPanel panelTopButtons;
    private JCheckBox checkBoxAutoDelete;
    private JPanel hSpacerBeforeRun;
    private JButton buttonRun;
    private JPanel panelHelp;
    private JLabel labelHelp;
    private JScrollPane scrollPaneEmpties;
    private JTable tableEmpties;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
