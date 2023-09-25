package com.corionis.els.gui.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

public class PathFilter extends DocumentFilter
{
    final char[] invalidCharacters = {'\\', '/', ':', '*', '?', '"', '<', '>', '|'};

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException
    {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.insert(offset, string);

        if (test(sb.toString()))
            super.insertString(fb, offset, string, attr);
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException
    {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.delete(offset, offset + length);
        super.remove(fb, offset, length);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
    {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.replace(offset, offset + length, text);

        if (test(sb.toString()))
            super.replace(fb, offset, length, text, attrs);
    }

    private boolean test(String text)
    {
        boolean sense = true;
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; ++i)
        {
            for (int j = 0; j < invalidCharacters.length; ++j)
            {
                if (chars[i] == invalidCharacters[j])
                {
                    sense = false;
                    break;
                }
            }
        }
        return sense;
    }
}
