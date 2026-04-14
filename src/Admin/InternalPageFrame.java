package Admin;

import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.plaf.basic.BasicInternalFrameUI;

public class InternalPageFrame extends JInternalFrame {

    public InternalPageFrame() {
        super("", false, false, false, false);
        setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (getUI() instanceof BasicInternalFrameUI) {
            ((BasicInternalFrameUI) getUI()).setNorthPane(null);
        }
    }
}
