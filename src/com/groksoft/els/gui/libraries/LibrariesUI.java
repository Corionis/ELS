package com.groksoft.els.gui.libraries;

import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.MainFrame;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.gui.util.NumberFilter;
import com.groksoft.els.repository.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

/*
    IDEA
        + When adding a Target or Location use a dialog with a checkbox
          for each library and an "Add to All" checkbox to select which libraries
          should have that storage space added to it.

    QUESTION
        + Targets?
            = Research how they ACTUALLY work.
            - Should Targets be a tab for a Library with a top check-box for enable?
            - Or should Targets be removed entirely??
 */

@SuppressWarnings(value = "unchecked")

public class LibrariesUI
{
    private JComboBox comboBoxMode;
    private JTable configItems;
    private Context context;
    private ConfigModel configModel;
    private int currentConfigIndex = -1;
    private String currentLibraryName = "";
    protected ArrayList<LibMeta> deletedLibraries;
    private String displayName;
    private boolean fileAny = false;
    private boolean fileDirectoryOnly = false;
    private boolean fileMustExist = false;
    private NavHelp helpDialog;
    private File lastFile;
    private boolean loading = false;
    private Logger logger = LogManager.getLogger("applog");
    private MainFrame mf;
    private Mode[] modes;
    private NumberFilter numberFilter;

    public static enum Cards { Library, HintServer, Targets }

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

    private void actionCancelClicked(ActionEvent e)
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
                loadOptions();
                configItems.editCellAt(currentConfigIndex, 0);
                configItems.changeSelection(currentConfigIndex, currentConfigIndex, false, false);
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

                configModel.removeRow(index);
                if (index > configModel.getRowCount() - 1)
                    index = configModel.getRowCount() - 1;
                currentConfigIndex = index;
                configModel.fireTableDataChanged();
                if (configModel.getRowCount() > 0)
                {
                    configItems.changeSelection(index, 0, false, false);
                    loadOptions();
                }
                else
                {
                    ((CardLayout) mf.tabbedPaneLibrarySpaces.getLayout()).show(mf.tabbedPaneLibrarySpaces, "cardGettingStarted");
                    mf.labelLibaryType.setText("");
                    mf.buttonCopy.setEnabled(false);
                    mf.buttonDelete.setEnabled(false);
                    mf.saveButton.setEnabled(false);
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
                //mf.textFieldKey.requestFocus();
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
            //ConfirmDialog(mf, params, displayName, JOptionPane.OK_CANCEL_OPTION);
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
                loadOptions();
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
                        loadOptions();
                    }
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
                loadOptions();

                configItems.editCellAt(currentConfigIndex, 0);
                configItems.changeSelection(currentConfigIndex, currentConfigIndex, false, false);
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

    private void actionSaveClicked(ActionEvent e)
    {
        int cci = currentConfigIndex;
        saveConfigurations();
        savePreferences();
        if (configModel.getRowCount() - 1 < cci)
            cci = configModel.getRowCount() - 1;
        configItems.setRowSelectionInterval(cci, cci);
    }

    private void cancelChanges()
    {
        int cci = currentConfigIndex;
        if (deletedLibraries.size() > 0)
            deletedLibraries = new ArrayList<LibMeta>();

        configModel.setRowCount(0);
        loadConfigurations();
        if (configModel.getRowCount() - 1 < cci)
            cci = configModel.getRowCount() - 1;
        configItems.setRowSelectionInterval(cci, cci);

        mf.labelStatusMiddle.setText(context.cfg.gs("Libraries.libraries.changes.cancelled"));
    }

    public void cardShown(ComponentEvent e)
    {
        // TODO add your code here
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
                //logger.warn("unsaved changes in " + ((LibMeta) configModel.getValueAt(i, 0)).getConfigName());
                return true;
            }
        }
        return false;
    }

    private void filePicker(JButton button)
    {
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
                if (fileDirectoryOnly && !file.isDirectory())
                    return false;
                if (!fileAny)
                    return (file.getName().toLowerCase().endsWith(".json"));
                return true;
            }

            @Override
            public String getDescription()
            {
                String desc = "";
                switch (button.getName().toLowerCase())
                {
                    case "templocation":
                        desc = context.cfg.gs("Libraries.temp.location");
                        fileAny = true;
                        fileDirectoryOnly = true;
                        fileMustExist = true;
                        //fc.setFileHidingEnabled(true);
                        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        break;
                }
                return desc;
            }
        });

        String fileName = "";
        switch (button.getName().toLowerCase())
        {
            case "templocation":
                fileName = mf.textFieldTempLocation.getText();
                fc.setAcceptAllFileFilterUsed(false);
                break;
        }

        File dir;
        File file;
        if (fileName.length() > 0)
        {
            dir = new File(filePickerDirectory(fileName));
            fc.setCurrentDirectory(dir.getAbsoluteFile());

            file = new File(fileName);
            fc.setSelectedFile(file);
        }
        else if (lastFile != null)
        {
            fc.setCurrentDirectory(lastFile);
        }
        else
        {
            // default to ELS home
            fc.setCurrentDirectory(new File(context.cfg.getWorkingDirectory()));
        }

        fc.setDialogType(fileMustExist ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG);

        while (true)
        {
            int selection = fc.showOpenDialog(mf);
            if (selection == JFileChooser.APPROVE_OPTION)
            {

                lastFile = fc.getCurrentDirectory();
                file = fc.getSelectedFile();

                // sanity checks
                if (!fileDirectoryOnly && file.isDirectory())
                {
                    JOptionPane.showMessageDialog(mf,
                            context.cfg.gs("Navigator.open.error.select.a.file.only"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                if (fileMustExist && !file.exists())
                {
                    JOptionPane.showMessageDialog(mf,
                            context.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                            displayName, JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                // make path relative if possible
                String path = "";
                if (file.getPath().startsWith(context.cfg.getWorkingDirectory()))
                    path = file.getPath().substring(context.cfg.getWorkingDirectory().length() + 1);
                else
                    path = file.getPath();

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

    public String getConfigName()
    {
        return currentLibraryName;
    }

    public String getDirectoryPath()
    {
        String path = System.getProperty("user.dir") + System.getProperty("file.separator") + "libraries";
        return path;
    }

    public ArrayList<LibMeta> getDeletedLibraries()
    {
        return deletedLibraries;
    }

    public String getFullPath()
    {
        String path = getDirectoryPath() + System.getProperty("file.separator") +
                Utils.scrubFilename(getConfigName()) + ".json";
        return path;
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
                    if (index != currentConfigIndex && currentConfigIndex >= 0)
                    {
                        currentConfigIndex = index;
                        loadOptions();
                    }
                }
            }
        });
        configItems.setTableHeader(null);

        // setup the right-side tab handler
        mf.tabbedPaneLibrarySpaces.addChangeListener(e -> tabbedPaneLibrarySpacesStateChanged(e));

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
    }

    private void initializeControls()
    {
        // buttons
        mf.cancelButton.addActionListener(e -> actionCancelClicked(e));
        mf.buttonCopy.addActionListener(e -> actionCopyClicked(e));
        mf.buttonDelete.addActionListener(e -> actionDeleteClicked(e));
        mf.buttonNew.addActionListener(e -> actionNewClicked(e));
        mf.saveButton.addActionListener(e -> actionSaveClicked(e));
        mf.labelLibrariesHelp.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                actionHelpClicked(e);
            }
        });

        // configuration (libraries) list
        mf.librariesConfigItems.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent mouseEvent)
            {
                super.mouseClicked(mouseEvent);
            }
        });

        // general tab
        mf.textFieldKey.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                genericTextFieldFocusLost(e);
            }
        });
        mf.textFieldKey.addActionListener(e -> genericAction(e));

        mf.buttonLibraryGenerateKey.addActionListener(e -> actionGenerateUUIDClicked(e));

        mf.textFieldHost.addActionListener(e -> genericAction(e));
        mf.textFieldHost.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                genericTextFieldFocusLost(e);
            }
        });

        mf.textFieldListen.addActionListener(e -> genericAction(e));
        mf.textFieldListen.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
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
        mf.buttonLibrarySelectTempLocation.addActionListener(e -> filePicker(mf.buttonLibrarySelectTempLocation));

        mf.checkBoxTerminalAllowed.addActionListener(e -> genericAction(e));

        mf.buttonLibrariesAddIgnore.addActionListener(e -> actionIgnorePatternAdd(e));
        mf.buttonLibrariesRemoveIgnore.addActionListener(e -> actionIgnorePatternRemove(e));

    }

    private void initNewCard()
    {
        loading = true;
        //LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);

        updateState();
        loading = false;
    }

    public Repositories getRepositories()
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

    private void loadConfigurations()
    {
        currentConfigIndex = 0;
        Repositories repositories = getRepositories();
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
                libMeta.repo.read(libMeta.path, false);
            }
            catch (Exception e)
            {
                String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
                logger.error(msg);
                JOptionPane.showMessageDialog(mf, msg, displayName, JOptionPane.ERROR_MESSAGE);
            }

            configModel.addRow(new Object[]{libMeta});
        }

        if (configModel.getRowCount() == 0)
        {
            mf.buttonCopy.setEnabled(false);
            mf.buttonDelete.setEnabled(false);
            mf.saveButton.setEnabled(false);
        }
        else
        {
            currentConfigIndex = 0;
            loadOptions();
            configItems.requestFocus();
            configItems.setRowSelectionInterval(0, 0);
        }
    }

    private void loadOptions()
    {
        if (currentConfigIndex >= 0 && currentConfigIndex < configModel.getRowCount())
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
            if (index < 0)
            {
                // TODO
            }
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
            updateState();
            loading = false;
        }
    }

    private void saveConfigurations()
    {
        LibMeta libMeta = null;
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
                    libMeta.repo.write();
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
        loadConfigurations();

        mf.labelStatusMiddle.setText(context.cfg.gs("Libraries.libraries.changes.saved"));
    }

    public void savePreferences()
    {
        context.preferences.setLibrariesDividerLocation(mf.splitPaneLibs.getDividerLocation());
        context.preferences.setLibrariesBiblioDividerLocation(mf.splitPanelBiblio.getDividerLocation());
    }


    private void setNumberFilter(JTextField field)
    {
        PlainDocument pd = (PlainDocument) field.getDocument();
        pd.setDocumentFilter(numberFilter);
    }

    public void tabbedPaneLibrarySpacesStateChanged(ChangeEvent changeEvent)
    {
        int index = mf.tabbedPaneLibrarySpaces.getSelectedIndex();
        if (index == 0)
        {
            mf.generalTab.requestFocus();
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
        LibMeta libMeta = (LibMeta) configModel.getValueAt(currentConfigIndex, 0);
        if (libMeta.card == Cards.Library)
        {
        } else if (libMeta.card == Cards.HintServer)
        {
        } else if (libMeta.card == Cards.Targets)
        {
        }
    }

    // ================================================================================================================

    protected class LibMeta extends RepoMeta
    {
        Cards card;
        public boolean dataHasChanged = false;
        public Repository repo;

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
        String description;
        Cards card;

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
