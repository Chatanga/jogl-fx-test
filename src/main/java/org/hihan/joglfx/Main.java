package org.hihan.joglfx;

import com.jogamp.opengl.GL;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        //Native.setLibraryPath();
        launch(args);
    }

    private CanvasJOGL canvas;

    private ColoredSquare square;

    private Framebuffer framebuffer;

    @Override
    public void start(Stage primaryStage) {

        Group root = new Group();
        canvas = new CanvasJOGL(300, 300);
        canvas.setDirtyCallback(this::render);
        drawShapes(canvas.getGraphicsContext2D());
        root.getChildren().add(canvas);

        primaryStage.setTitle("OpenGL + JavaFX");
        primaryStage.setScene(new Scene(root, -1, -1, true));
        primaryStage.show();
    }

    private void render(GL gl) {
        //directRender(gl);
        indirectRender(gl);
    }

    private void directRender(GL gl) {
        assert JOGL.isInQuatumRendererThread();

        if (square == null) {
            square = new ColoredSquare(gl.getGL3());
        }

        // Render the content.
        square.display(gl.getGL3());
    }

    private void indirectRender(GL gl) {
        assert JOGL.isInQuatumRendererThread();

        if (square == null) {
            square = new ColoredSquare(gl.getGL3());
        }

        if (updateFramebuffer(gl)) {
            // Render off-screen first.
            Runnable revert = framebuffer.bind(gl);

            // Clear the off-screen buffer.
            gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);

            // Render the content.
            square.display(gl.getGL3());

            // Then copy back the result into the default framebuffer.
            revert.run();
            framebuffer.display(gl);
        } else {
            // Copy back the result into the default framebuffer.
            framebuffer.display(gl);
        }
    }

    private boolean updateFramebuffer(GL gl) {
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
        int width = viewport[2];
        int height = viewport[3];

        if (framebuffer != null) {
            if (framebuffer.getWidth() != width || framebuffer.getHeight() != height) {
                framebuffer.dispose(gl);
                framebuffer = null;
            }
        }

        if (framebuffer == null) {
            framebuffer = new Framebuffer(gl, width, height, false, false);
            return true;
        } else {
            return false;
        }
    }

    /**
     * JavaFX Canvas standard drawing: we register a list of shapes / commands
     * to be translated later (in the Quantum renderer thread) into OpenGL calls
     * (and shaders actually) provided the corresponding accelerated pipeline is
     * used.
     */
    private void drawShapes(GraphicsContext gc) {
        assert Platform.isFxApplicationThread();

        gc.setFill(Color.GREEN);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(5);
        gc.strokeLine(40, 10, 10, 40);
        gc.fillOval(10, 60, 30, 30);
        gc.strokeOval(60, 60, 30, 30);
        gc.fillRoundRect(110, 60, 30, 30, 10, 10);
        gc.strokeRoundRect(160, 60, 30, 30, 10, 10);
        gc.fillArc(10, 110, 30, 30, 45, 240, ArcType.OPEN);
        gc.fillArc(60, 110, 30, 30, 45, 240, ArcType.CHORD);
        gc.fillArc(110, 110, 30, 30, 45, 240, ArcType.ROUND);
        gc.strokeArc(10, 160, 30, 30, 45, 240, ArcType.OPEN);
        gc.strokeArc(60, 160, 30, 30, 45, 240, ArcType.CHORD);
        gc.strokeArc(110, 160, 30, 30, 45, 240, ArcType.ROUND);
        gc.fillPolygon(
                new double[]{10, 40, 10, 40},
                new double[]{210, 210, 240, 240},
                4);
        gc.strokePolygon(
                new double[]{60, 90, 60, 90},
                new double[]{210, 210, 240, 240},
                4);
        gc.strokePolyline(
                new double[]{110, 140, 110, 140},
                new double[]{210, 210, 240, 240},
                4);
    }
}
