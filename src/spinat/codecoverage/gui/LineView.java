package spinat.codecoverage.gui;

import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.View;

public class LineView extends LabelView {

    public LineView(Element elem) {
        super(elem);
    }

    @Override
    public int getBreakWeight(int axis, float pos, float len) {
        if (axis == View.X_AXIS) {
            return View.BadBreakWeight;
        }
        return super.getBreakWeight(axis, pos, len);
    }

    @Override
    public View breakView(int axis, int p0, float pos, float len) {
        return this;
    }
}
