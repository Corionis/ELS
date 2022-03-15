package com.groksoft.els.gui.tools.junkremover;

import java.awt.event.*;
import java.beans.*;

import com.google.gson.Gson;
import com.groksoft.els.gui.GuiContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

public class JunkRemoverUI extends JDialog
{
    private transient Logger logger = LogManager.getLogger("applog");
    private GuiContext guiContext;
    DefaultListModel<JunkRemoverTool> listModel;

    public JunkRemoverUI(Window owner, GuiContext guiContext) {
        super(owner);
        this.guiContext = guiContext;

        initComponents();
        loadConfigurations();
        adjustTableColumns();

        listModel = new DefaultListModel<JunkRemoverTool>();
        //listItems = new JList<JunkRemoverTool>(listModel);
        listItems.setModel(listModel);

    }

    private void adjustTableColumns()
    {
        //tableJunk.setName("tableJunk");
        //tableJunk.setAutoCreateRowSorter(true);
        //tableJunk.setShowGrid(false);
        tableJunk.getTableHeader().setReorderingAllowed(false);
        //tableJunk.setRowSelectionAllowed(true);
        //tableJunk.setColumnSelectionAllowed(false);

        tableJunk.setModel(new JunkTableModel(guiContext.cfg));

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        TableColumn column = tableJunk.getColumnModel().getColumn(0);
        cellRenderer.setHorizontalAlignment(JLabel.LEFT);
        column.setCellRenderer(cellRenderer);
        column.setResizable(true);

        column = tableJunk.getColumnModel().getColumn(1);
        column.setResizable(false);
        column.setWidth(62);
        column.setPreferredWidth(62);
        column.setMaxWidth(62);
        column.setMinWidth(62);
    }

    private void doneClicked(ActionEvent e) {
        setVisible(false);
    }

    private void cancelClicked(ActionEvent e) {
        setVisible(false);
    }

    private void loadConfigurations()
    {
        JunkRemoverTool tmpTool = new JunkRemoverTool(guiContext.cfg, guiContext.context);
        File toolDir = new File(tmpTool.getToolPath());
        if (toolDir.exists())
        {
            //tools = new ArrayList<JunkRemoverTool>();
            File[] files = FileSystemView.getFileSystemView().getFiles(toolDir, false);
            for (File entry : files)
            {
                if (!entry.isDirectory())
                {
                    try
                    {
                        String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                        Gson gson = new Gson();
                        JunkRemoverTool jrt = gson.fromJson(json, JunkRemoverTool.class);
                        if (jrt != null)
                        {
                            listModel.addElement(jrt);
                        }
                    }
                    catch (IOException e)
                    {
                        // file might not exist
                    }
                }
            }
        }
    }

    private void thisWindowOpened(WindowEvent e) {
        // TODO add your code here
    }

    private void newClicked(ActionEvent e)
    {
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }
        JunkRemoverTool jrt = new JunkRemoverTool(guiContext);
        jrt.setConfigName("Untitled");
        jrt.newJunkItem();
        textFieldName.setText(jrt.getConfigName());

        listModel.addElement(jrt);
        listItems.setSelectedIndex(listModel.getSize() - 1);

        ((JunkTableModel) tableJunk.getModel()).setTool(null);
        tableJunk.removeAll();
        ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();

        ((JunkTableModel) tableJunk.getModel()).setTool(jrt);
        ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();

        textFieldName.selectAll();
        textFieldName.requestFocus();
    }

    private void listItemsMouseClicked(MouseEvent e) {
        JList src = (JList) e.getSource();
        if (e.getClickCount() == 1)
        {
            if (tableJunk.isEditing())
            {
                tableJunk.getCellEditor().stopCellEditing();
            }

            int index = src.locationToIndex(e.getPoint());
            if (index >= 0 && index < listModel.getSize())
            {
                JunkRemoverTool jrt = (JunkRemoverTool) src.getModel().getElementAt(index);
                ((JunkTableModel) tableJunk.getModel()).setTool(jrt);
                ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();
                textFieldName.setText(jrt.getConfigName());
            }
        }
    }

    private void textFieldNameChanged(ActionEvent e) {
        updateListName();
    }

    private void updateListName()
    {
        int index = listItems.getSelectedIndex();
        JunkRemoverTool jrt = (JunkRemoverTool) listModel.getElementAt(index);
        if (jrt != null)
        {
            jrt.setConfigName(textFieldName.getText());
            listModel.setElementAt(jrt, index);

            tableJunk.requestFocus();
            tableJunk.changeSelection(0, 0, false, false);
            tableJunk.editCellAt(0, 0);
            tableJunk.getEditorComponent().requestFocus();
        }
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        panelTop = new JPanel();
        panelTopButtons = new JPanel();
        buttonNew = new JButton();
        buttonCopy = new JButton();
        buttonDelete = new JButton();
        textFieldName = new JTextField();
        separatorSections = new JSeparator();
        splitPaneContent = new JSplitPane();
        scrollPaneList = new JScrollPane();
        listItems = new JList();
        panelOptions = new JPanel();
        scrollPaneOptions = new JScrollPane();
        tableJunk = new JTable();
        panelOptionsButtons = new JPanel();
        buttonNewRow = new JButton();
        buttonDeleteRow2 = new JButton();
        buttonBar = new JPanel();
        doneButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(guiContext.cfg.gs("JunkRemover.this.title"));
        setName("junkRemoverUI");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setPreferredSize(new Dimension(640, 500));
                contentPanel.setLayout(new BorderLayout());

                //======== panelTop ========
                {
                    panelTop.setLayout(new BorderLayout());

                    //======== panelTopButtons ========
                    {
                        panelTopButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                        //---- buttonNew ----
                        buttonNew.setText(guiContext.cfg.gs("JunkRemover.buttonNew.text"));
                        buttonNew.addActionListener(e -> newClicked(e));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(guiContext.cfg.gs("JunkRemover.buttonCopy.text"));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(guiContext.cfg.gs("JunkRemover.buttonDelete.text"));
                        panelTopButtons.add(buttonDelete);
                    }
                    panelTop.add(panelTopButtons, BorderLayout.LINE_START);

                    //---- textFieldName ----
                    textFieldName.setPreferredSize(new Dimension(280, 30));
                    textFieldName.addActionListener(e -> textFieldNameChanged(e));
                    panelTop.add(textFieldName, BorderLayout.CENTER);

                    //---- separatorSections ----
                    separatorSections.setPreferredSize(new Dimension(0, 3));
                    separatorSections.setMinimumSize(new Dimension(0, 3));
                    panelTop.add(separatorSections, BorderLayout.SOUTH);
                }
                contentPanel.add(panelTop, BorderLayout.NORTH);

                //======== splitPaneContent ========
                {
                    splitPaneContent.setDividerLocation(142);
                    splitPaneContent.setLastDividerLocation(142);

                    //======== scrollPaneList ========
                    {

                        //---- listItems ----
                        listItems.setPreferredSize(new Dimension(128, 54));
                        listItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        listItems.setFocusable(false);
                        listItems.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                listItemsMouseClicked(e);
                            }
                        });
                        scrollPaneList.setViewportView(listItems);
                    }
                    splitPaneContent.setLeftComponent(scrollPaneList);

                    //======== panelOptions ========
                    {
                        panelOptions.setLayout(new BorderLayout());

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
                            panelOptionsButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));

                            //---- buttonNewRow ----
                            buttonNewRow.setText(guiContext.cfg.gs("JunkRemover.buttonNewRow.text"));
                            buttonNewRow.setFont(buttonNewRow.getFont().deriveFont(buttonNewRow.getFont().getSize() - 2f));
                            buttonNewRow.setPreferredSize(new Dimension(68, 24));
                            buttonNewRow.setMinimumSize(new Dimension(68, 24));
                            buttonNewRow.setMaximumSize(new Dimension(68, 24));
                            panelOptionsButtons.add(buttonNewRow);

                            //---- buttonDeleteRow2 ----
                            buttonDeleteRow2.setText(guiContext.cfg.gs("JunkRemover.buttonDeleteRow2.text"));
                            buttonDeleteRow2.setFont(buttonDeleteRow2.getFont().deriveFont(buttonDeleteRow2.getFont().getSize() - 2f));
                            buttonDeleteRow2.setPreferredSize(new Dimension(68, 24));
                            buttonDeleteRow2.setMinimumSize(new Dimension(68, 24));
                            buttonDeleteRow2.setMaximumSize(new Dimension(68, 24));
                            panelOptionsButtons.add(buttonDeleteRow2);
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
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- doneButton ----
                doneButton.setText(guiContext.cfg.gs("JunkRemover.doneButton.text"));
                doneButton.addActionListener(e -> doneClicked(e));
                buttonBar.add(doneButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(guiContext.cfg.gs("JunkRemover.cancelButton.text"));
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
    private JTextField textFieldName;
    private JSeparator separatorSections;
    private JSplitPane splitPaneContent;
    private JScrollPane scrollPaneList;
    private JList listItems;
    private JPanel panelOptions;
    private JScrollPane scrollPaneOptions;
    private JTable tableJunk;
    private JPanel panelOptionsButtons;
    private JButton buttonNewRow;
    private JButton buttonDeleteRow2;
    private JPanel buttonBar;
    private JButton doneButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
