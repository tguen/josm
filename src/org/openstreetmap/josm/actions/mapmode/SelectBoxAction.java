// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

import java.awt.event.KeyEvent;

import static org.openstreetmap.josm.tools.I18n.tr;

public class SelectBoxAction extends SelectAction {

    public SelectBoxAction(MapFrame mapFrame) {
        super(tr("Box select mode"),
                "move/move",
                tr("Select, move, scale and rotate objects"),
                Shortcut.registerShortcut("mapmode:boxselect", tr("Mode: {0}", tr("Box select mode")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE),
                ImageProvider.getCursor("normal", "selection"),
                mapFrame);
    }

    @Override
    public void setLassoMode(boolean lassoMode) {
    }

    @Override
    protected boolean setRepeatedKeySwitchLassoOption() {
        return false;
    }

}
