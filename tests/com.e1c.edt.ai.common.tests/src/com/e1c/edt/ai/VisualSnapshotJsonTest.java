/**
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.e1c.edt.ai.assistent.model.ClipboardInfo;
import com.e1c.edt.ai.assistent.model.VisualEditorInfo;
import com.e1c.edt.ai.assistent.model.VisualField;
import com.e1c.edt.ai.assistent.model.VisualSnapshot;
import com.e1c.edt.ai.assistent.model.VisualWindow;

/**
 * Verifies the Gson wire format of {@link VisualSnapshot}: the snake_case field names are part of
 * the GetVisualContext tool contract consumed by skills and the LLM.
 */
@SuppressWarnings("nls")
public class VisualSnapshotJsonTest
{
    private final IJson json = new Json();

    @Test
    public void serializesSnakeCaseFieldNames()
    {
        var snapshot = createSnapshot();

        var content = json.serialize(snapshot);

        assertTrue(content.contains("\"windows\""));
        assertTrue(content.contains("\"is_active\""));
        assertTrue(content.contains("\"is_modal\""));
        assertTrue(content.contains("\"is_dialog\""));
        assertTrue(content.contains("\"is_focused\""));
        assertTrue(content.contains("\"is_checked\""));
        assertTrue(content.contains("\"selected_text\""));
        assertTrue(content.contains("\"options\""));
        assertTrue(content.contains("\"columns\""));
        assertTrue(content.contains("\"rows\""));
        assertTrue(content.contains("\"active_editor\""));
        assertTrue(content.contains("\"visible_text\""));
        assertTrue(content.contains("\"cursor_line\""));
        assertTrue(content.contains("\"cursor_column\""));
        assertTrue(content.contains("\"clipboard\""));
    }

    @Test
    public void omitsNullFields()
    {
        var snapshot = new VisualSnapshot();
        var window = new VisualWindow();
        window.title = "Window";
        snapshot.windows = List.of(window);

        var content = json.serialize(snapshot);

        assertFalse(content.contains("is_active"));
        assertFalse(content.contains("is_truncated"));
        assertFalse(content.contains("active_editor"));
        assertFalse(content.contains("clipboard"));
    }

    @Test
    public void roundTripsThroughJson()
    {
        var snapshot = createSnapshot();

        var restored = json.deserialize(json.serialize(snapshot), VisualSnapshot.class).orElseThrow();

        assertEquals(1, restored.windows.size());
        var window = restored.windows.get(0);
        assertEquals("New Data Processor", window.title);
        assertEquals(Boolean.TRUE, window.isActive);
        assertEquals(Boolean.TRUE, window.isModal);
        var field = window.fields.get(0);
        assertEquals("Name", field.name);
        assertEquals("checkbox", window.fields.get(1).kind);
        assertEquals(Boolean.TRUE, window.fields.get(1).isChecked);
        assertEquals("Return", field.selectedText);
        assertEquals(List.of("Customer", "CatalogRef"), window.fields.get(2).rows.get(0));
        assertEquals("Module.bsl", restored.activeEditor.title);
        assertEquals(Integer.valueOf(12), restored.activeEditor.cursorLine);
        assertEquals("CopiedText", restored.clipboard.text);
    }

    @Test
    public void isEmptyReflectsContent()
    {
        assertTrue(new VisualSnapshot().isEmpty());

        var snapshot = new VisualSnapshot();
        snapshot.clipboard = new ClipboardInfo();
        assertFalse(snapshot.isEmpty());
    }

    private VisualSnapshot createSnapshot()
    {
        var snapshot = new VisualSnapshot();

        var window = new VisualWindow();
        window.title = "New Data Processor";
        window.isActive = Boolean.TRUE;
        window.isModal = Boolean.TRUE;
        window.isDialog = Boolean.TRUE;

        var nameField = new VisualField();
        nameField.name = "Name";
        nameField.value = "ReturnProcessing";
        nameField.kind = "text";
        nameField.isFocused = Boolean.TRUE;
        nameField.isMultiline = Boolean.FALSE;
        nameField.selectedText = "Return";

        var checkboxField = new VisualField();
        checkboxField.name = "Use standard commands";
        checkboxField.kind = "checkbox";
        checkboxField.isChecked = Boolean.TRUE;

        var tableField = new VisualField();
        tableField.kind = "table";
        tableField.options = List.of("Internal", "External");
        tableField.columns = List.of("Name", "Type");
        tableField.rows = List.of(List.of("Customer", "CatalogRef"), List.of("Amount", "Number"));

        window.fields = List.of(nameField, checkboxField, tableField);
        snapshot.windows = List.of(window);

        var editor = new VisualEditorInfo();
        editor.title = "Module.bsl";
        editor.path = "MyProject/src/Documents/Order/Module.bsl";
        editor.isDirty = Boolean.TRUE;
        editor.visibleText = "Procedure Posting(Cancel, Mode)";
        editor.selectedText = "Cancel";
        editor.cursorLine = 12;
        editor.cursorColumn = 20;
        snapshot.activeEditor = editor;

        var clipboard = new ClipboardInfo();
        clipboard.text = "CopiedText";
        clipboard.path = "MyProject/src/CommonModules/Common/Module.bsl";
        snapshot.clipboard = clipboard;

        return snapshot;
    }
}
