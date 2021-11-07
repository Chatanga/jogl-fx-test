package org.hihan.joglfx;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import java.io.File;
import java.io.IOException;
import static org.hihan.joglfx.JOGL.getInt;

public class Framebuffer {

    private final int width;

    private final int height;

    private Texture colorTexture;

    private final int fboTarget;

    private int fbo;

    private int rbo;

    public Framebuffer(GL gl, int width, int height, boolean alphaChannel, boolean depthBuffer) {
        fboTarget = GL.GL_FRAMEBUFFER; // GL.GL_DRAW_FRAMEBUFFER

        this.width = width;
        this.height = height;

        int oldFbo = getInt(gl, GL.GL_FRAMEBUFFER_BINDING);
        fbo = JOGL.create(fbos -> gl.glGenFramebuffers(1, fbos));
        gl.glBindFramebuffer(fboTarget, fbo);

        colorTexture = TextureIO.newTexture(GL.GL_TEXTURE_2D);
        colorTexture.bind(gl);
        colorTexture.enable(gl);

        gl.glTexImage2D(
                GL.GL_TEXTURE_2D,
                0, // mipmap level
                alphaChannel ? GL.GL_RGBA8 : GL.GL_RGB8, // internal format
                width, height,
                0, // border
                alphaChannel ? GL.GL_RGBA : GL.GL_RGB, // format
                GL.GL_UNSIGNED_BYTE, // type
                null);

        colorTexture.setTexParameterf(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        colorTexture.setTexParameterf(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        colorTexture.setTexParameterf(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        colorTexture.setTexParameterf(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

        gl.glBindTexture(GL.GL_TEXTURE_2D, GL.GL_NONE);

        gl.glFramebufferTexture2D(
                fboTarget,
                GL.GL_COLOR_ATTACHMENT0,
                GL.GL_TEXTURE_2D,
                colorTexture.getTextureObject(),
                0);

        if (depthBuffer) {
            rbo = JOGL.create(rbos -> gl.glGenRenderbuffers(1, rbos));
            gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, rbo);
            gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT, width, height);
            gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, GL.GL_NONE);

            gl.glFramebufferRenderbuffer(
                    fboTarget,
                    GL2.GL_DEPTH_ATTACHMENT,
                    GL.GL_RENDERBUFFER,
                    rbo);
        }

        int status = gl.glCheckFramebufferStatus(fboTarget);
        if (status != GL.GL_FRAMEBUFFER_COMPLETE) {
            throw new AssertionError(status);
        }

        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, oldFbo);
        System.out.format("Created framebuffer: %d (%d x %d)\n", fbo, width, height);
    }

    public void dispose(GL gl) {
        if (fbo != GL.GL_NONE) {
            JOGL.using(fbo, fbos -> gl.glDeleteFramebuffers(1, fbos));
            fbo = GL.GL_NONE;
            colorTexture.destroy(gl);
            colorTexture = null;
        } else {
            throw new IllegalStateException();
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Runnable bind(GL gl) {
        if (fbo != GL.GL_NONE) {
            int oldFbo = getInt(gl, GL.GL_FRAMEBUFFER_BINDING);

            gl.glBindFramebuffer(fboTarget, fbo);
            gl.glViewport(0, 0, width, height);

            return () -> {
                System.out.format("Restoring framebuffer: %d -> %d\n", fbo, oldFbo);
                gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, oldFbo);
            };
        } else {
            throw new IllegalStateException();
        }
    }

    public void display(GL glAll) {
        if (fbo != GL.GL_NONE) {
            GL2 gl = glAll.getGL2();
            gl.glBindFramebuffer(GL.GL_READ_FRAMEBUFFER, fbo);
            gl.glBlitFramebuffer(
                    0, 0, width, height,
                    0, 0, width, height,
                    GL.GL_COLOR_BUFFER_BIT, GL.GL_NEAREST);
        } else {
            throw new IllegalStateException();
        }
    }

    public int getColorTextureObject() {
        return colorTexture.getTextureObject();
    }

    public void dumpToFile(File file) throws IOException {
        TextureIO.write(colorTexture, file);
    }
}
