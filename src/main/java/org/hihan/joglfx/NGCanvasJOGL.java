package org.hihan.joglfx;

import com.jogamp.opengl.GL;
import com.sun.javafx.sg.prism.NGCanvas;
import com.sun.javafx.tk.RenderJob;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.Graphics;
import java.util.Optional;
import java.util.function.Consumer;
import static org.hihan.joglfx.JOGL.getInt;

public class NGCanvasJOGL extends NGCanvas {

    private Consumer<GL> dirtyCallback;

    public void setDirtyCallback(Consumer<GL> dirtyCallback) {
        Toolkit.getToolkit().addRenderJob(
                new RenderJob(() -> this.dirtyCallback = dirtyCallback));
    }

    @Override
    protected void renderContent(Graphics g) {
        // Standar Canvas rendering.
        super.renderContent(g);

        geometryChanged();
        Optional.ofNullable(dirtyCallback).ifPresent(callback -> {
            GL gl = JOGL.getGL();

            System.out.println("FBO: " + getInt(gl, GL.GL_FRAMEBUFFER_BINDING));

            JOGL.saveAllStates();
            try {
                callback.accept(gl);
            } finally {
                JOGL.restoreAllStates();
            }

            JOGL.checkNoError(gl);
        });
    }
}
