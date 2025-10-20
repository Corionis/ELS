package com.corionis.els.gui.bookmarks;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class BookmarksUI extends JDialog 
{
    Bookmarks bookmarks = null;
    Context context;
    private Logger logger = LogManager.getLogger("applog");
    BookmarksTableModel model;
    Bookmarks originals = null;

    public BookmarksUI(Window owner, Context context)
    {
        super(owner);
        this.context = context;
        this.bookmarks = context.navigator.bookmarks;
        this.originals = (Bookmarks) context.navigator.bookmarks.clone();
        bookmarks.sort();

        initComponents();

        model = new BookmarksTableModel(context, bookmarks);
        bookmarksTable.setModel(model);

        if (context.preferences.getBookmarksXpos() != -1 && Utils.isOnScreen(context.preferences.getBookmarksXpos(),
                context.preferences.getBookmarksYpos()))
        {
            this.setLocation(context.preferences.getBookmarksXpos(), context.preferences.getBookmarksYpos());
            Dimension dim = new Dimension(context.preferences.getBookmarksWidth(), context.preferences.getBookmarksHeight());
            this.setSize(dim);
        }
        else
        {
            this.setLocation(Utils.getRelativePosition(context.mainFrame, this));
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

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                BookmarksUI.this.windowClosing(e);
            }
        });

        setWidths();
        model.fireTableDataChanged();
        context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");

        setButtons();
        setVisible(true);
        requestFocus();
    }

    private void actionCancelClicked(ActionEvent e)
    {
        bookmarks = originals;
        saveBookmarks();
        setVisible(false);
    }

    private void actionDeleteClicked(ActionEvent e)
    {
        int[] selected = bookmarksTable.getSelectedRows();
        if (selected != null && selected.length > 0)
        {
            for (int i = selected.length - 1; i >= 0; i--)
            {
                int sel = selected[i];
                bookmarks.delete(sel);
            }
            model.fireTableDataChanged();
        }
    }

    private void actionSaveClicked(ActionEvent event)
    {
        saveBookmarks();
        savePreferences();
        setVisible(false);
    }

    private void saveBookmarks()
    {
        try
        {
            bookmarks.write();
            context.navigator.loadBookmarksMenu();
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(this, msg, context.cfg.gs("BookmarksUI.this.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void savePreferences()
    {
        context.preferences.setBookmarksHeight(this.getHeight());
        context.preferences.setBookmarksWidth(this.getWidth());
        Point location = this.getLocation();
        context.preferences.setBookmarksXpos(location.x);
        context.preferences.setBookmarksYpos(location.y);
        context.preferences.setBookmarksNameWidth(bookmarksTable.getColumnModel().getColumn(1).getWidth());
        context.preferences.setBookmarksPathWidth(bookmarksTable.getColumnModel().getColumn(2).getWidth());
    }

    public void setButtons()
    {
        int count = bookmarks.size();
        boolean enable = count > 0 ? true : false;
        deleteButton.setEnabled(enable);
    }

    private void setWidths()
    {
        bookmarksTable.getColumnModel().getColumn(0).setPreferredWidth(42);
        bookmarksTable.getColumnModel().getColumn(0).setWidth(42);
        bookmarksTable.getColumnModel().getColumn(0).setMaxWidth(42);
        bookmarksTable.getColumnModel().getColumn(0).setMinWidth(42);
        bookmarksTable.getColumnModel().getColumn(0).setResizable(false);

        bookmarksTable.getColumnModel().getColumn(1).setPreferredWidth(context.preferences.getBookmarksNameWidth());
        bookmarksTable.getColumnModel().getColumn(1).setWidth(context.preferences.getBookmarksNameWidth());

        bookmarksTable.getColumnModel().getColumn(2).setPreferredWidth(context.preferences.getBookmarksPathWidth());
        bookmarksTable.getColumnModel().getColumn(2).setWidth(context.preferences.getBookmarksPathWidth());
    }

    private void windowClosing(WindowEvent e)
    {
        cancelButton.doClick();
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
        scrollPane = new JScrollPane();
        bookmarksTable = new JTable();
        buttonBar = new JPanel();
        leftPanel = new JPanel();
        deleteButton = new JButton();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(context.cfg.gs("BookmarksUI.this.title"));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setName("dialogBookmarks");
        setMinimumSize(new Dimension(150, 126));
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout());

                //======== scrollPane ========
                {

                    //---- bookmarksTable ----
                    bookmarksTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
                    bookmarksTable.setFillsViewportHeight(true);
                    bookmarksTable.setAutoCreateRowSorter(true);
                    bookmarksTable.setShowVerticalLines(false);
                    bookmarksTable.setShowHorizontalLines(false);
                    scrollPane.setViewportView(bookmarksTable);
                }
                contentPanel.add(scrollPane, BorderLayout.CENTER);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //======== leftPanel ========
                {
                    leftPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

                    //---- deleteButton ----
                    deleteButton.setText(context.cfg.gs("BookmarksUI.deleteButton.text"));
                    deleteButton.setToolTipText(context.cfg.gs("Navigator.menuTbDelete.toolTipText"));
                    deleteButton.setMnemonic('D');
                    deleteButton.addActionListener(e -> actionDeleteClicked(e));
                    leftPanel.add(deleteButton);
                }
                buttonBar.add(leftPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- okButton ----
                okButton.setText(context.cfg.gs("Z.save"));
                okButton.setToolTipText(context.cfg.gs("Z.save.toolTip.text"));
                okButton.setMnemonic('S');
                okButton.addActionListener(e -> actionSaveClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setToolTipText(context.cfg.gs("Z.cancel.changes.toolTipText"));
                cancelButton.setMnemonic('L');
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
    private JScrollPane scrollPane;
    private JTable bookmarksTable;
    private JPanel buttonBar;
    private JPanel leftPanel;
    private JButton deleteButton;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on

    //
    // @formatter:on
    // </editor-fold>
}
