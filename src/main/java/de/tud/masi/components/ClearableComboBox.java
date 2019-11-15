package de.tud.masi.components;

import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.ui.ComboBox;

/**
 * This fix allows the ComboBox to be cleared. Taken from
 * https://github.com/vaadin/framework/issues/9047#issuecomment-371487223
 *
 * @author mpt74 (https://github.com/mpt74)
 * @param <T>
 */
@SuppressWarnings("serial")
public class ClearableComboBox<T> extends ComboBox<T> {
    private static final long serialVersionUID = 1L;

    public ClearableComboBox(String in) {
        super(in);
    }

    protected void setSelectedFromServer(T item) {
        String key = itemToKey(item);

        T oldSelection = getSelectedItem().orElse(getEmptyValue());
        doSetSelectedKey(key);

        fireEvent(new SingleSelectionEvent<>(ClearableComboBox.this, oldSelection, false));
    }
}
