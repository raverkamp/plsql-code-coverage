package spinat.codecoverage.gui;

import javax.swing.text.AbstractDocument;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import static javax.swing.text.View.X_AXIS;
import javax.swing.text.ViewFactory;

public class SourceEditorKit extends StyledEditorKit {

    ViewFactory defaultFactory;

    @Override
    public ViewFactory getViewFactory() {
        return defaultFactory;
    }

    public SourceEditorKit(int linedistance) {
        this.defaultFactory = new WrapColumnFactory(linedistance);
    }

//        public MutableAttributeSet getInputAttributes() {
//            MutableAttributeSet mAttrs=super.getInputAttributes();
//            mAttrs.removeAttribute(WrapApp.LINE_BREAK_ATTRIBUTE_NAME);
//            return mAttrs;
//        }
    static class WrapColumnFactory implements ViewFactory {

        final int linedist;

        WrapColumnFactory(int linedist) {
            this.linedist = linedist;
        }

        @Override
        public View create(Element elem) {
            //System.out.println(elem);
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new LineView(elem);
                    // return new LabelView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    // return new NoWrapParagraphView(elem);
                    return new ParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new BoxView(elem, View.Y_AXIS) {
                        @Override
                        protected void baselineLayout(int targetSpan,
                                int axis,
                                int[] offsets,
                                int[] spans) {
                            for (int i = 0; i < offsets.length; i++) {
                                offsets[i] = linedist * (i);
                                spans[i] = linedist;
                            }
                        }

                        @Override
                        protected void layoutMajorAxis(int targetSpan,
                                int axis,
                                int[] offsets,
                                int[] spans) {
                            for (int i = 0; i < offsets.length; i++) {
                                offsets[i] = (i) * linedist;
                                spans[i] = linedist;
                            }
                        }

                        @Override
                        public float getPreferredSpan(int axis) {
                            if (axis == X_AXIS) {
                                return super.getPreferredSpan(axis);
                            } else {
                                return this.getViewCount() * linedist;
                            }
                        }

                        ;
                        @Override
                        public float getMinimumSpan(int axis) {
                            if (axis == X_AXIS) {
                                return super.getMinimumSpan(axis);
                            } else {
                                return this.getViewCount() * linedist;
                            }
                        }

                        @Override
                        public float getMaximumSpan(int axis) {
                            if (axis == X_AXIS) {
                                return super.getMaximumSpan(axis);
                            } else {
                                return this.getViewCount() * linedist;
                            }
                        }
                    };
                    //return new BoxView(elem, View.Y_AXIS);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    return new IconView(elem);
                }
            }

            // default to text display
            return new LabelView(elem);
        }
    }
}
