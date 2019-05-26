package org.hihan.joglfx;

import com.jogamp.opengl.GL;
import com.sun.javafx.sg.prism.NGCanvas;
import com.sun.javafx.sg.prism.NGNode;
import java.util.function.Consumer;
import javafx.scene.canvas.Canvas;

public class CanvasJOGL extends Canvas {

    public CanvasJOGL() {
    }

    public CanvasJOGL(double width, double height) {
        super(width, height);
    }

    @Override
    public void impl_updatePeer() {
        super.impl_updatePeer();
        NGCanvas peer = impl_getPeer();
    }

    @Override
    protected NGNode impl_createPeer() {
        return new NGCanvasJOGL();
    }

    public void setDirtyCallback(Consumer<GL> dirtyCallback) {
        ((NGCanvasJOGL) impl_getPeer()).setDirtyCallback(dirtyCallback);
    }
}
