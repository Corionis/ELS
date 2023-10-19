package com.corionis.els.gui.libraries;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.MainFrame;
import com.corionis.els.gui.NavHelp;
import com.corionis.els.MungeException;
import com.corionis.els.gui.browser.NavTreeUserObject;
import com.corionis.els.gui.util.DirectoryPicker;
import com.corionis.els.gui.util.NumberFilter;
import com.corionis.els.gui.util.RotatedIcon;
import com.corionis.els.gui.util.TextIcon;
import com.corionis.els.jobs.Origin;
import com.corionis.els.jobs.Origins;
import com.corionis.els.repository.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableColumn;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.MessageFormat;
import java.util.*;

@SuppressWarnings(value = "unchecked")

public class LibrariesUI
{
    public JTable configItems;
    protected ArrayList<LibMeta> deletedLibraries;
    private BiblioLibrariesTableModel biblioLibrariesTableModel;
    private JComboBox comboBoxMode;
    private ConfigModel configModel;
    private Context context;
    private int currentConfigIndex = -1;
    private int currentLibraryIndex = 0;
    private int currentLocationIndex = 0;
    private int currentSourceIndex = -1;
    private DirectoryPicker directoryPicker = null;
    private String displayName;
    private NavHelp helpDialog;
    private File lastDirectory;
    private int lastTab = 0;
    private LibrarySelectorTableModel librarySelectorTableModel = null;
    private LibrarySelector[] librarySelectors = null;
    private DefaultListModel listSourcesModel = null;
    private boolean loading = false; // avoid unnecessary change events while loading data
    private LocationsTableModel locationsTableModel = null;
    private Logger logger = LogManager.getLogger("applog");
    private MainFrame mf;
    private Mode[] modes;
    private NumberFilter numberFilter;
    private boolean pickerAnyFile = false;
    private boolean pickerDirectoryOnly = false;
    private boolean pickerFileMustExist = false;
    private String promptedPath = "";

    public static enum Cards
    {Library, HintServer, Targets}

    private LibrariesUI()
    {
        // hide default constructor
    }

    public LibrariesUI(Context context)
    {
        this.context = context;
        this.mf = context.mainFrame;
        this.displayName = context.cfg.gs("Navigator.splitPane.Libraries.tab.title");
        initialize();
    }

    private void actionCopyClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            LibMeta original = (LibMeta) configModel.getValueAt(index, 0);
            String rename = original.description + context.cfg.gs("Z.copy");
            if (configModel.find(rename, null) == null)
            {
                LibMeta copy = original.clone();
                copy.description = rename;
                copy.setDataHasChanged();
                configModel.addRow(new Object[]{copy});

                currentConfigIndex = configModel.getRowCount() - 1;
                loadGeneralTab();
                configItems.editCellAt(currentConfigIndex, 0);
                configItems.changeSelection(currentConfigIndex, 0, false, false);
                configItems.getEditorComponent().requestFocus();
                ((JTextField) configItems.getEditorComponent()).selectAll();
            }
            else
            {
                JOptionPane.showMessageDialog(mf, context.cfg.gs("Z.please.rename.the.existing") +
                        rename, displayName, JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void actionDeleteClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            if (mf.librariesConfigItems.isEditing())
                mf.librariesConfigItems.getCellEditor().stopCellEditing();

            LibMeta libMeta = (LibMeta) configModel.getValueAt(index, 0);
            int reply = JOptionPane.showConfirmDialog(mf, context.cfg.gs("Z.are.you.sure.you.want.to.delete.configuration") + libMeta.description,
                    context.cfg.gs("Z.delete.configuration"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                // add to delete list if file exists
                File file = new File(libMeta.path);
                if (file.exists())
                {
                    deletedLibraries.add(libMeta);
                }
                libMeta.setDataHasChanged();

                configModel.removeRow(index);
                if (index > configModel.getRowCount() - 1)
                    index = configModel.getRowCount() - 1;
                if (index < 0)
                    index = 0;
                currentConfigIndex = index;
                configModel.fireTableDataChanged();
                if (configModel.getRowCount() > 0)
                {
                    configItems.changeSelection(index, 0, false, false);
                    loadGeneralTab();
                }
                else
                {
                    mf.tabbedPaneLibrarySpaces.setSelectedIndex(0);
                    ((CardLayout) mf.generalTab.getLayout()).show(mf.generalTab, "cardGettingStarted");

                    mf.labelLibaryType.setText("");
                    mf.buttonCopy.setEnabled(false);
                    mf.buttonDelete.setEnabled(false);
                    currentConfigIndex = 0;
                }
                configItems.requestFocus();
            }
        }
    }

    private void actionGenerateUUIDClicked(ActionEvent e)
    {
        currentConfigIndex = configItems.getSelectedRow();
        if (currentConfigIndex >= 0)
        {
            int reply = JOptionPane.YES_OPTION;
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            if (libMeta.key != null && libMeta.key.length() > 0)
            {
                String description = context.cfg.gs("Libraries.generate.new.uuid.for") + libMeta.description;
                String blank = "<html><body><br/></body></html>";
                String message = context.cfg.gs("Libraries.this.will.overwrite.any.existing.uuid");
                String question = context.cfg.gs("Z.are.you.sure");
                Object[] params = {description, blank, message, question};
                reply = JOptionPane.showConfirmDialog(mf, params, displayName, JOptionPane.OK_CANCEL_OPTION);
            }
            if (reply == JOptionPane.YES_OPTION)
            {
                UUID uuid = java.util.UUID.randomUUID();
                mf.textFieldKey.setText(uuid.toString());
                mf.textFieldKey.postActionEvent();
            }
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(mf, mf, context,
                    context.cfg.gs("Navigator.labelLibrariesHelp.toolTipText"), "libraries_" + context.preferences.getLocale() + ".html");
        }
        if (!helpDialog.fault)
        {
            if (!helpDialog.isVisible())
            {
                helpDialog.setVisible(true);
                // offset the help dialog from the parent
                Point loc = mf.getLocation();
                loc.x = loc.x + 82;
                loc.y = loc.y + 90;
                helpDialog.setLocation(loc);
            }
            else
            {
                helpDialog.toFront();
            }
        }
    }

    private void actionIgnorePatternAdd(ActionEvent e)
    {
        currentConfigIndex = configItems.getSelectedRow();
        if (currentConfigIndex >= 0)
        {
            String line1 = context.cfg.gs("Libraries.add.ignore.1");
            String line2 = context.cfg.gs("Libraries.add.ignore.2");
            String line3 = context.cfg.gs("Libraries.add.ignore.3");
            String line4 = context.cfg.gs("Libraries.add.ignore.4");
            String blank = "<html><body><br/></body></html>";
            Object[] params = {line1, blank, line2, line3, blank, line4};
            String pattern = JOptionPane.showInputDialog(mf, params, displayName, JOptionPane.PLAIN_MESSAGE);
            if (pattern != null && pattern.length() > 0)
            {
                LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
                int newSize = libMeta.repo.getLibraryData().libraries.ignore_patterns.length + 1;
                String[] expanded = new String[newSize];
                System.arraycopy(libMeta.repo.getLibraryData().libraries.ignore_patterns, 0, expanded,
                        0, libMeta.repo.getLibraryData().libraries.ignore_patterns.length);
                expanded[newSize - 1] = pattern;
                libMeta.repo.getLibraryData().libraries.ignore_patterns = expanded;
                libMeta.setDataHasChanged();
                loadGeneralTab();
                mf.listLibrariesIgnorePatterns.setSelectionInterval(newSize - 1, newSize - 1);
            }
        }
    }

    private void actionIgnorePatternRemove(ActionEvent e)
    {
        currentConfigIndex = configItems.getSelectedRow();
        if (currentConfigIndex >= 0)
        {
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            if (libMeta.repo.getLibraryData().libraries.ignore_patterns.length > 0)
            {
                int[] selected = mf.listLibrariesIgnorePatterns.getSelectedIndices();
                if (selected.length > 0)
                {
                    int reply = JOptionPane.showConfirmDialog(mf,
                            MessageFormat.format(context.cfg.gs("Libraries.are.you.sure.remove.ignores"), selected.length), displayName,
                            JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.YES_OPTION)
                    {
                        String[] ignores = libMeta.repo.getLibraryData().libraries.ignore_patterns;
                        ArrayList<String> contracted = new ArrayList<>();
                        for (int i = 0; i < ignores.length; ++i)
                        {
                            contracted.add(ignores[i]);
                        }
                        for (int i = selected.length - 1; i >= 0; --i)
                        {
                            contracted.remove(selected[i]);
                        }
                        ignores = new String[contracted.size()];
                        for (int i = 0; i < contracted.size(); ++i)
                        {
                            ignores[i] = contracted.get(i);
                        }
                        libMeta.repo.getLibraryData().libraries.ignore_patterns = ignores;
                        libMeta.setDataHasChanged();
                        loadGeneralTab();
                    }
                }
            }
        }
    }

    private void actionLibraryAdd(ActionEvent evt)
    {
        if (currentConfigIndex >= 0 && currentConfigIndex < configModel.getRowCount())
        {
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            try
            {
                Library lib = libMeta.repo.getLibrary(context.cfg.gs("Z.untitled"));
                if (lib == null)
                {
                    lib = new Library();
                    lib.name = context.cfg.gs("Z.untitled");
                    lib.sources = new String[0];

                    Library[] biblio = libMeta.repo.getLibraryData().libraries.bibliography;
                    int arraySize = biblio.length + 1;
                    Library[] expanded = new Library[arraySize];
                    System.arraycopy(libMeta.repo.getLibraryData().libraries.bibliography, 0, expanded,
                            0, libMeta.repo.getLibraryData().libraries.bibliography.length);
                    expanded[arraySize - 1] = lib;
                    libMeta.repo.getLibraryData().libraries.bibliography = expanded;
                    libMeta.setDataHasChanged();

                    biblioLibrariesTableModel.addRow(new Object[]{lib});
                    currentLibraryIndex = arraySize - 1;
                    loadBibliographyTab();

                    mf.tableBiblioLibraries.editCellAt(currentLibraryIndex, 0);
                    mf.tableBiblioLibraries.changeSelection(currentLibraryIndex, 0, false, false);
                    mf.tableBiblioLibraries.getEditorComponent().requestFocus();
                    ((JTextField) mf.tableBiblioLibraries.getEditorComponent()).selectAll();
                }
                else
                {
                    JOptionPane.showMessageDialog(mf, context.cfg.gs("Z.please.rename.the.existing") +
                            context.cfg.gs("Z.untitled"), displayName, JOptionPane.WARNING_MESSAGE);
                }
            }
            catch (MungeException e)
            {
                // should never happen
            }
        }
    }

    private void actionLibraryRemove(ActionEvent e)
    {
        if (currentConfigIndex >= 0)
        {
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            int[] rows = mf.tableBiblioLibraries.getSelectedRows();
            if (rows.length > 0)
            {
                int row = rows[0];
                int reply = JOptionPane.showConfirmDialog(mf, java.text.MessageFormat.format(context.cfg.gs("Libraries.remove.library.from"),
                                libMeta.repo.getLibraryData().libraries.bibliography[row].name, libMeta.description),
                        displayName, JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.YES_OPTION)
                {
                    Library[] libraries = libMeta.repo.getLibraryData().libraries.bibliography;
                    ArrayList<Library> libraryList = new ArrayList<>();
                    for (int i = 0; i < libraries.length; ++i)
                    {
                        libraryList.add(libraries[i]);
                    }
                    for (int i = rows.length - 1; i >= 0; --i)
                    {
                        libraryList.remove(rows[i]);
                    }
                    libraries = new Library[libraryList.size()];
                    for (int i = 0; i < libraryList.size(); ++i)
                    {
                        libraries[i] = libraryList.get(i);
                    }
                    libMeta.repo.getLibraryData().libraries.bibliography = libraries;

                    libMeta.setDataHasChanged();
                    currentLibraryIndex = 0;
                    currentSourceIndex = 0;
                    if (libraries.length == 0)
                        biblioLibrariesTableModel.setRowCount(0);
                    else
                    {
                        mf.tableBiblioLibraries.changeSelection(currentLocationIndex, 0, false, false);
                        loadBibliographyTab();
                    }
                }
            }
        }
    }

    private void actionLocationAddClicked(ActionEvent e)
    {
        if (currentConfigIndex >= 0 && currentConfigIndex < configModel.getRowCount())
        {
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            if (libMeta.repo.getLibraryData().libraries.bibliography.length > 0 &&
                    currentLibraryIndex < libMeta.repo.getLibraryData().libraries.bibliography.length)
            {
                directoryPicker = new DirectoryPicker(context, context.cfg.gs("Libraries.select.new.location.path"),
                        context.cfg.gs("Libraries.select.new.location"), true, false);

                directoryPicker.browserSelectionButton.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        boolean done = false;
                        boolean empty = false;
                        boolean isSubscriber = false;
                        int reply = JOptionPane.NO_OPTION;
                        if (context.browser.lastComponent != null)
                        {
                            try
                            {
                                context.mainFrame.tabbedPaneMain.setSelectedIndex(0);

                                // get selection(s)
                                ArrayList<Origin> origins = new ArrayList<Origin>();
                                isSubscriber = Origins.makeOriginsFromSelected(context, context.mainFrame, origins, true);

                                // there can be only one
                                if (origins != null && origins.size() > 0)
                                {
                                    int count = origins.size();
                                    Origin origin = origins.get(0);
                                    if (count != 1 || origin.getType() != NavTreeUserObject.REAL || !origin.tuo.isDir)
                                    {
                                        JOptionPane.showMessageDialog(directoryPicker.pane,
                                                context.cfg.gs(("Libraries.please.select.a.single.directory.to.add")),
                                                context.cfg.gs("Libraries.select.new.location.path"), JOptionPane.ERROR_MESSAGE);
                                    }
                                    else
                                    {
                                        // make path relative if possible
                                        String path;
                                        if (origin.tuo.isRemote)
                                            path = origin.getLocation();
                                        else
                                            path = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), origin.getLocation());

                                        // avoid duplicates
                                        boolean found = false;
                                        Location[] locations = libMeta.repo.getLibraryData().libraries.locations;
                                        for (int j = 0; j < locations.length; ++j)
                                        {
                                            if (locations[j].location.equals(path))
                                            {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (found)
                                        {
                                            JOptionPane.showMessageDialog(directoryPicker.pane,
                                                    context.cfg.gs("Libraries.that.location.is.already.defined"),
                                                    context.cfg.gs("Libraries.select.new.location.path"), JOptionPane.ERROR_MESSAGE);
                                            directoryPicker.directoryPathTextField.setText("");
                                            directoryPicker.pane.requestFocus();
                                        }
                                        else
                                        {
                                            // do origin repository and Libraries repository match?
                                            String key = origin.tuo.node.getMyRepo().getLibraryData().libraries.key;
                                            if (!libMeta.repo.getLibraryData().libraries.key.equals(key))
                                            {
                                                String name = libMeta.repo.getLibraryData().libraries.description;
                                                reply = JOptionPane.showConfirmDialog(directoryPicker.pane,
                                                        java.text.MessageFormat.format(context.cfg.gs("Libraries.selected.path.is.not.from.repository.continue"), name),
                                                        context.cfg.gs("Libraries.select.new.location.path"), JOptionPane.YES_NO_OPTION);
                                                if (reply == JOptionPane.YES_OPTION)
                                                    done = true;
                                            }
                                            else
                                            {
                                                // confirm using Browser selection
                                                directoryPicker.pane.requestFocus();
                                                String which = (isSubscriber) ? context.cfg.gs("Z.subscriber") : context.cfg.gs("Z.publisher");
                                                String message = MessageFormat.format(context.cfg.gs("Libraries.use.selected.item"), which);
                                                reply = JOptionPane.showConfirmDialog(directoryPicker.pane, message,
                                                        context.cfg.gs("Libraries.select.new.location.path"), JOptionPane.YES_NO_OPTION);
                                                done = true;
                                            }
                                        }

                                        if (reply == JOptionPane.YES_OPTION)
                                            directoryPicker.directoryPathTextField.setText(path);

                                        if (done)
                                            context.mainFrame.tabbedPaneMain.setSelectedIndex(1);
                                    }
                                }
                                else
                                    empty = true;
                            }
                            catch (Exception e)
                            {
                                String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                                if (e.getMessage() == null || !e.getMessage().equals("HANDLED_INTERNALLY"))
                                {
                                    if (context.navigator != null)
                                    {
                                        logger.error(msg);
                                        JOptionPane.showMessageDialog(directoryPicker.pane, msg,
                                                context.cfg.gs("Libraries.select.new.location.path"), JOptionPane.ERROR_MESSAGE);
                                    }
                                    else
                                        logger.error(msg);
                                }
                                else
                                    logger.error(msg);
                            }
                        }
                        else
                            empty = true;

                        if (empty)
                        {
                            JOptionPane.showMessageDialog(directoryPicker.pane, context.cfg.gs("Libraries.nothing.selected.in.browser"),
                                    context.cfg.gs("Libraries.select.new.location.path"), JOptionPane.WARNING_MESSAGE);
                        }
                    }
                });

                directoryPicker.directorySelectionButton.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        directoryPicker.directoryPathTextField.setText(filePicker(mf.buttonAddLocation));
                        String path = directoryPicker.directoryPathTextField.getText();

                        // avoid duplicates
                        boolean found = false;
                        Location[] locations = libMeta.repo.getLibraryData().libraries.locations;
                        for (int j = 0; j < locations.length; ++j)
                        {
                            if (locations[j].location.equals(path))
                            {
                                found = true;
                                break;
                            }
                        }
                        if (found)
                        {
                            JOptionPane.showMessageDialog(directoryPicker.pane,
                                    context.cfg.gs("Libraries.that.location.is.already.defined"),
                                    context.cfg.gs("Libraries.select.new.location.path"), JOptionPane.ERROR_MESSAGE);
                            directoryPicker.directoryPathTextField.setText("");
                        }
                    }
                });


                directoryPicker.pane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, ignored ->
                {
                    if (directoryPicker.pane != null && directoryPicker.pane.getValue() != null)
                    {
                        int selectedValue = (int) directoryPicker.pane.getValue();
                        if (selectedValue == JOptionPane.YES_OPTION)
                        {
                            String path = directoryPicker.directoryPathTextField.getText();
                            String min = directoryPicker.minSize.getText();
                            if (path.length() > 0 && min.length() > 0)
                            {
                                // add location to repository
                                Location loc = new Location();
                                loc.location = path;
                                loc.minimum = min + directoryPicker.scales.getSelectedItem();

                                Location[] locations = libMeta.repo.getLibraryData().libraries.locations;
                                int arraySize = locations.length + 1;
                                Location[] expanded = new Location[arraySize];
                                System.arraycopy(libMeta.repo.getLibraryData().libraries.locations, 0, expanded,
                                        0, libMeta.repo.getLibraryData().libraries.locations.length);
                                expanded[arraySize - 1] = loc;
                                libMeta.repo.getLibraryData().libraries.locations = expanded;
                                currentLocationIndex = arraySize - 1;

                                libMeta.setDataHasChanged();
                                mf.tableLocations.changeSelection(currentLocationIndex, 0, false, false);
                                loadLocationsTab();

                                // select added Location row
                                int i = 0;
                                for (; i < mf.tableLocations.getRowCount(); ++i)
                                {
                                    if (locationsTableModel.getValueAt(i, 0).equals(path))
                                        break;
                                }
                                if (i < mf.tableLocations.getRowCount())
                                {
                                    mf.tableLocations.setRowSelectionInterval(i, i);
                                }
                            }
                        }
                    }

                    context.mainFrame.tabbedPaneMain.setSelectedIndex(1);
                    mf.buttonNew.setEnabled(true);
                    mf.buttonCopy.setEnabled(true);
                    mf.buttonDelete.setEnabled(true);
                    mf.librariesConfigItems.setEnabled(true);
                    mf.tabbedPaneLibrarySpaces.setEnabled(true);
                    mf.buttonAddLocation.setEnabled(true);
                    mf.saveButton.setEnabled(true);
                    mf.cancelButton.setEnabled(true);
                    directoryPicker.dialog.dispose();
                    mf.buttonAddLocation.requestFocus();
                    directoryPicker = null;
                });

                mf.buttonNew.setEnabled(false);
                mf.buttonCopy.setEnabled(false);
                mf.buttonDelete.setEnabled(false);
                mf.librariesConfigItems.setEnabled(false);
                mf.tabbedPaneLibrarySpaces.setEnabled(false);
                mf.buttonAddLocation.setEnabled(false);
                mf.saveButton.setEnabled(false);
                mf.cancelButton.setEnabled(false);
                directoryPicker.dialog.setVisible(true);
            }
        }
    }

    private void actionLocationsRemove(ActionEvent e)
    {
        if (currentConfigIndex >= 0)
        {
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            int[] rows = mf.tableLocations.getSelectedRows();
            if (rows.length > 0)
            {
                int reply = JOptionPane.showConfirmDialog(mf, java.text.MessageFormat.format(context.cfg.gs("Libraries.remove.locations.from"),
                                rows.length, libMeta.description),
                        displayName, JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.YES_OPTION)
                {
                    Location[] locations = libMeta.repo.getLibraryData().libraries.locations;
                    ArrayList<Location> locationsList = new ArrayList<>();
                    for (int i = 0; i < locations.length; ++i)
                    {
                        locationsList.add(locations[i]);
                    }
                    for (int i = rows.length - 1; i >= 0; --i)
                    {
                        locationsList.remove(rows[i]);
                    }
                    locations = new Location[locationsList.size()];
                    for (int i = 0; i < locationsList.size(); ++i)
                    {
                        locations[i] = locationsList.get(i);
                    }
                    libMeta.repo.getLibraryData().libraries.locations = locations;

                    libMeta.setDataHasChanged();
                    mf.tableLocations.changeSelection(currentLocationIndex, 0, false, false);
                    loadLocationsTab();
                }
            }
        }
    }

    private void actionNewClicked(ActionEvent e)
    {
        if (configModel.find(context.cfg.gs("Z.untitled"), null) == null)
        {
            String message = context.cfg.gs("Libraries.mode.select.type");
            Object[] params = {message, comboBoxMode};
            comboBoxMode.setSelectedIndex(0);

            // get ELS operationsUI/mode

            //------------------------------------------
            // NOTES: Libraries were designed to use the
            // generalTab cards but the panelLibraryCard
            // is the only card implemented so far.  So
            // the card selection logic is commented out
            // --> and some places assume that card only
            // --> and mainframe.topType visible = false
            // because it is unneeded with a single type
            //------------------------------------------
            //    int opt = JOptionPane.showConfirmDialog(mf, params, displayName, JOptionPane.OK_CANCEL_OPTION);
            //    if (opt == JOptionPane.YES_OPTION)
            {
                LibMeta libMeta = new LibMeta();
                Mode mode = modes[comboBoxMode.getSelectedIndex()];
                libMeta.description = context.cfg.gs("Z.untitled");
                libMeta.key = "";
                libMeta.path = "";
                libMeta.card = mode.card;
                libMeta.repo = new Repository(context, -1);
                libMeta.repo.createStructure();
                libMeta.setDataHasChanged();
                initNewCard();

                mf.buttonCopy.setEnabled(true);
                mf.buttonDelete.setEnabled(true);
                mf.saveButton.setEnabled(true);

                configModel.addRow(new Object[]{libMeta});
                currentConfigIndex = configModel.getRowCount() - 1;
                loadGeneralTab();

                mf.tabbedPaneLibrarySpaces.setSelectedIndex(0);

                configItems.editCellAt(currentConfigIndex, 0);
                configItems.changeSelection(currentConfigIndex, 0, false, false);
                configItems.getEditorComponent().requestFocus();
                ((JTextField) configItems.getEditorComponent()).selectAll();
            }
            // MORE card selection logic
            //     else
            //         configItems.requestFocus();
        }
        else
        {
            JOptionPane.showMessageDialog(mf, context.cfg.gs("Z.please.rename.the.existing") +
                    context.cfg.gs("Z.untitled"), displayName, JOptionPane.WARNING_MESSAGE);
        }
    }

    private void actionOkClicked(ActionEvent e)
    {
        int cci = currentConfigIndex;

        boolean changes = saveConfigurations();
        savePreferences();

        if (configModel.getRowCount() - 1 < cci)
            cci = configModel.getRowCount() - 1;
        configItems.setRowSelectionInterval(cci, cci);

        if (changes)
            mf.labelStatusMiddle.setText(context.cfg.gs("Libraries.libraries.changes.saved"));
        else
            mf.labelStatusMiddle.setText(context.cfg.gs("Libraries.libraries.changes.none"));
    }

    private void actionSourcesAddClicked(ActionEvent e)
    {
        if (currentConfigIndex >= 0 && currentConfigIndex < configModel.getRowCount())
        {
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            if (libMeta.repo.getLibraryData().libraries.bibliography.length > 0 &&
                    currentLibraryIndex < libMeta.repo.getLibraryData().libraries.bibliography.length)
            {
                directoryPicker = new DirectoryPicker(context, context.cfg.gs("Libraries.select.new.source.path"),
                        context.cfg.gs("Libraries.select.new.source"), false, false);

                directoryPicker.browserSelectionButton.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        boolean done = false;
                        boolean empty = false;
                        boolean isSubscriber = false;
                        int reply = JOptionPane.NO_OPTION;
                        if (context.browser.lastComponent != null)
                        {
                            try
                            {
                                context.mainFrame.tabbedPaneMain.setSelectedIndex(0);

                                // get selection(s)
                                ArrayList<Origin> origins = new ArrayList<Origin>();
                                isSubscriber = Origins.makeOriginsFromSelected(context, context.mainFrame, origins, true);

                                // there can be only one
                                if (origins != null && origins.size() > 0)
                                {
                                    int count = origins.size();
                                    Origin origin = origins.get(0);
                                    if (count != 1 || origin.getType() != NavTreeUserObject.REAL || !origin.tuo.isDir)
                                    {
                                        JOptionPane.showMessageDialog(directoryPicker.pane,
                                                context.cfg.gs(("Libraries.please.select.a.single.directory.to.add")),
                                                context.cfg.gs("Libraries.select.new.source.path"), JOptionPane.ERROR_MESSAGE);
                                    }
                                    else
                                    {
                                        // make path relative if possible
                                        String path;
                                        if (origin.tuo.isRemote)
                                            path = origin.getLocation();
                                        else
                                            path = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), origin.getLocation());

                                        // avoid duplicates
                                        boolean found = false;
                                        Library lib = libMeta.repo.getLibraryData().libraries.bibliography[currentLibraryIndex];
                                        for (int j = 0; j < lib.sources.length; ++j)
                                        {
                                            if (lib.sources[j].equals(path))
                                            {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (found)
                                        {
                                            JOptionPane.showMessageDialog(directoryPicker.pane,
                                                    context.cfg.gs("Libraries.that.source.is.already.defined"),
                                                    context.cfg.gs("Libraries.select.new.source.path"), JOptionPane.ERROR_MESSAGE);
                                            directoryPicker.directoryPathTextField.setText("");
                                            directoryPicker.pane.requestFocus();
                                        }
                                        else
                                        {
                                            // do origin repository and Libraries repository match?
                                            String key = origin.tuo.node.getMyRepo().getLibraryData().libraries.key;
                                            if (!libMeta.repo.getLibraryData().libraries.key.equals(key))
                                            {
                                                String name = libMeta.repo.getLibraryData().libraries.description;
                                                reply = JOptionPane.showConfirmDialog(directoryPicker.pane,
                                                        java.text.MessageFormat.format(context.cfg.gs("Libraries.selected.path.is.not.from.repository.continue"), name),
                                                        context.cfg.gs("Libraries.select.new.source.path"), JOptionPane.YES_NO_OPTION);
                                                if (reply == JOptionPane.YES_OPTION)
                                                    done = true;
                                            }
                                            else
                                            {
                                                // confirm using Browser selection
                                                directoryPicker.pane.requestFocus();
                                                String which = (isSubscriber) ? context.cfg.gs("Z.subscriber") : context.cfg.gs("Z.publisher");
                                                String message = MessageFormat.format(context.cfg.gs("Libraries.use.selected.item"), which);
                                                reply = JOptionPane.showConfirmDialog(directoryPicker.pane, message,
                                                        context.cfg.gs("Libraries.select.new.source.path"), JOptionPane.YES_NO_OPTION);
                                                done = true;
                                            }
                                        }

                                        if (reply == JOptionPane.YES_OPTION)
                                            directoryPicker.directoryPathTextField.setText(path);

                                        if (done)
                                            context.mainFrame.tabbedPaneMain.setSelectedIndex(1);
                                    }
                                }
                                else
                                    empty = true;
                            }
                            catch (Exception e)
                            {
                                String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                                if (e.getMessage() == null || !e.getMessage().equals("HANDLED_INTERNALLY"))
                                {
                                    if (context.navigator != null)
                                    {
                                        logger.error(msg);
                                        JOptionPane.showMessageDialog(directoryPicker.pane, msg,
                                                context.cfg.gs("Libraries.select.new.source.path"), JOptionPane.ERROR_MESSAGE);
                                    }
                                    else
                                        logger.error(msg);
                                }
                                else
                                    logger.error(msg);
                            }
                        }
                        else
                            empty = true;

                        if (empty)
                        {
                            JOptionPane.showMessageDialog(directoryPicker.pane, context.cfg.gs("Libraries.nothing.selected.in.browser"),
                                    context.cfg.gs("Libraries.select.new.source.path"), JOptionPane.WARNING_MESSAGE);
                        }
                    }
                });

                directoryPicker.directorySelectionButton.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        directoryPicker.directoryPathTextField.setText(filePicker(mf.buttonAddSource));
                        String path = directoryPicker.directoryPathTextField.getText();

                        // avoid duplicates
                        boolean found = false;
                        Library lib = libMeta.repo.getLibraryData().libraries.bibliography[currentLibraryIndex];
                        for (int j = 0; j < lib.sources.length; ++j)
                        {
                            if (lib.sources[j].equals(path))
                            {
                                found = true;
                                break;
                            }
                        }
                        if (found)
                        {
                            JOptionPane.showMessageDialog(directoryPicker.pane,
                                    context.cfg.gs("Libraries.that.source.is.already.defined"),
                                    context.cfg.gs("Libraries.select.new.source.path"), JOptionPane.ERROR_MESSAGE);
                            directoryPicker.directoryPathTextField.setText("");
                        }
                    }
                });

                directoryPicker.pane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, ignored ->
                {
                    if (directoryPicker.pane != null && directoryPicker.pane.getValue() != null)
                    {
                        int selectedValue = (int) directoryPicker.pane.getValue();
                        if (selectedValue == JOptionPane.YES_OPTION)
                        {
                            Library lib = libMeta.repo.getLibraryData().libraries.bibliography[currentLibraryIndex];
                            String path = directoryPicker.directoryPathTextField.getText();
                            if (path != null && path.length() > 0)
                            {
                                int arraySize = lib.sources.length + 1;
                                String[] expanded = new String[arraySize];
                                System.arraycopy(lib.sources, 0, expanded,
                                        0, lib.sources.length);
                                expanded[arraySize - 1] = path;
                                lib.sources = expanded;
                                listSourcesModel.addElement(path);
                                libMeta.setDataHasChanged();
                            }
                            currentSourceIndex = lib.sources.length - 1;
                            loadSources();
                        }
                    }

                    context.mainFrame.tabbedPaneMain.setSelectedIndex(1);
                    mf.buttonNew.setEnabled(true);
                    mf.buttonCopy.setEnabled(true);
                    mf.buttonDelete.setEnabled(true);
                    mf.librariesConfigItems.setEnabled(true);
                    mf.tabbedPaneLibrarySpaces.setEnabled(true);
                    mf.tableBiblioLibraries.setEnabled(true);
                    mf.buttonAddLibrary.setEnabled(true);
                    mf.buttonRemoveLibrary.setEnabled(true);
                    mf.buttonAddSource.setEnabled(true);
                    mf.buttonAddMultiSource.setEnabled(true);
                    mf.saveButton.setEnabled(true);
                    mf.cancelButton.setEnabled(true);
                    directoryPicker.dialog.dispose();
                    mf.buttonAddSource.requestFocus();
                    directoryPicker = null;
                });

                mf.buttonNew.setEnabled(false);
                mf.buttonCopy.setEnabled(false);
                mf.buttonDelete.setEnabled(false);
                mf.librariesConfigItems.setEnabled(false);
                mf.tabbedPaneLibrarySpaces.setEnabled(false);
                mf.tableBiblioLibraries.setEnabled(false);
                mf.buttonAddLibrary.setEnabled(false);
                mf.buttonRemoveLibrary.setEnabled(false);
                mf.buttonAddSource.setEnabled(false);
                mf.buttonAddMultiSource.setEnabled(false);
                mf.saveButton.setEnabled(false);
                mf.cancelButton.setEnabled(false);
                directoryPicker.dialog.setVisible(true);
            }
        }
    }

    private void actionSourcesDown(ActionEvent e)
    {
        if (currentConfigIndex >= 0)
        {
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            int[] rows = mf.listSources.getSelectedIndices();
            if (rows.length > 1)
            {
                JOptionPane.showMessageDialog(mf, context.cfg.gs("Libraries.please.select.a.single.source.to.move"), displayName, JOptionPane.INFORMATION_MESSAGE);
            }
            else if (rows.length == 1)
            {
                int row = rows[0];
                String[] sources = libMeta.repo.getLibraryData().libraries.bibliography[currentLibraryIndex].sources;
                if (row >= 0 && row < sources.length - 1)
                {
                    mf.listSources.requestFocus();

                    String s1 = sources[row];
                    String s2 = sources[row + 1];
                    sources[row + 1] = s1;
                    sources[row] = s2;
                    libMeta.repo.getLibraryData().libraries.bibliography[currentLibraryIndex].sources = sources;
                    currentSourceIndex = row + 1;
                    libMeta.setDataHasChanged();
                    loadSources();
                }
            }
        }
    }

    private void actionSourcesMultiClicked(ActionEvent e)
    {
        if (currentConfigIndex >= 0 && currentConfigIndex < configModel.getRowCount())
        {
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            if (libMeta.repo.getLibraryData().libraries.bibliography.length > 0 &&
                    currentLibraryIndex < libMeta.repo.getLibraryData().libraries.bibliography.length)
            {
                directoryPicker = new DirectoryPicker(context, context.cfg.gs("Libraries.select.new.multiple.source.path"),
                        context.cfg.gs("Libraries.select.new.multiple.source"), false, true);

                directoryPicker.browserSelectionButton.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        boolean done = false;
                        boolean empty = false;
                        boolean isSubscriber = false;
                        int reply = JOptionPane.NO_OPTION;
                        if (context.browser.lastComponent != null)
                        {
                            try
                            {
                                context.mainFrame.tabbedPaneMain.setSelectedIndex(0);

                                // get selection(s)
                                ArrayList<Origin> origins = new ArrayList<Origin>();
                                isSubscriber = Origins.makeOriginsFromSelected(context, context.mainFrame, origins, true);

                                // there can be only one
                                if (origins != null && origins.size() > 0)
                                {
                                    int count = origins.size();
                                    Origin origin = origins.get(0);
                                    if (count != 1 || origin.getType() != NavTreeUserObject.REAL || !origin.tuo.isDir)
                                    {
                                        JOptionPane.showMessageDialog(directoryPicker.pane,
                                                context.cfg.gs(("Libraries.please.select.a.single.directory.to.add")),
                                                context.cfg.gs("Libraries.select.new.multiple.source.path"), JOptionPane.ERROR_MESSAGE);
                                    }
                                    else
                                    {
                                        // make path relative if possible
                                        String path;
                                        if (origin.tuo.isRemote)
                                            path = origin.getLocation();
                                        else
                                            path = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), origin.getLocation());

                                        // avoid duplicates
                                        boolean found = false;
                                        Library lib = libMeta.repo.getLibraryData().libraries.bibliography[currentLibraryIndex];
                                        for (int j = 0; j < lib.sources.length; ++j)
                                        {
                                            if (lib.sources[j].equals(path))
                                            {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (found)
                                        {
                                            JOptionPane.showMessageDialog(directoryPicker.pane,
                                                    context.cfg.gs("Libraries.that.source.is.already.defined"),
                                                    context.cfg.gs("Libraries.select.new.multiple.source.path"), JOptionPane.ERROR_MESSAGE);
                                            directoryPicker.directoryPathTextField.setText("");
                                            directoryPicker.pane.requestFocus();
                                        }
                                        else
                                        {
                                            // do origin repository and Libraries repository match?
                                            String key = origin.tuo.node.getMyRepo().getLibraryData().libraries.key;
                                            if (!libMeta.repo.getLibraryData().libraries.key.equals(key))
                                            {
                                                String name = libMeta.repo.getLibraryData().libraries.description;
                                                reply = JOptionPane.showConfirmDialog(directoryPicker.pane,
                                                        java.text.MessageFormat.format(context.cfg.gs("Libraries.selected.path.is.not.from.repository.continue"), name),
                                                        context.cfg.gs("Libraries.select.new.multiple.source.path"), JOptionPane.YES_NO_OPTION);
                                                if (reply == JOptionPane.YES_OPTION)
                                                    done = true;
                                            }
                                            else
                                            {
                                                // confirm using Browser selection
                                                directoryPicker.pane.requestFocus();
                                                String which = (isSubscriber) ? context.cfg.gs("Z.subscriber") : context.cfg.gs("Z.publisher");
                                                String message = MessageFormat.format(context.cfg.gs("Libraries.use.selected.item"), which);
                                                reply = JOptionPane.showConfirmDialog(directoryPicker.pane, message,
                                                        context.cfg.gs("Libraries.select.new.multiple.source.path"), JOptionPane.YES_NO_OPTION);
                                                done = true;
                                            }
                                        }

                                        if (reply == JOptionPane.YES_OPTION)
                                            directoryPicker.directoryPathTextField.setText(path);

                                        if (done)
                                            context.mainFrame.tabbedPaneMain.setSelectedIndex(1);
                                    }
                                }
                                else
                                    empty = true;
                            }
                            catch (Exception e)
                            {
                                String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                                if (e.getMessage() == null || !e.getMessage().equals("HANDLED_INTERNALLY"))
                                {
                                    if (context.navigator != null)
                                    {
                                        logger.error(msg);
                                        JOptionPane.showMessageDialog(directoryPicker.pane, msg,
                                                context.cfg.gs("Libraries.select.new.multiple.source.path"), JOptionPane.ERROR_MESSAGE);
                                    }
                                    else
                                        logger.error(msg);
                                }
                                else
                                    logger.error(msg);
                            }
                        }
                        else
                            empty = true;

                        if (empty)
                        {
                            JOptionPane.showMessageDialog(directoryPicker.pane, context.cfg.gs("Libraries.nothing.selected.in.browser"),
                                    context.cfg.gs("Libraries.select.new.multiple.source.path"), JOptionPane.WARNING_MESSAGE);
                        }
                    }
                });

                directoryPicker.directorySelectionButton.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        directoryPicker.directoryPathTextField.setText(filePicker(mf.buttonAddSource));
                        String path = directoryPicker.directoryPathTextField.getText();

                        // avoid duplicates
                        boolean found = false;
                        Library lib = libMeta.repo.getLibraryData().libraries.bibliography[currentLibraryIndex];
                        for (int j = 0; j < lib.sources.length; ++j)
                        {
                            if (lib.sources[j].equals(path))
                            {
                                found = true;
                                break;
                            }
                        }
                        if (found)
                        {
                            JOptionPane.showMessageDialog(directoryPicker.pane,
                                    context.cfg.gs("Libraries.that.source.is.already.defined"),
                                    context.cfg.gs("Libraries.select.new.multiple.source.path"), JOptionPane.ERROR_MESSAGE);
                            directoryPicker.directoryPathTextField.setText("");
                        }
                    }
                });

                directoryPicker.allCheckbox.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        boolean sense = directoryPicker.allCheckbox.isSelected();
                        for (int i = 0; i < librarySelectorTableModel.getRowCount(); ++i)
                        {
                            librarySelectors[i].selected = sense;
                        }
                        librarySelectorTableModel.fireTableDataChanged();
                    }
                });

                directoryPicker.pane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, ignored ->
                {
                    if (directoryPicker.pane != null && directoryPicker.pane.getValue() != null)
                    {
                        int selectedValue = (int) directoryPicker.pane.getValue();
                        if (selectedValue == JOptionPane.YES_OPTION)
                        {
                            String path = directoryPicker.directoryPathTextField.getText();
                            if (path != null && path.length() > 0)
                            {
                                try
                                {
                                    // add location to selected libraries
                                    boolean dontAsk = false;
                                    for (int i = 0; i < librarySelectors.length; ++i)
                                    {
                                        if (librarySelectors[i].selected)
                                        {
                                            Library lib = libMeta.repo.getLibrary(librarySelectors[i].name);
                                            if (lib != null)
                                            {
                                                String source = path + libMeta.repo.getSeparator() + lib.name;
                                                boolean found = false;
                                                // avoid adding a duplicate
                                                for (int j = 0; j < lib.sources.length; ++j)
                                                {
                                                    if (lib.sources[j].equals(source))
                                                    {
                                                        found = true;
                                                        break;
                                                    }
                                                }
                                                if (!found)
                                                {
                                                    int srcSize = lib.sources.length + 1;
                                                    String[] expSources = new String[srcSize];
                                                    System.arraycopy(lib.sources, 0, expSources, 0, lib.sources.length);
                                                    expSources[srcSize - 1] = source;
                                                    lib.sources = expSources;
                                                    libMeta.setDataHasChanged();
                                                }
                                                else
                                                {
                                                    if (!dontAsk)
                                                    {
                                                        Object[] opts = {context.cfg.gs("Z.ok"), context.cfg.gs("Z.dont.ask.again")};
                                                        int reply = JOptionPane.showOptionDialog(directoryPicker.pane,
                                                                java.text.MessageFormat.format(context.cfg.gs("Libraries.select.new.multiple.already.defined"), source, lib.name),
                                                                context.cfg.gs("Libraries.select.new.multiple.source.path"),
                                                                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, opts, opts[0]);
                                                        if (reply == JOptionPane.NO_OPTION)
                                                            dontAsk = true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                catch (MungeException me)
                                {
                                    // should never happen
                                }

                                loadSources();
                            }
                        }
                    }

                    context.mainFrame.tabbedPaneMain.setSelectedIndex(1);
                    mf.buttonNew.setEnabled(true);
                    mf.buttonCopy.setEnabled(true);
                    mf.buttonDelete.setEnabled(true);
                    mf.librariesConfigItems.setEnabled(true);
                    mf.tabbedPaneLibrarySpaces.setEnabled(true);
                    mf.tableBiblioLibraries.setEnabled(true);
                    mf.buttonAddLibrary.setEnabled(true);
                    mf.buttonRemoveLibrary.setEnabled(true);
                    mf.buttonAddSource.setEnabled(true);
                    mf.buttonAddMultiSource.setEnabled(true);
                    mf.saveButton.setEnabled(true);
                    mf.cancelButton.setEnabled(true);
                    directoryPicker.dialog.dispose();
                    mf.buttonAddMultiSource.requestFocus();
                    directoryPicker = null;
                });

                // load libraries table
                if (libMeta.repo != null &&
                        libMeta.repo.getLibraryData().libraries.bibliography != null && libMeta.repo.getLibraryData().libraries.bibliography.length > 0)
                {
                    Library[] biblio = libMeta.repo.getLibraryData().libraries.bibliography;
                    librarySelectors = new LibrarySelector[biblio.length];
                    librarySelectorTableModel = new LibrarySelectorTableModel(context, librarySelectors);
                    for (int i = 0; i < biblio.length; ++i)
                    {
                        LibrarySelector libSel = new LibrarySelector(biblio[i].name);
                        librarySelectors[i] = libSel;
                    }
                    Arrays.sort(librarySelectors);
                }
                else
                {
                    librarySelectors = new LibrarySelector[0];
                    librarySelectorTableModel = new LibrarySelectorTableModel(context, null);
                }
                directoryPicker.table.setModel(librarySelectorTableModel);
                // setup columns
                TableColumn column = directoryPicker.table.getColumnModel().getColumn(0);
                column.setResizable(false);
                column.setWidth(32);
                column.setPreferredWidth(32);
                column.setMaxWidth(32);
                column.setMinWidth(32);
                column = directoryPicker.table.getColumnModel().getColumn(1);
                column.setResizable(false);


                mf.buttonNew.setEnabled(false);
                mf.buttonCopy.setEnabled(false);
                mf.buttonDelete.setEnabled(false);
                mf.librariesConfigItems.setEnabled(false);
                mf.tabbedPaneLibrarySpaces.setEnabled(false);
                mf.tableBiblioLibraries.setEnabled(false);
                mf.buttonAddLibrary.setEnabled(false);
                mf.buttonRemoveLibrary.setEnabled(false);
                mf.buttonAddSource.setEnabled(false);
                mf.buttonAddMultiSource.setEnabled(false);
                mf.saveButton.setEnabled(false);
                mf.cancelButton.setEnabled(false);
                directoryPicker.dialog.setVisible(true);
            }
        }
    }

    private void actionSourcesRemove(ActionEvent e)
    {
        if (currentConfigIndex >= 0)
        {
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            int[] rows = mf.listSources.getSelectedIndices();
            if (rows.length > 0)
            {
                int reply = JOptionPane.showConfirmDialog(mf, java.text.MessageFormat.format(context.cfg.gs("Libraries.remove.sources.from.library"),
                                rows.length, libMeta.description, libMeta.repo.getLibraryData().libraries.bibliography[currentLibraryIndex].name),
                        displayName, JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.YES_OPTION)
                {
                    String[] sources = libMeta.repo.getLibraryData().libraries.bibliography[currentLibraryIndex].sources;
                    ArrayList<String> sourcesList = new ArrayList<>();
                    for (int i = 0; i < sources.length; ++i)
                    {
                        sourcesList.add(sources[i]);
                    }
                    for (int i = rows.length - 1; i >= 0; --i)
                    {
                        sourcesList.remove(rows[i]);
                    }
                    sources = new String[sourcesList.size()];
                    for (int i = 0; i < sourcesList.size(); ++i)
                    {
                        sources[i] = sourcesList.get(i);
                    }
                    libMeta.repo.getLibraryData().libraries.bibliography[currentLibraryIndex].sources = sources;

                    libMeta.setDataHasChanged();
                    mf.tableLocations.changeSelection(currentLocationIndex, 0, false, false);
                    loadSources();
                }
            }
        }
    }

    private void actionSourcesUp(ActionEvent e)
    {
        if (currentConfigIndex >= 0)
        {
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            int[] rows = mf.listSources.getSelectedIndices();
            if (rows.length > 1)
            {
                JOptionPane.showMessageDialog(mf, context.cfg.gs("Libraries.please.select.a.single.source.to.move"), displayName, JOptionPane.INFORMATION_MESSAGE);
            }
            else if (rows.length == 1)
            {
                int row = rows[0];
                if (row > 0)
                {
                    mf.listSources.requestFocus();

                    String[] sources = libMeta.repo.getLibraryData().libraries.bibliography[currentLibraryIndex].sources;
                    String s1 = sources[row];
                    String s2 = sources[row - 1];
                    sources[row - 1] = s1;
                    sources[row] = s2;
                    libMeta.repo.getLibraryData().libraries.bibliography[currentLibraryIndex].sources = sources;
                    currentSourceIndex = row - 1;
                    libMeta.setDataHasChanged();
                    loadSources();
                }
            }
        }
    }

    private void actionSelectTempLocationClicked(ActionEvent e)
    {
        if (currentConfigIndex >= 0 && currentConfigIndex < configModel.getRowCount())
        {
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            if (libMeta.repo.getLibraryData().libraries.bibliography.length > 0 &&
                    currentLibraryIndex < libMeta.repo.getLibraryData().libraries.bibliography.length)
            {
                directoryPicker = new DirectoryPicker(context, context.cfg.gs("Libraries.select.temp.location.path"),
                        context.cfg.gs("Libraries.select.temp.location"), false, false);

                directoryPicker.browserSelectionButton.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        boolean done = false;
                        boolean empty = false;
                        boolean isSubscriber = false;
                        int reply = JOptionPane.NO_OPTION;
                        if (context.browser.lastComponent != null)
                        {
                            try
                            {
                                context.mainFrame.tabbedPaneMain.setSelectedIndex(0);

                                // get selection(s)
                                ArrayList<Origin> origins = new ArrayList<Origin>();
                                isSubscriber = Origins.makeOriginsFromSelected(context, context.mainFrame, origins, true);

                                // there can be only one
                                if (origins != null && origins.size() > 0)
                                {
                                    int count = origins.size();
                                    Origin origin = origins.get(0);
                                    if (count != 1 || origin.getType() != NavTreeUserObject.REAL || !origin.tuo.isDir)
                                    {
                                        JOptionPane.showMessageDialog(directoryPicker.pane,
                                                context.cfg.gs(("Libraries.please.select.a.single.directory.to.add")),
                                                context.cfg.gs("Libraries.select.temp.location.path"), JOptionPane.ERROR_MESSAGE);
                                    }
                                    else
                                    {
                                        // make path relative if possible
                                        String path;
                                        if (origin.tuo.isRemote)
                                            path = origin.getLocation();
                                        else
                                            path = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), origin.getLocation());

                                        // do origin repository and Libraries repository match?
                                        String key = origin.tuo.node.getMyRepo().getLibraryData().libraries.key;
                                        if (!libMeta.repo.getLibraryData().libraries.key.equals(key))
                                        {
                                            String name = libMeta.repo.getLibraryData().libraries.description;
                                            reply = JOptionPane.showConfirmDialog(directoryPicker.pane,
                                                    java.text.MessageFormat.format(context.cfg.gs("Libraries.selected.path.is.not.from.repository.continue"), name),
                                                    context.cfg.gs("Libraries.select.temp.location.path"), JOptionPane.YES_NO_OPTION);
                                            if (reply == JOptionPane.YES_OPTION)
                                                done = true;
                                        }
                                        else
                                        {
                                            // confirm using Browser selection
                                            directoryPicker.pane.requestFocus();
                                            String which = (isSubscriber) ? context.cfg.gs("Z.subscriber") : context.cfg.gs("Z.publisher");
                                            String message = MessageFormat.format(context.cfg.gs("Libraries.use.selected.item"), which);
                                            reply = JOptionPane.showConfirmDialog(directoryPicker.pane, message,
                                                    context.cfg.gs("Libraries.select.temp.location.path"), JOptionPane.YES_NO_OPTION);
                                            done = true;
                                        }

                                        if (reply == JOptionPane.YES_OPTION)
                                            directoryPicker.directoryPathTextField.setText(path);

                                        if (done)
                                            context.mainFrame.tabbedPaneMain.setSelectedIndex(1);
                                    }
                                }
                                else
                                    empty = true;
                            }
                            catch (Exception e)
                            {
                                String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                                if (e.getMessage() == null || !e.getMessage().equals("HANDLED_INTERNALLY"))
                                {
                                    if (context.navigator != null)
                                    {
                                        logger.error(msg);
                                        JOptionPane.showMessageDialog(directoryPicker.pane, msg,
                                                context.cfg.gs("Libraries.select.temp.location.path"), JOptionPane.ERROR_MESSAGE);
                                    }
                                    else
                                        logger.error(msg);
                                }
                                else
                                    logger.error(msg);
                            }
                        }
                        else
                            empty = true;

                        if (empty)
                        {
                            JOptionPane.showMessageDialog(directoryPicker.pane, context.cfg.gs("Libraries.nothing.selected.in.browser"),
                                    context.cfg.gs("Libraries.select.temp.location.path"), JOptionPane.WARNING_MESSAGE);
                        }
                    }
                });

                directoryPicker.directorySelectionButton.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        directoryPicker.directoryPathTextField.setText(filePicker(mf.buttonLibrarySelectTempLocation));
                    }
                });


                directoryPicker.pane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, ignored ->
                {
                    if (directoryPicker.pane != null && directoryPicker.pane.getValue() != null)
                    {
                        int selectedValue = (int) directoryPicker.pane.getValue();
                        if (selectedValue == JOptionPane.YES_OPTION)
                        {
                            String path = directoryPicker.directoryPathTextField.getText();
                            if (path.length() > 0)
                            {
                                mf.textFieldTempLocation.setText(path);
                                mf.textFieldTempLocation.postActionEvent();
                                libMeta.setDataHasChanged();
                            }
                        }
                    }

                    context.mainFrame.tabbedPaneMain.setSelectedIndex(1);
                    mf.buttonNew.setEnabled(true);
                    mf.buttonCopy.setEnabled(true);
                    mf.buttonDelete.setEnabled(true);
                    mf.librariesConfigItems.setEnabled(true);
                    mf.tabbedPaneLibrarySpaces.setEnabled(true);
                    mf.saveButton.setEnabled(true);
                    mf.cancelButton.setEnabled(true);
                    directoryPicker.dialog.dispose();
                    mf.textFieldTempLocation.requestFocus();
                    directoryPicker = null;
                });

                mf.buttonNew.setEnabled(false);
                mf.buttonCopy.setEnabled(false);
                mf.buttonDelete.setEnabled(false);
                mf.librariesConfigItems.setEnabled(false);
                mf.tabbedPaneLibrarySpaces.setEnabled(false);
                mf.saveButton.setEnabled(false);
                mf.cancelButton.setEnabled(false);
                directoryPicker.dialog.setVisible(true);
            }
        }
    }

    private void actionUndoClicked(ActionEvent e)
    {
        if (checkForChanges())
        {
            int reply = JOptionPane.showConfirmDialog(mf, context.cfg.gs("Z.cancel.all.changes"),
                    displayName, JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                cancelChanges();
            }
        }
        else
            mf.labelStatusMiddle.setText(context.cfg.gs("Libraries.nothing.to.undo"));
    }

    private void cancelChanges()
    {
        int cci = currentConfigIndex;
        if (deletedLibraries.size() > 0)
            deletedLibraries = new ArrayList<LibMeta>();

        configModel.setRowCount(0);
        biblioLibrariesTableModel.setRowCount(0);
        loadConfigurations();

        if (configModel.getRowCount() - 1 < cci)
            cci = configModel.getRowCount() - 1;
        configItems.setRowSelectionInterval(cci, cci);

        mf.labelStatusMiddle.setText(context.cfg.gs("Libraries.libraries.changes.cancelled"));
    }

    public void cardShown(ComponentEvent e)
    {
        // other cards not implemented, but can be
    }

    public boolean checkForChanges()
    {
        for (int i = 0; i < deletedLibraries.size(); ++i)
        {
            if (deletedLibraries.get(i).isDataChanged())
                return true;
        }

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            if (((LibMeta) configModel.getValueAt(i, 0)).isDataChanged())
            {
                return true;
            }
        }
        return false;
    }

    private String filePicker(JButton button)
    {
        String path = "";
        JFileChooser fc = new JFileChooser();
        fc.setFileHidingEnabled(false);
        fc.setDialogTitle(button.getToolTipText());

        fc.setFileFilter(new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                if (file.isDirectory())
                    return true;
                if (pickerDirectoryOnly && !file.isDirectory())
                    return false;
                if (!pickerAnyFile)
                    return (file.getName().toLowerCase().endsWith(".json"));
                return true;
            }

            @Override
            public String getDescription()
            {
                String desc = "";
                switch (button.getName().toLowerCase())
                {
                    case "selectlocation":
                        desc = context.cfg.gs("Libraries.select.new.location");
                        break;
                    case "templocation":
                        desc = context.cfg.gs("Libraries.temp.location");
                        break;
                    case "addsource":
                        desc = context.cfg.gs("Libraries.select.new.source.path");
                        break;
                    case "selectsource":
                        desc = context.cfg.gs("Libraries.select.new.source.base.path");
                        break;
                }
                return desc;
            }
        });

        String fileName = "";
        switch (button.getName().toLowerCase())
        {
            case "selectlocation":
                pickerAnyFile = true;
                pickerDirectoryOnly = true;
                pickerFileMustExist = true;
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setAcceptAllFileFilterUsed(false);
                break;
            case "templocation":
                fileName = mf.textFieldTempLocation.getText();
                pickerAnyFile = true;
                pickerDirectoryOnly = true;
                pickerFileMustExist = true;
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setAcceptAllFileFilterUsed(false);
                break;
            case "addsource":
            case "selectsource":
                pickerAnyFile = true;
                pickerDirectoryOnly = true;
                pickerFileMustExist = true;
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setAcceptAllFileFilterUsed(false);
                break;
        }

        File dir;
        File file;
        if (fileName.length() > 0)
        {
            dir = new File(pickerDirectoryOnly ? fileName : filePickerDirectory(fileName));
            fc.setCurrentDirectory(dir.getAbsoluteFile());

            file = new File(fileName);
            fc.setSelectedFile(file);
        }
        else if (lastDirectory != null)
        {
            fc.setCurrentDirectory(lastDirectory);
        }
        else
        {
            // default to ELS home
            fc.setCurrentDirectory(new File(context.cfg.getWorkingDirectory()));
        }

        fc.setDialogType(pickerFileMustExist ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG);

        while (true)
        {
            int selection = fc.showOpenDialog((directoryPicker == null) ? mf : directoryPicker.pane);
            if (selection == JFileChooser.APPROVE_OPTION)
            {
                file = fc.getSelectedFile();
                lastDirectory = (pickerDirectoryOnly) ? file : fc.getCurrentDirectory();

                // sanity checks
                if (!pickerDirectoryOnly && file.isDirectory())
                {
                    JOptionPane.showMessageDialog(mf,
                            context.cfg.gs("Navigator.open.error.select.a.file.only"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                if (pickerFileMustExist && !file.exists())
                {
                    JOptionPane.showMessageDialog(mf,
                            context.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                            displayName, JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                if (pickerFileMustExist && !file.canWrite())
                {
                    JOptionPane.showMessageDialog(mf,
                            context.cfg.gs(pickerDirectoryOnly ? "Navigator.open.error.directory.not.writable" : "Navigator.open.error.file.not.writable") + file.getName(),
                            displayName, JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                // make path relative if possible
                path = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), file.getPath());

                // save value & fire updateOnChange()
                switch (button.getName().toLowerCase())
                {
                    // textFieldOperation
                    case "templocation":
                        mf.textFieldTempLocation.setText(path);
                        mf.textFieldTempLocation.postActionEvent();
                        break;
                }
            }
            break;
        }
        return path;
    }

    private String filePickerDirectory(String path)
    {
        if (Utils.isRelativePath(path))
            path = context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + path;
        return Utils.getLeftPath(path, Utils.getSeparatorFromPath(path));
    }

    public void genericAction(ActionEvent e)
    {
        if (e.getSource().getClass().equals(JButton.class))
        {
            JButton button = (JButton) e.getSource();
            if (button.getActionCommand().toLowerCase().endsWith("filepick"))
            {
                //filePicker(button);
            }
            else
                updateOnChange(e.getSource());
        }
        else
        {
            updateOnChange(e.getSource());
        }
    }

    public void genericTextFieldFocusLost(FocusEvent e)
    {
        updateOnChange(e.getSource());
    }

    public ArrayList<LibMeta> getDeletedLibraries()
    {
        return deletedLibraries;
    }

    public String getDirectoryPath()
    {
        String path = System.getProperty("user.dir") + System.getProperty("file.separator") + "libraries";
        return path;
    }

    public String getPromptedPath()
    {
        return this.promptedPath;
    }

    private Repositories getRepositories()
    {
        Repositories repositories = null;
        try
        {
            repositories = new Repositories();
            repositories.loadList(context);
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(mf, msg, displayName, JOptionPane.ERROR_MESSAGE);
        }
        return repositories;
    }

    private void initNewCard()
    {
        loading = true;
        updateState();
        loading = false;
    }

    private void initialize()
    {
        this.configItems = mf.librariesConfigItems;
        this.deletedLibraries = new ArrayList<LibMeta>();

        // scale the help icon
        Icon icon = mf.labelLibrariesHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        mf.labelLibrariesHelp.setIcon(replacement);

        // dividers
        mf.splitPaneLibs.setDividerLocation(context.preferences.getLibrariesDividerLocation());
        mf.splitPanelBiblio.setDividerLocation(context.preferences.getLibrariesBiblioDividerLocation());

        // setup the left-side list of configurations
        configModel = new ConfigModel(context, displayName, this);
        configModel.setColumnCount(1);
        configItems.setModel(configModel);

        configItems.getTableHeader().setUI(null);
        configItems.setTableHeader(null);
        mf.scrollPaneConfig.setColumnHeaderView(null);

        //
        ListSelectionModel lsm = configItems.getSelectionModel();
        lsm.addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent)
            {
                if (!listSelectionEvent.getValueIsAdjusting())
                {
                    ListSelectionModel sm = (ListSelectionModel) listSelectionEvent.getSource();
                    int index = sm.getMinSelectionIndex();
                    if (index >= 0 && index != currentConfigIndex)
                    {
                        currentConfigIndex = index;
                        currentLocationIndex = 0;
                        currentLibraryIndex = 0;
                        currentSourceIndex = 0;
                        mf.labelStatusMiddle.setText("");
                        loadGeneralTab();
                    }
                }
            }
        });

        // setup the right-side tab handler
        mf.tabbedPaneLibrarySpaces.addChangeListener(e -> tabbedPaneLibrarySpacesStateChanged(e));

        // locations tab
        locationsTableModel = new LocationsTableModel(context, null);
        ListSelectionModel llsm = mf.tableLocations.getSelectionModel();
        llsm.addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent)
            {
                if (!listSelectionEvent.getValueIsAdjusting())
                {
                    ListSelectionModel sm = (ListSelectionModel) listSelectionEvent.getSource();
                    int index = sm.getMinSelectionIndex();
                    if (index >= 0 && index != currentLocationIndex)
                    {
                        currentLocationIndex = index;
                        mf.labelStatusMiddle.setText("");
                    }
                }
            }
        });
        mf.tableLocations.setModel(locationsTableModel);
        mf.tableLocations.getColumnModel().getColumn(0).setWidth(context.preferences.getLibrariesLocationColumnWidth());
        mf.tableLocations.getColumnModel().getColumn(0).setPreferredWidth(context.preferences.getLibrariesLocationColumnWidth());
        mf.tableLocations.getColumnModel().getColumn(1).setWidth(context.preferences.getLibrariesMinimumSizeColumnWidth());
        mf.tableLocations.getColumnModel().getColumn(1).setPreferredWidth(context.preferences.getLibrariesMinimumSizeColumnWidth());

        // bibliography tab
        biblioLibrariesTableModel = new BiblioLibrariesTableModel(context, displayName, null);
        biblioLibrariesTableModel.setColumnCount(1);
        mf.tableBiblioLibraries.setModel(biblioLibrariesTableModel);

        mf.tableBiblioLibraries.getTableHeader().setUI(null);
        mf.tableBiblioLibraries.setTableHeader(null);
        mf.scrollPaneBiblioLibraries.setColumnHeaderView(null);

        ListSelectionModel blsm = mf.tableBiblioLibraries.getSelectionModel();
        blsm.addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent)
            {
                if (!loading && !listSelectionEvent.getValueIsAdjusting())
                {
                    ListSelectionModel sm = (ListSelectionModel) listSelectionEvent.getSource();
                    int index = sm.getMinSelectionIndex();
                    if (index >= 0 && index != currentLibraryIndex)
                    {
                        currentLibraryIndex = index;
                        currentSourceIndex = 0;
                        mf.labelStatusMiddle.setText("");
                        loadSources();
                    }
                }
            }
        });

        listSourcesModel = new DefaultListModel();
        mf.listSources.setModel(listSourcesModel);

        // Make Mode objects
        //  * library
        //  * hint server
        //  * targets
        // See Cards
        modes = new Mode[3];
        modes[0] = new Mode(context.cfg.gs("Libraries.library"), Cards.Library);
        modes[1] = new Mode(context.cfg.gs("Libraries.hint.server"), Cards.HintServer);
        modes[2] = new Mode(context.cfg.gs("Libraries.targets"), Cards.Targets);

        // make New combobox
        comboBoxMode = new JComboBox<>();
        comboBoxMode.setModel(new DefaultComboBoxModel<>(new Mode[]{}));
        comboBoxMode.removeAllItems();
        for (Mode m : modes)
        {
            comboBoxMode.addItem(m);
        }

        numberFilter = new NumberFilter();
        setNumberFilter(mf.textFieldTimeout);

        initializeControls();
        loadConfigurations();
        configItems.requestFocus();
    }

    private void initializeControls()
    {
        // buttons
        mf.cancelButton.addActionListener(e -> actionUndoClicked(e));
        mf.buttonCopy.addActionListener(e -> actionCopyClicked(e));
        mf.buttonDelete.addActionListener(e -> actionDeleteClicked(e));
        mf.buttonNew.addActionListener(e -> actionNewClicked(e));
        mf.saveButton.addActionListener(e -> actionOkClicked(e));
        mf.labelLibrariesHelp.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                actionHelpClicked(e);
            }
        });

        // === general tab ==========================================
        mf.textFieldKey.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                genericTextFieldFocusLost(e);
            }
        });
        mf.textFieldKey.addActionListener(e -> genericAction(e));

        mf.buttonLibraryGenerateKey.addActionListener(e -> actionGenerateUUIDClicked(e));

        mf.textFieldHost.addActionListener(e -> genericAction(e));
        mf.textFieldHost.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                genericTextFieldFocusLost(e);
            }
        });

        mf.textFieldListen.addActionListener(e -> genericAction(e));
        mf.textFieldListen.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                genericTextFieldFocusLost(e);
            }
        });

        mf.textFieldTimeout.addActionListener(e -> genericAction(e));
        mf.textFieldTimeout.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                genericTextFieldFocusLost(e);
            }
        });

        mf.comboBoxFlavor.addActionListener(e -> genericAction(e));
        mf.checkBoxCase.addActionListener(e -> genericAction(e));

        mf.checkBoxTempDated.addActionListener(e -> genericAction(e));
        mf.textFieldTempLocation.addActionListener(e -> genericAction(e));
        mf.textFieldTempLocation.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                genericTextFieldFocusLost(e);
            }
        });
        mf.buttonLibrarySelectTempLocation.addActionListener(e -> actionSelectTempLocationClicked(e));

        mf.checkBoxTerminalAllowed.addActionListener(e -> genericAction(e));

        mf.buttonLibrariesAddIgnore.addActionListener(e -> actionIgnorePatternAdd(e));
        mf.buttonLibrariesRemoveIgnore.addActionListener(e -> actionIgnorePatternRemove(e));

        // locations tab ==========================================

        mf.buttonAddLocation.addActionListener(e -> actionLocationAddClicked(e));
        mf.buttonRemoveLocation.addActionListener(e -> actionLocationsRemove(e));

        // bibliography tab ==========================================

        mf.buttonAddLibrary.addActionListener(e -> actionLibraryAdd(e));
        mf.buttonRemoveLibrary.addActionListener(e -> actionLibraryRemove(e));

        mf.buttonAddSource.addActionListener(e -> actionSourcesAddClicked(e));

        TextIcon t1 = new TextIcon(mf.buttonUpSource, ">", TextIcon.Layout.HORIZONTAL);
        mf.buttonUpSource.setText("");
        RotatedIcon r1 = new RotatedIcon(t1, RotatedIcon.Rotate.UP);
        mf.buttonUpSource.setIcon(r1);
        mf.buttonUpSource.addActionListener(e -> actionSourcesUp(e));

        t1 = new TextIcon(mf.buttonDownSource, ">", TextIcon.Layout.HORIZONTAL);
        mf.buttonDownSource.setText("");
        r1 = new RotatedIcon(t1, RotatedIcon.Rotate.DOWN);
        mf.buttonDownSource.setIcon(r1);
        mf.buttonDownSource.addActionListener(e -> actionSourcesDown(e));

        mf.buttonRemoveSource.addActionListener(e -> actionSourcesRemove(e));
        mf.buttonAddMultiSource.addActionListener(e -> actionSourcesMultiClicked(e));
    }

    private void loadBibliographyTab()
    {
        mf.tableBiblioLibraries.removeAll();
        biblioLibrariesTableModel.getDataVector().removeAllElements();
        if (!loading && currentConfigIndex >= 0 && currentConfigIndex < configModel.getRowCount())
        {
            loading = true;
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            biblioLibrariesTableModel.setLibMeta(libMeta);

            Library[] libraries = libMeta.repo.getLibraryData().libraries.bibliography;
            if (!libMeta.isDataChanged() && libraries != null && libraries.length > 0)
                Arrays.sort(libraries);

            if (libraries != null && libraries.length > 0)
            {
                for (int i = 0; i < libraries.length; ++i)
                {
                    biblioLibrariesTableModel.addRow(new Object[]{libraries[i]});
                }
            }

            biblioLibrariesTableModel.fireTableDataChanged();

            if (biblioLibrariesTableModel.getRowCount() == 0)
            {
                mf.buttonRemoveLibrary.setEnabled(false);

                mf.buttonAddSource.setEnabled(false);
                mf.buttonAddMultiSource.setEnabled(false);
                mf.buttonUpSource.setEnabled(false);
                mf.buttonDownSource.setEnabled(false);
                mf.buttonRemoveSource.setEnabled(false);
            }
            else
            {
                mf.buttonRemoveLibrary.setEnabled(true);
                mf.buttonAddSource.setEnabled(true);
                mf.buttonAddMultiSource.setEnabled(true);
                if (((Library) biblioLibrariesTableModel.getValueAt(currentLibraryIndex, 0)).sources.length > 1)
                {
                    mf.buttonUpSource.setEnabled(true);
                    mf.buttonDownSource.setEnabled(true);
                }
                // TODO apply same logic to other add/up/down/remove code
                if (((Library) biblioLibrariesTableModel.getValueAt(currentLibraryIndex, 0)).sources.length > 0)
                    mf.buttonRemoveSource.setEnabled(true);

                mf.tableBiblioLibraries.changeSelection(currentLibraryIndex, 0, false, false);
            }
            loading = false;

            loadSources();
        }
    }

    private void loadConfigurations()
    {
        loading = true;
        currentConfigIndex = 0;
        currentLocationIndex = 0;
        currentLibraryIndex = 0;
        currentSourceIndex = 0;
        Repositories repositories = getRepositories();
        if (repositories.getList() != null)
        {
            for (RepoMeta repoMeta : repositories.getList())
            {
                LibMeta libMeta = new LibMeta();
                libMeta.description = repoMeta.description;
                libMeta.key = repoMeta.key;
                libMeta.path = repoMeta.path;
                libMeta.card = Cards.Library;   // only using Library right now
                libMeta.dataHasChanged = false;

                try
                {
                    // load each repo
                    libMeta.repo = new Repository(context, -1);
                    libMeta.repo.read(libMeta.path, "a", false);
                }
                catch (Exception e)
                {
                    String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
                    logger.error(msg);
                    JOptionPane.showMessageDialog(mf, msg, displayName, JOptionPane.ERROR_MESSAGE);
                }

                configModel.addRow(new Object[]{libMeta});
            }
        }
        loading = false;

        if (configModel.getRowCount() == 0)
        {
            mf.buttonCopy.setEnabled(false);
            mf.buttonDelete.setEnabled(false);
            mf.saveButton.setEnabled(false);
        }
        else
        {
            currentConfigIndex = 0;
            configItems.setRowSelectionInterval(0, 0);
            loadGeneralTab();
            configItems.requestFocus();
        }
    }

    private void loadGeneralTab()
    {
        if (!loading && currentConfigIndex >= 0 && currentConfigIndex < configModel.getRowCount())
        {
            loading = true;
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            ((CardLayout) mf.generalTab.getLayout()).show(mf.generalTab, libMeta.card.name());

            Repository repo = libMeta.repo;
            Libraries libraries = repo.getLibraryData().libraries;
            mf.textFieldKey.setText(libraries.key);
            mf.textFieldHost.setText(libraries.host);
            mf.textFieldListen.setText(libraries.listen);
            mf.textFieldTimeout.setText(Integer.toString(libraries.timeout));
            int index = libraries.flavor.toLowerCase().equals("linux") ? 0 :
                    (libraries.flavor.toLowerCase().equals("mac") ? 1 :
                            (libraries.flavor.toLowerCase().equals("windows") ? 2 : -1));
            assert (index >= 0);
            mf.comboBoxFlavor.setSelectedIndex(index);
            if (libraries.case_sensitive != null)
                mf.checkBoxCase.setSelected(libraries.case_sensitive);
            if (libraries.temp_dated != null)
                mf.checkBoxTempDated.setSelected(libraries.temp_dated);
            mf.textFieldTempLocation.setText((libraries.temp_location));

            if (libraries.terminal_allowed != null)
                mf.checkBoxTerminalAllowed.setSelected(libraries.terminal_allowed);

            if (libraries.ignore_patterns != null)
            {
                ArrayList<String> ignores = new ArrayList<>();
                for (int i = 0; i < libraries.ignore_patterns.length; ++i)
                {
                    ignores.add(libraries.ignore_patterns[i]);
                }

                DefaultListModel<String> model = new DefaultListModel<String>();
                if (ignores.size() > 0)
                {
                    Collections.sort(ignores);
                    for (String element : ignores)
                    {
                        model.addElement(element);
                    }
                }
                else
                {
                    mf.listLibrariesIgnorePatterns.removeAll();
                    model.removeAllElements();
                    model.clear();
                }
                mf.listLibrariesIgnorePatterns.setModel(model);
                mf.scrollPaneLibrariesIgnorePatterns.setViewportView(mf.listLibrariesIgnorePatterns);
                mf.listLibrariesIgnorePatterns.setSelectionInterval(0, 0);
            }
            loading = false;

            loadLocationsTab();

            if (mf.librariesConfigItems.isEditing())
                mf.librariesConfigItems.getCellEditor().stopCellEditing();

            updateState();
        }
    }

    private void loadLocationsTab()
    {
        locationsTableModel.getDataVector().removeAllElements();
        if (!loading && currentConfigIndex >= 0 && currentConfigIndex < configModel.getRowCount())
        {
            loading = true;
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            Location[] locations = libMeta.repo.getLibraryData().libraries.locations;

            locationsTableModel.setLocations(locations);

            if (locations.length > 0)
                Arrays.sort(locations);

            locationsTableModel.fireTableDataChanged();

            if (locationsTableModel.getRowCount() == 0)
            {
                mf.buttonAddLocation.setEnabled(true);
                mf.buttonRemoveLocation.setEnabled(false);
            }
            else
            {
                mf.tableLocations.changeSelection(currentLocationIndex, 0, false, false);
                mf.buttonAddLocation.setEnabled(true);
                mf.buttonRemoveLocation.setEnabled(true);
            }
            loading = false;

            loadBibliographyTab();
        }
    }

    private void loadSources()
    {
        mf.listSources.removeAll();
        listSourcesModel.removeAllElements();
        if (!loading && currentConfigIndex >= 0 && currentConfigIndex < configModel.getRowCount())
        {
            loading = true;
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            if (this.currentLibraryIndex < libMeta.repo.getLibraryData().libraries.bibliography.length)
            {
                Library lib = libMeta.repo.getLibraryData().libraries.bibliography[this.currentLibraryIndex];

                if (lib != null && lib.sources.length > 0)
                {
                    for (int i = 0; i < lib.sources.length; ++i)
                    {
                        listSourcesModel.addElement(lib.sources[i]);
                    }

                    mf.buttonRemoveLibrary.setEnabled(true);
                    mf.buttonAddSource.setEnabled(true);
                    mf.buttonAddMultiSource.setEnabled(true);
                    if (lib.sources.length > 1)
                    {
                        mf.buttonUpSource.setEnabled(true);
                        mf.buttonDownSource.setEnabled(true);
                    }
                    mf.buttonRemoveSource.setEnabled(true);
                    if (currentSourceIndex >= 0)
                        mf.listSources.setSelectedIndex(currentSourceIndex);
                }
            }
            loading = false;
        }
    }

    private boolean saveConfigurations()
    {
        LibMeta libMeta = null;
        boolean changes = false;
        try
        {
            // write/update changed tool JSON configuration files
            for (int i = 0; i < configModel.getRowCount(); ++i)
            {
                libMeta = (LibMeta) configModel.getValueAt(i, 0);
                if (libMeta.isDataChanged())
                {
                    if (libMeta.repo.getJsonFilename() == null || libMeta.repo.getJsonFilename().length() == 0)
                    {
                        libMeta.repo.setJsonFilename(getDirectoryPath() + System.getProperty("file.separator") + libMeta.description + ".json");
                    }
                    Arrays.sort(libMeta.repo.getLibraryData().libraries.bibliography);
                    libMeta.repo.write();
                    changes = true;
                }
                libMeta.setDataHasChanged(false);
            }

            // remove any deleted tools JSON configuration file
            for (int i = 0; i < deletedLibraries.size(); ++i)
            {
                libMeta = (LibMeta) deletedLibraries.get(i);
                File file = new File(libMeta.path);
                if (file.exists())
                {
                    file.delete();
                }
                libMeta.setDataHasChanged(false);
                changes = true;
            }
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (context != null)
            {
                logger.error(msg);
                JOptionPane.showMessageDialog(mf, msg, displayName, JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }

        if (deletedLibraries.size() > 0)
            deletedLibraries = new ArrayList<LibMeta>();

        configModel.setRowCount(0);
        biblioLibrariesTableModel.setRowCount(0);
        loadConfigurations();
        return changes;
    }

    public void savePreferences()
    {
        context.preferences.setLibrariesDividerLocation(mf.splitPaneLibs.getDividerLocation());
        context.preferences.setLibrariesBiblioDividerLocation(mf.splitPanelBiblio.getDividerLocation());
    }

    public void selectLastTab()
    {
        if (lastTab == 0)
        {
            mf.generalTab.requestFocus();
        }
        else if (lastTab == 1)
        {
            mf.locationsTab.requestFocus();
        }
        else if (lastTab == 2)
        {
            mf.bibliographyTab.requestFocus();
        }
    }

    private void setNumberFilter(JTextField field)
    {
        PlainDocument pd = (PlainDocument) field.getDocument();
        pd.setDocumentFilter(numberFilter);
    }

    public void setPromptedPath(String promptedPath)
    {
        this.promptedPath = promptedPath;
    }

    public void tabbedPaneLibrarySpacesStateChanged(ChangeEvent changeEvent)
    {
        mf.labelStatusMiddle.setText(context.cfg.gs(""));
        int index = mf.tabbedPaneLibrarySpaces.getSelectedIndex();
        lastTab = index;
        if (index == 0)
        {
            mf.generalTab.requestFocus();
            mf.textFieldKey.select(0, 0); // fixes odd problem of the field being highlighted when it's not selected
        }
        else if (index == 1)
        {
            mf.locationsTab.requestFocus();
        }
        else if (index == 2)
        {
            mf.bibliographyTab.requestFocus();
        }
    }

    private void updateOnChange(Object source)
    {
        String name = null;
        int selection = -1;
        if (source != null && !loading)
        {
            currentConfigIndex = configItems.getSelectedRow();
            if (currentConfigIndex >= 0)
            {
                LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
                if (source instanceof JTextField)
                {
                    String current = null;
                    JTextField tf = (JTextField) source;
                    name = tf.getName();
                    switch (name.toLowerCase())
                    {
                        case "key":
                            current = libMeta.key;
                            libMeta.key = tf.getText();
                            libMeta.repo.getLibraryData().libraries.key = tf.getText();
                            break;
                        case "host":
                            current = libMeta.repo.getLibraryData().libraries.host;
                            libMeta.repo.getLibraryData().libraries.host = tf.getText();
                            break;
                        case "listen":
                            current = libMeta.repo.getLibraryData().libraries.listen;
                            libMeta.repo.getLibraryData().libraries.listen = tf.getText();
                            break;
                        case "timeout":
                            current = Integer.toString(libMeta.repo.getLibraryData().libraries.timeout);
                            libMeta.repo.getLibraryData().libraries.timeout = Integer.valueOf(tf.getText());
                            break;
                        case "templocation":
                            current = libMeta.repo.getLibraryData().libraries.temp_location;
                            libMeta.repo.getLibraryData().libraries.temp_location = tf.getText();
                            break;
                    }
                    if (tf != null && current != null && !current.equals(tf.getText()))
                    {
                        libMeta.setDataHasChanged();
                        updateState();
                    }
                }
                else if (source instanceof JCheckBox)
                {
                    boolean state = false;
                    JCheckBox cb = (JCheckBox) source;
                    name = cb.getName();
                    switch (name.toLowerCase())
                    {
                        case "case":
                            state = libMeta.repo.getLibraryData().libraries.case_sensitive;
                            libMeta.repo.getLibraryData().libraries.case_sensitive = cb.isSelected();
                            break;
                        case "tempdated":
                            state = libMeta.repo.getLibraryData().libraries.temp_dated;
                            libMeta.repo.getLibraryData().libraries.temp_dated = cb.isSelected();
                            break;
                        case "terminalallowed":
                            state = libMeta.repo.getLibraryData().libraries.terminal_allowed;
                            libMeta.repo.getLibraryData().libraries.terminal_allowed = cb.isSelected();
                            break;
                    }
                    if (state != cb.isSelected())
                    {
                        libMeta.setDataHasChanged();
                        updateState();
                    }
                }
                else if (source instanceof JComboBox)
                {
                    JComboBox combo = (JComboBox) source;
                    name = combo.getName();
                    int current = -1;
                    int index = combo.getSelectedIndex();
                    String value = "";
                    switch (name.toLowerCase())
                    {
                        case "flavor":
                            current = libMeta.repo.getLibraryData().libraries.flavor.toLowerCase().equals("linux") ? 0 :
                                    (libMeta.repo.getLibraryData().libraries.flavor.toLowerCase().equals("mac") ? 1 : 2);
                            libMeta.repo.getLibraryData().libraries.flavor = (String) combo.getSelectedItem();
                            break;
                    }
                    if (index != current)
                    {
                        libMeta.setDataHasChanged();
                        updateState();
                    }
                }
            }
        }
    }

    private void updateState()
    {
        if (configModel.getRowCount() > 0)
        {
            LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
            if (libMeta.card == Cards.Library)
            {
            }
            else if (libMeta.card == Cards.HintServer)
            {
            }
            else if (libMeta.card == Cards.Targets)
            {
            }
        }
    }

// ================================================================================================================

    protected class LibMeta extends RepoMeta
    {
        public boolean dataHasChanged = false;
        public Repository repo;
        Cards card;

        @Override
        public LibMeta clone()
        {
            LibMeta libMeta = new LibMeta();
            libMeta.description = this.description;
            libMeta.key = this.key;
            libMeta.path = this.path;
            //
            libMeta.card = this.card;
            libMeta.dataHasChanged = true;
            libMeta.repo = this.repo;
            return libMeta;
        }

        public boolean isDataChanged()
        {
            return dataHasChanged;
        }

        public void setDataHasChanged(boolean sense)
        {
            dataHasChanged = sense;
        }

        public void setDataHasChanged()
        {
            dataHasChanged = true;
        }
    }

// ================================================================================================================

    private class Mode
    {
        public Cards card;
        public String description;

        public Mode(String description, Cards card)
        {
            this.description = description;
            this.card = card;
        }

        @Override
        public String toString()
        {
            return description;
        }
    }

// ================================================================================================================

}
