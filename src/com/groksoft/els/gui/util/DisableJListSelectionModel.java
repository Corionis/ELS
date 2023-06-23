package com.groksoft.els.gui.util;

import javax.swing.*;

/**
 * DisableJListSelectionModel class
 * <p></p>
 * <p>Disables selection in a JList so elements are shown in normal text and
 * selections cannot be made.</p>
 * <p></p>
  * <p>Example: <pre>myJList.setSelectionModel(new DisableJListSelectionModel());</pre></p>
 */
public class DisableJListSelectionModel extends DefaultListSelectionModel
{
    @Override
    public void addSelectionInterval(int index0, int index1)
    {
        super.setSelectionInterval(-1, -1);
    }

    @Override
    public void setSelectionInterval(int index0, int index1)
    {
        super.setSelectionInterval(-1, -1);
    }
}
