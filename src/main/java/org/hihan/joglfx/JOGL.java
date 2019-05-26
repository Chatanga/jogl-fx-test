package org.hihan.joglfx;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import java.nio.IntBuffer;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Consumer;

public class JOGL {

    public interface State {

        void save(GL gl);

        void restore(GL gl);

        default void reset(GL gl) {
            throw new UnsupportedOperationException();
        }
    }

    public static final State BLENDING = new State() {

        private boolean blendEnabled;

        private int blendEquation;

        private int blendSrc;

        private int blendDst;

        @Override
        public void save(GL gl) {
            blendEnabled = getBool(gl, GL.GL_BLEND);
            blendEquation = getInt(gl, GL.GL_BLEND_EQUATION);
            blendSrc = getInt(gl, GL.GL_BLEND_SRC);
            blendDst = getInt(gl, GL.GL_BLEND_DST);
        }

        @Override
        public void restore(GL gl) {
            setEnabled(gl, GL.GL_BLEND, blendEnabled);
            gl.glBlendEquation(blendEquation);
            gl.glBlendFunc(blendSrc, blendDst);
        }

        @Override
        public void reset(GL gl) {
            gl.glDisable(GL.GL_BLEND);
        }
    };

    public static final State CLEAR_COLOR = new State() {

        private final float[] clearColor = new float[4];

        @Override
        public void save(GL gl) {
            gl.glGetFloatv(GL.GL_COLOR_CLEAR_VALUE, clearColor, 0);
        }

        @Override
        public void restore(GL gl) {
            gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        }
    };

    public static final State TRANSFORM = new State() {

        @Override
        public void save(GL gl) {
            GL2 gl2 = gl.getGL2();
            gl2.glMatrixMode(GL2.GL_MODELVIEW);
            gl2.glPushMatrix();
            gl2.glMatrixMode(GL2.GL_PROJECTION);
            gl2.glPushMatrix();
        }

        @Override
        public void restore(GL gl) {
            GL2 gl2 = gl.getGL2();
            gl2.glMatrixMode(GL2.GL_MODELVIEW);
            gl2.glPopMatrix();
            gl2.glMatrixMode(GL2.GL_PROJECTION);
            gl2.glPopMatrix();
        }

        @Override
        public void reset(GL gl) {
            GL2 gl2 = gl.getGL2();
            gl2.glMatrixMode(GL2.GL_MODELVIEW);
            gl2.glLoadIdentity();
            gl2.glMatrixMode(GL2.GL_PROJECTION);
            gl2.glLoadIdentity();
        }
    };

    public static final State PROGRAM = new State() {

        private int currentProgram;

        @Override
        public void save(GL gl) {
            currentProgram = getInt(gl, GL2.GL_CURRENT_PROGRAM);
        }

        @Override
        public void restore(GL gl) {
            gl.getGL2().glUseProgram(currentProgram);
        }

        @Override
        public void reset(GL gl) {
            gl.getGL2().glUseProgram(GL.GL_NONE);
        }
    };

    public static final State DEPTH_TEST = new State() {

        private boolean depthTestEnabled;

        private int depthFunc;

        @Override
        public void save(GL gl) {
            depthTestEnabled = getBool(gl, GL.GL_DEPTH_TEST);
            depthFunc = getInt(gl, GL.GL_DEPTH_FUNC);
        }

        @Override
        public void restore(GL gl) {
            setEnabled(gl, GL.GL_DEPTH_TEST, depthTestEnabled);
            gl.glDepthFunc(depthFunc);
        }

        @Override
        public void reset(GL gl) {
            gl.glDisable(GL.GL_DEPTH_TEST);
        }
    };

    public static final State CULLING = new State() {

        private boolean cullingEnabled;

        private int cullFaceMode;

        @Override
        public void save(GL gl) {
            cullingEnabled = getBool(gl, GL.GL_CULL_FACE);
            cullFaceMode = getInt(gl, GL.GL_CULL_FACE_MODE);
        }

        @Override
        public void restore(GL gl) {
            setEnabled(gl, GL.GL_CULL_FACE, cullingEnabled);
            gl.glCullFace(cullFaceMode);
        }

        @Override
        public void reset(GL gl) {
            gl.glDisable(GL.GL_CULL_FACE);
        }
    };

    public static class TextureState implements State {

        private final int index;

        private boolean enabled;

        private int textureObject;

        public TextureState(int index) {
            this.index = index;
        }

        @Override
        public void save(GL gl) {
            int oldActiveTexture = getInt(gl, GL.GL_ACTIVE_TEXTURE);
            gl.glActiveTexture(GL.GL_TEXTURE0 + index);
            enabled = getBool(gl, GL.GL_TEXTURE_2D);
            textureObject = getInt(gl, GL.GL_TEXTURE_BINDING_2D);
            gl.glActiveTexture(oldActiveTexture);
        }

        @Override
        public void restore(GL gl) {
            int oldActiveTexture = getInt(gl, GL.GL_ACTIVE_TEXTURE);
            gl.glActiveTexture(GL.GL_TEXTURE0 + index);
            setEnabled(gl, GL.GL_TEXTURE_2D, enabled);
            gl.glBindTexture(GL.GL_TEXTURE_2D, textureObject);
            gl.glActiveTexture(oldActiveTexture);
        }

        @Override
        public void reset(GL gl) {
            int oldActiveTexture = getInt(gl, GL.GL_ACTIVE_TEXTURE);
            gl.glActiveTexture(GL.GL_TEXTURE0 + index);
            gl.glDisable(GL.GL_TEXTURE_2D);
            gl.glBindTexture(GL.GL_TEXTURE_2D, GL.GL_NONE);
            gl.glActiveTexture(oldActiveTexture);
        }
    }

    public static final State TEXTURE_0 = new TextureState(0);

    public static final State TEXTURE_1 = new TextureState(1);

    public static final State TEXTURE_2 = new TextureState(2);

    public static final State TEXTURE_3 = new TextureState(3);

    public static final State VIEWPORT = new State() {

        private final int[] viewport = new int[4];

        @Override
        public void save(GL gl) {
            gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
        }

        @Override
        public void restore(GL gl) {
            gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        }
    };

    public static final State BUFFERS = new State() {

        private int fbo;

        private int vbo;

        private int ibo;

        private int vao;

        @Override
        public void save(GL gl) {
            fbo = getInt(gl, GL.GL_FRAMEBUFFER_BINDING);
            vbo = getInt(gl, GL.GL_ARRAY_BUFFER_BINDING);
            ibo = getInt(gl, GL.GL_ELEMENT_ARRAY_BUFFER_BINDING);
            vao = getInt(gl, GL2.GL_VERTEX_ARRAY_BINDING);
        }

        @Override
        public void restore(GL gl) {
            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
            gl.getGL2().glBindVertexArray(vao);
        }
    };

    public static final Deque<State> savedStates = new LinkedList<>();

    public static void saveAllStates() {
        saveStates(
                VIEWPORT,
                TRANSFORM,
                PROGRAM,
                CLEAR_COLOR,
                DEPTH_TEST,
                CULLING,
                BLENDING,
                TEXTURE_0,
                TEXTURE_1,
                TEXTURE_2,
                TEXTURE_3,
                BUFFERS);
    }

    public static void resetStates(State... states) {
        checkIsInQuatumRendererThread();
        GL gl = getGL();
        for (State state : states) {
            state.reset(gl);
        }
    }

    public static void saveStates(State... states) {
        checkIsInQuatumRendererThread();
        GL gl = getGL();
        for (State state : states) {
            state.save(gl);
            savedStates.push(state);
        }
    }

    public static void saveState() {
        checkIsInQuatumRendererThread();
        GL gl = getGL();
        if (!savedStates.isEmpty()) {
            State state = savedStates.pop();
            state.restore(gl);
        } else {
            throw new IllegalStateException();
        }
    }

    public static void restoreAllStates() {
        while (!savedStates.isEmpty()) {
            saveState();
        }
    }

    private static GLContext glContext;

    public static GL getGL() {
        checkIsInQuatumRendererThread();
        if (glContext == null) {
            glContext = GLDrawableFactory.getDesktopFactory().createExternalGLContext();
            glContext.makeCurrent();
        }
        return glContext.getGL();
    }

    public static void checkIsInQuatumRendererThread() {
        if (!isInQuatumRendererThread()) {
            throw new IllegalStateException();
        }
    }

    public static boolean isInQuatumRendererThread() {
        return Thread.currentThread().getName().contains("Quantum");
    }

    public static int getInt(GL gl, int parameter) {
        return create(values -> gl.glGetIntegerv(parameter, values));
    }

    public static boolean getBool(GL gl, int parameter) {
        return getInt(gl, parameter) == 1;
    }

    public static void setEnabled(GL gl, int parameter, boolean enabled) {
        if (enabled) {
            gl.glEnable(parameter);
        } else {
            gl.glDisable(parameter);
        }
    }

    public static void using(int target, Consumer<IntBuffer> action) {
        action.accept(IntBuffer.wrap(new int[]{target}));
    }

    public static int create(Consumer<IntBuffer> action) {
        IntBuffer buffer = IntBuffer.allocate(1);
        action.accept(buffer);
        return buffer.get();
    }

    public static void checkNoError(GL gl) {
        int error = gl.glGetError();
        if (error != GL.GL_NO_ERROR) {
            throw new AssertionError("OpenGL error: " + error);
        }
    }

    private JOGL() {
    }
}
