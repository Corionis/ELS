package com.groksoft.els.gui.tools.junkremover;

import java.awt.event.*;
import javax.swing.event.*;

import com.google.gson.Gson;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.gui.browser.NavTreeNode;
import com.groksoft.els.gui.browser.NavTreeUserObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;

public class JunkRemoverUI extends JDialog
{
    private transient Logger logger = LogManager.getLogger("applog");
    private ArrayList<JunkRemoverTool> deletedTools;
    private GuiContext guiContext;
    private NavHelp helpDialog;
    private DefaultListModel<JunkRemoverTool> listModel;
    private boolean somethingChanged = false;

    public JunkRemoverUI(Window owner, GuiContext guiContext)
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

        listModel = new DefaultListModel<JunkRemoverTool>();
        listItems.setModel(listModel);
        adjustTable();
        loadConfigurations();
        deletedTools = new ArrayList<JunkRemoverTool>();
    }

    private void addRowClicked(ActionEvent e)
    {
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }
        int row = tableJunk.getRowCount();
        int col = 0; //tableJunk.getColumnCount();
        if (((JunkTableModel) tableJunk.getModel()).getTool() != null)
        {
            ((JunkTableModel) tableJunk.getModel()).getTool().addJunkItem();
            ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();
            tableJunk.requestFocus();
            tableJunk.changeSelection(row, col, false, false);
            tableJunk.editCellAt(row, col);
            if (tableJunk.getEditorComponent() != null)
                tableJunk.getEditorComponent().requestFocus();
        }
    }

    private void adjustTable()
    {
        tableJunk.setModel(new JunkTableModel(guiContext.cfg));
        tableJunk.getTableHeader().setReorderingAllowed(false);

        // junk pattern column
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        TableColumn column = tableJunk.getColumnModel().getColumn(0);
        cellRenderer.setHorizontalAlignment(JLabel.LEFT);
        column.setMinWidth(32);
        column.setCellRenderer(cellRenderer);
        column.setResizable(true);

        // case-sensitive column
        column = tableJunk.getColumnModel().getColumn(1);
        column.setResizable(false);
        column.setWidth(62);
        column.setPreferredWidth(62);
        column.setMaxWidth(62);
        column.setMinWidth(62);

        // tab to move to next row & add new row if needed
        tableJunk.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "Action.NextCell");
        tableJunk.getActionMap().put("Action.NextCell", new NextCellAction());
    }

    private void cancelClicked(ActionEvent e)
    {
        checkForChanges();
        if (somethingChanged)
        {
            int reply = JOptionPane.showConfirmDialog(guiContext.form, guiContext.cfg.gs("JunkRemover.cancel.changes"),
                    guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
                setVisible(false);
        }
        else
            setVisible(false);
    }

    public boolean checkForChanges()
    {
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }

        if (deletedTools.size() > 0)
            somethingChanged = true;

        for (int i = 0; i < listModel.getSize(); ++i)
        {
            if (((JunkRemoverTool) listModel.getElementAt(i)).dataHasChanged)
            {
                somethingChanged = true;
                break;
            }
        }
        return somethingChanged;
    }

    private void copyClicked(ActionEvent e)
    {
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }
        int index = listItems.getSelectedIndex();
        if (index >= 0)
        {
            JunkRemoverTool origJrt = listModel.getElementAt(index);
            JunkRemoverTool jrt = origJrt.clone();
            jrt.setConfigName(guiContext.cfg.gs("JunkRemover.untitled"));
            jrt.setDataHasChanged(true);
            jrt.addJunkItem();
            textFieldName.setText(jrt.getConfigName());

            listModel.addElement(jrt);
            listItems.setSelectedIndex(listModel.getSize() - 1);

            ((JunkTableModel) tableJunk.getModel()).setTool(null);
            tableJunk.removeAll();
            ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();

            ((JunkTableModel) tableJunk.getModel()).setTool(jrt);
            ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();

            textFieldName.setEnabled(true);
            textFieldName.selectAll();
            textFieldName.requestFocus();
        }
    }

    private void deleteClicked(ActionEvent e)
    {
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }
        int index = listItems.getSelectedIndex();
        if (index >= 0)
        {
            JunkRemoverTool jrt = listModel.getElementAt(index);
            int reply = JOptionPane.showConfirmDialog(guiContext.form, guiContext.cfg.gs("JunkRemover.are.you.sure.you.want.to.delete.configuration") + jrt.getConfigName(),
                    guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                deletedTools.add(jrt);
                listModel.removeElementAt(index);
                loadTable(-1);
            }
        }
    }

    private void doneClicked(ActionEvent e)
    {
        saveConfigurations();
        setVisible(false);
    }

    private void labelHelpMouseClicked(MouseEvent e) {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, this, guiContext, guiContext.cfg.gs("JunkRemover.help"), "junkremover_" + guiContext.preferences.getLocale() + ".html");
        }
        if (!helpDialog.isVisible())
        {
            helpDialog.setVisible(true);
            // offset the help dialog from the Settings dialog
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

    private boolean listItemExists(JunkRemoverTool jrt, String configName)
    {
        boolean exists = false;
        for (int i = 0; i < listModel.getSize(); ++i)
        {
            if (listModel.getElementAt(i).getConfigName().equalsIgnoreCase(configName))
            {
                if (jrt == null || jrt != listModel.getElementAt(i))
                {
                    exists = true;
                    break;
                }
            }
        }
        return exists;
    }

    private void listItemsMouseClicked(MouseEvent e)
    {
        JList src = (JList) e.getSource();
        if (e.getClickCount() == 1)
        {
            if (tableJunk.isEditing())
            {
                tableJunk.getCellEditor().stopCellEditing();
            }

            int index = src.locationToIndex(e.getPoint());
            loadTable(index);
        }
    }

    private void listItemsValueChanged(ListSelectionEvent e)
    {
        if (!e.getValueIsAdjusting())
        {
            if (tableJunk.isEditing())
            {
                tableJunk.getCellEditor().stopCellEditing();
            }

            int index = listItems.getSelectedIndex();
            loadTable(index);
        }
    }

    private void loadConfigurations()
    {
        JunkRemoverTool tmpTool = new JunkRemoverTool(guiContext.cfg, guiContext.context);
        File toolDir = new File(tmpTool.getDirectoryPath());
        if (toolDir.exists())
        {
            File[] files = FileSystemView.getFileSystemView().getFiles(toolDir, false);
            Arrays.sort(files);
            for (File entry : files)
            {
                if (!entry.isDirectory())
                {
                    try
                    {
                        Gson gson = new Gson();
                        String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                        if (json != null)
                        {
                            JunkRemoverTool jrt = gson.fromJson(json, JunkRemoverTool.class);
                            if (jrt != null)
                            {
                                jrt.setGuiContext(guiContext);
                                listModel.addElement(jrt);
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        // file might not exist
                    }
                }
            }
        }
        if (listModel.getSize() == 0)
        {
            buttonCopy.setEnabled(false);
            buttonDelete.setEnabled(false);
            buttonRun.setEnabled(false);
            buttonAddRow.setEnabled(false);
            buttonRemoveRow.setEnabled(false);
            textFieldName.setEnabled(false);
        }
        else
        {
            loadTable(0);
            listItems.requestFocus();
            listItems.setSelectedIndex(0);
        }
    }

    private void loadTable(int index)
    {
        if (index >= 0 && index < listModel.getSize())
        {
            JunkRemoverTool jrt = (JunkRemoverTool) listModel.getElementAt(index);
            JunkTableModel model = (JunkTableModel) tableJunk.getModel();
            model.setTool(jrt);
            model.fireTableDataChanged();
            textFieldName.setText(jrt.getConfigName());
        }
        else
        {
            JunkTableModel model = (JunkTableModel) tableJunk.getModel();
            for (int i = model.getRowCount() - 1; i >= 0; --i)
            {
                model.removeRow(i);
            }
            model.setTool(null);
            model.fireTableDataChanged();
            textFieldName.setText("");
        }
    }

    private void newClicked(ActionEvent e)
    {
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }
        JunkRemoverTool jrt = new JunkRemoverTool(guiContext);
        jrt.setConfigName(guiContext.cfg.gs("JunkRemover.untitled"));
        jrt.setDataHasChanged(true);
        jrt.addJunkItem();
        textFieldName.setText(jrt.getConfigName());

        listModel.addElement(jrt);
        listItems.setSelectedIndex(listModel.getSize() - 1);

        ((JunkTableModel) tableJunk.getModel()).setTool(null);
        tableJunk.removeAll();
        ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();

        ((JunkTableModel) tableJunk.getModel()).setTool(jrt);
        ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();

        if (listModel.getSize() > 0)
        {
            buttonCopy.setEnabled(true);
            buttonDelete.setEnabled(true);
            buttonRun.setEnabled(true);
            buttonAddRow.setEnabled(true);
            buttonRemoveRow.setEnabled(true);
        }

        textFieldName.setEnabled(true);
        textFieldName.selectAll();
        textFieldName.requestFocus();
    }

    private void removeRowClicked(ActionEvent e)
    {
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }
        int rows[] = tableJunk.getSelectedRows();
        Arrays.sort(rows);
        // iterate in reverse so the order does not change as items are removed
        for (int i = rows.length - 1; i >= 0; --i)
        {
            ((JunkTableModel) tableJunk.getModel()).removeRow(rows[i]);
        }
        ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();
        tableJunk.requestFocus();
    }

    private void runClicked(ActionEvent e)
    {
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }
if (0 == 1)
{
    int index = listItems.getSelectedIndex();
    if (index >= 0)
    {
        JunkRemoverTool jrt = listModel.getElementAt(index);

        // process what was selected last
        Object object = guiContext.browser.lastComponent;
        if (object instanceof JTree)
        {
            JTree sourceTree = (JTree) object;
            int row = sourceTree.getLeadSelectionRow();
            if (row > -1)
            {
                boolean isRemote = false;
                TreePath[] paths = sourceTree.getSelectionPaths();
                for (TreePath path : paths)
                {
                    NavTreeNode ntn = (NavTreeNode) path.getLastPathComponent();
                    NavTreeUserObject tuo = ntn.getUserObject();
                }
            }
        }
        else if (object instanceof JTable)
        {
            JTable sourceTable = (JTable) object;
            int row = sourceTable.getSelectedRow();
            if (row > -1)
            {
                boolean isRemote = false;
                int[] rows = sourceTable.getSelectedRows();
                for (int i = 0; i < rows.length; ++i)
                {
                    NavTreeUserObject tuo = (NavTreeUserObject) sourceTable.getValueAt(rows[i], 1);
                }
            }

        }
    }
}
    }

    private void saveConfigurations()
    {
        JunkRemoverTool jrt = null;
        try
        {
            // write/update changed tool JSON configuration files
            for (int i = 0; i < listModel.getSize(); ++i)
            {
                jrt = listModel.getElementAt(i);
                if (jrt.isDataChanged())
                    jrt.write();
            }

            // remove any deleted tools JSON configuration file
            for (int i = 0; i < deletedTools.size(); ++i)
            {
                jrt = deletedTools.get(i);
                File file = new File(jrt.getFullPath());
                if (file.exists())
                {
                    file.delete();
                }
            }
        }
        catch (Exception e)
        {
            String name = (jrt != null) ? jrt.getConfigName() + " " : " ";
            guiContext.browser.printLog(Utils.getStackTrace(e), true);
            JOptionPane.showMessageDialog(guiContext.form,
                    guiContext.cfg.gs("JunkRemover.error.saving.configuration") + name + e.getMessage(),
                    guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void textFieldNameChanged(ActionEvent e)
    {
        updateListName();
    }

    private void textFieldNameFocusLost(FocusEvent e)
    {
        updateListName();
    }

    private void updateListName()
    {
        int index = listItems.getSelectedIndex();
        JunkRemoverTool jrt = (JunkRemoverTool) listModel.getElementAt(index);
        if (jrt != null)
        {
            if (listItemExists(jrt, textFieldName.getText()))
            {
                JOptionPane.showMessageDialog(guiContext.form,
                        guiContext.cfg.gs(("JunkRemover.that.configuration.already.exists")),
                        guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                textFieldName.selectAll();
                textFieldName.requestFocus();
            }
            else
            {
                jrt.setConfigName(textFieldName.getText());
                jrt.setDataHasChanged(true);
                listModel.setElementAt(jrt, index);

                tableJunk.requestFocus();
                tableJunk.changeSelection(0, 0, false, false);
                tableJunk.editCellAt(0, 0);
                tableJunk.getEditorComponent().requestFocus();
            }
        }
    }

    // ================================================================================================================

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off
    //
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        panelTop = new JPanel();
        panelTopButtons = new JPanel();
        buttonNew = new JButton();
        buttonCopy = new JButton();
        buttonDelete = new JButton();
        hSpacerBeforeRun = new JPanel(null);
        buttonRun = new JButton();
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        splitPaneContent = new JSplitPane();
        scrollPaneList = new JScrollPane();
        listItems = new JList();
        panelOptions = new JPanel();
        textFieldName = new JTextField();
        scrollPaneOptions = new JScrollPane();
        tableJunk = new JTable();
        panelOptionsButtons = new JPanel();
        buttonAddRow = new JButton();
        buttonRemoveRow = new JButton();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(guiContext.cfg.gs("JunkRemover.title"));
        setName("junkRemoverUI");
        setMinimumSize(new Dimension(150, 126));
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(570, 470));
            dialogPane.setMinimumSize(new Dimension(150, 80));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setPreferredSize(new Dimension(570, 470));
                contentPanel.setMinimumSize(new Dimension(140, 120));
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

                        //---- buttonNew ----
                        buttonNew.setText(guiContext.cfg.gs("JunkRemover.button.New.text"));
                        buttonNew.setMnemonic(guiContext.cfg.gs("JunkRemover.button.New.mnemonic").charAt(0));
                        buttonNew.setToolTipText(guiContext.cfg.gs("JunkRemover.button.New.toolTipText"));
                        buttonNew.addActionListener(e -> newClicked(e));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(guiContext.cfg.gs("JunkRemover.button.Copy.text"));
                        buttonCopy.setMnemonic(guiContext.cfg.gs("JunkRemover.button.Copy.mnemonic").charAt(0));
                        buttonCopy.setToolTipText(guiContext.cfg.gs("JunkRemover.button.Copy.toolTipText"));
                        buttonCopy.addActionListener(e -> copyClicked(e));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(guiContext.cfg.gs("JunkRemover.button.Delete.text"));
                        buttonDelete.setMnemonic(guiContext.cfg.gs("JunkRemover.button.Delete.mnemonic").charAt(0));
                        buttonDelete.setToolTipText(guiContext.cfg.gs("JunkRemover.button.Delete.toolTipText"));
                        buttonDelete.addActionListener(e -> deleteClicked(e));
                        panelTopButtons.add(buttonDelete);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonRun ----
                        buttonRun.setText(guiContext.cfg.gs("JunkRemover.button.Run.text"));
                        buttonRun.setMnemonic(guiContext.cfg.gs("JunkRemover.button.Run.mnemonic").charAt(0));
                        buttonRun.setToolTipText(guiContext.cfg.gs("JunkRemover.button.Run.toolTipText"));
                        buttonRun.addActionListener(e -> runClicked(e));
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
                        labelHelp.setToolTipText(guiContext.cfg.gs("JunkRemover.labelHelp.toolTipText"));
                        labelHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        labelHelp.setIconTextGap(0);
                        labelHelp.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                labelHelpMouseClicked(e);
                            }
                        });
                        panelHelp.add(labelHelp);
                    }
                    panelTop.add(panelHelp, BorderLayout.EAST);
                }
                contentPanel.add(panelTop, BorderLayout.NORTH);

                //======== splitPaneContent ========
                {
                    splitPaneContent.setDividerLocation(142);
                    splitPaneContent.setLastDividerLocation(142);
                    splitPaneContent.setMinimumSize(new Dimension(140, 80));

                    //======== scrollPaneList ========
                    {
                        scrollPaneList.setMinimumSize(new Dimension(140, 16));
                        scrollPaneList.setPreferredSize(new Dimension(142, 146));

                        //---- listItems ----
                        listItems.setPreferredSize(new Dimension(128, 54));
                        listItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        listItems.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                listItemsMouseClicked(e);
                            }
                        });
                        listItems.addListSelectionListener(e -> listItemsValueChanged(e));
                        scrollPaneList.setViewportView(listItems);
                    }
                    splitPaneContent.setLeftComponent(scrollPaneList);

                    //======== panelOptions ========
                    {
                        panelOptions.setMinimumSize(new Dimension(0, 78));
                        panelOptions.setLayout(new BorderLayout());

                        //---- textFieldName ----
                        textFieldName.setPreferredSize(new Dimension(150, 30));
                        textFieldName.addActionListener(e -> textFieldNameChanged(e));
                        textFieldName.addFocusListener(new FocusAdapter() {
                            @Override
                            public void focusLost(FocusEvent e) {
                                textFieldNameFocusLost(e);
                            }
                        });
                        panelOptions.add(textFieldName, BorderLayout.NORTH);

                        //======== scrollPaneOptions ========
                        {

                            //---- tableJunk ----
                            tableJunk.setShowVerticalLines(false);
                            tableJunk.setName("tableJunk");
                            tableJunk.setAutoCreateRowSorter(true);
                            tableJunk.setCellSelectionEnabled(true);
                            scrollPaneOptions.setViewportView(tableJunk);
                        }
                        panelOptions.add(scrollPaneOptions, BorderLayout.CENTER);

                        //======== panelOptionsButtons ========
                        {
                            panelOptionsButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                            //---- buttonAddRow ----
                            buttonAddRow.setText(guiContext.cfg.gs("JunkRemover.button.AddRow.text"));
                            buttonAddRow.setFont(buttonAddRow.getFont().deriveFont(buttonAddRow.getFont().getSize() - 2f));
                            buttonAddRow.setPreferredSize(new Dimension(78, 24));
                            buttonAddRow.setMinimumSize(new Dimension(78, 24));
                            buttonAddRow.setMaximumSize(new Dimension(78, 24));
                            buttonAddRow.setMnemonic(guiContext.cfg.gs("JunkRemover.button.AddRow.mnemonic").charAt(0));
                            buttonAddRow.setToolTipText(guiContext.cfg.gs("JunkRemover.button.AddRow.toolTipText"));
                            buttonAddRow.addActionListener(e -> addRowClicked(e));
                            panelOptionsButtons.add(buttonAddRow);

                            //---- buttonRemoveRow ----
                            buttonRemoveRow.setText(guiContext.cfg.gs("JunkRemover.button.RemoveRow.text"));
                            buttonRemoveRow.setFont(buttonRemoveRow.getFont().deriveFont(buttonRemoveRow.getFont().getSize() - 2f));
                            buttonRemoveRow.setPreferredSize(new Dimension(78, 24));
                            buttonRemoveRow.setMinimumSize(new Dimension(78, 24));
                            buttonRemoveRow.setMaximumSize(new Dimension(78, 24));
                            buttonRemoveRow.setMnemonic(guiContext.cfg.gs("JunkRemover.button.RemoveRow.mnemonic").charAt(0));
                            buttonRemoveRow.setToolTipText(guiContext.cfg.gs("JunkRemover.button.RemoveRow.toolTipText"));
                            buttonRemoveRow.addActionListener(e -> removeRowClicked(e));
                            panelOptionsButtons.add(buttonRemoveRow);
                        }
                        panelOptions.add(panelOptionsButtons, BorderLayout.SOUTH);
                    }
                    splitPaneContent.setRightComponent(panelOptions);
                }
                contentPanel.add(splitPaneContent, BorderLayout.CENTER);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 82, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText(guiContext.cfg.gs("JunkRemover.button.Ok.text"));
                okButton.setMnemonic(guiContext.cfg.gs("JunkRemover.button.Ok.mnemonic").charAt(0));
                okButton.addActionListener(e -> doneClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 2), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(guiContext.cfg.gs("JunkRemover.button.Cancel.text"));
                cancelButton.addActionListener(e -> cancelClicked(e));
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

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JPanel panelTop;
    private JPanel panelTopButtons;
    private JButton buttonNew;
    private JButton buttonCopy;
    private JButton buttonDelete;
    private JPanel hSpacerBeforeRun;
    private JButton buttonRun;
    private JPanel panelHelp;
    private JLabel labelHelp;
    private JSplitPane splitPaneContent;
    private JScrollPane scrollPaneList;
    private JList listItems;
    private JPanel panelOptions;
    private JTextField textFieldName;
    private JScrollPane scrollPaneOptions;
    private JTable tableJunk;
    private JPanel panelOptionsButtons;
    private JButton buttonAddRow;
    private JButton buttonRemoveRow;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

    // ================================================================================================================

    private class NextCellAction extends AbstractAction
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            int row = tableJunk.getSelectedRow();
            int col = tableJunk.getSelectedColumn();
            int rowCount = tableJunk.getRowCount();
            int colCount = tableJunk.getColumnCount();
            ++col;
            if (col >= colCount)
            {
                col = 0;
                ++row;
            }
            if (row >= rowCount)
            {
                ((JunkTableModel) tableJunk.getModel()).getTool().addJunkItem();
                ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();
            }
            tableJunk.changeSelection(row, col, false, false);
            tableJunk.editCellAt(row, col);
            tableJunk.getEditorComponent().requestFocus();
        }
    }

}
