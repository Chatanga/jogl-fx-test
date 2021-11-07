package org.hihan.joglfx;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FALSE;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_NONE;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL2ES2.GL_COMPILE_STATUS;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_INFO_LOG_LENGTH;
import static com.jogamp.opengl.GL2ES2.GL_LINK_STATUS;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES2.*;
import com.jogamp.opengl.GL3;
import static com.jogamp.opengl.GL3ES3.GL_GEOMETRY_SHADER;
import com.jogamp.opengl.util.GLBuffers;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class ColoredSquare {

    public static final int POSITION = 0;

    public static final int COLOR = 1;

    private final String vertexShader = String.join("\n",
            "#version 330",
            "layout(location = POSITION) in vec2 position;",
            "layout(location = COLOR) in vec3 color;",
            "out vec3 outColor;",
            "void main()",
            "{",
            "   gl_Position = vec4(position, 0, 1);",
            "   outColor = color;",
            "}")
            .replaceAll("POSITION", String.valueOf(POSITION))
            .replaceAll("COLOR", String.valueOf(COLOR));

    private final String fragmentShader = String.join("\n",
            "#version 330",
            "in vec3 outColor;",
            "out vec4 outputColor;",
            "void main()",
            "{",
            "   outputColor = vec4(outColor, 1);",
            "}");

    private static final float R = 0.75f;

    private static final float[] VERTEX_DATA = {
        +R, +R, 1, 0, 0,
        +R, -R, 0, 1, 0,
        -R, -R, 0, 0, 1,
        +R, +R, 1, 0, 0,
        -R, -R, 0, 0, 1,
        -R, +R, 1, 0, 1
    };

    private int program;

    private int vbo;

    private int vao;

    public ColoredSquare(GL3 gl) {
        initializeProgram(gl);
        initializeVertexBuffer(gl);
        initializeVertexArrayObject(gl);
    }

    private void initializeProgram(GL3 gl) {
        ArrayList<Integer> shaderList = new ArrayList<>();

        shaderList.add(createShader(gl, GL_VERTEX_SHADER, vertexShader));
        shaderList.add(createShader(gl, GL_FRAGMENT_SHADER, fragmentShader));

        program = createProgram(gl, shaderList);

        shaderList.forEach(gl::glDeleteShader);
    }

    private int createShader(GL3 gl, int shaderType, String shaderFile) {
        int newShader = gl.glCreateShader(shaderType);
        String[] lines = {shaderFile};
        IntBuffer length = newDirectIntBuffer(new int[]{lines[0].length()});
        gl.glShaderSource(newShader, 1, lines, length);

        gl.glCompileShader(newShader);

        IntBuffer status = newDirectIntBuffer(1);
        gl.glGetShaderiv(newShader, GL_COMPILE_STATUS, status);
        if (status.get(0) == GL_FALSE) {

            IntBuffer infoLogLength = newDirectIntBuffer(1);
            gl.glGetShaderiv(newShader, GL_INFO_LOG_LENGTH, infoLogLength);

            ByteBuffer bufferInfoLog = newDirectByteBuffer(infoLogLength.get(0));
            gl.glGetShaderInfoLog(newShader, infoLogLength.get(0), null, bufferInfoLog);
            byte[] bytes = new byte[infoLogLength.get(0)];
            bufferInfoLog.get(bytes);
            String infoLog = new String(bytes);

            String shaderTypeName;
            switch (shaderType) {
                case GL_VERTEX_SHADER:
                    shaderTypeName = "vertex";
                    break;
                case GL_GEOMETRY_SHADER:
                    shaderTypeName = "geometry";
                    break;
                case GL_FRAGMENT_SHADER:
                    shaderTypeName = "fragment";
                    break;
                default:
                    shaderTypeName = "?";
            }
            throw new AssertionError("Compiler failure in " + shaderTypeName + " shader: " + infoLog);
        }

        return newShader;
    }

    private int createProgram(GL3 gl, ArrayList<Integer> shaders) {
        int newProgram = gl.glCreateProgram();

        shaders.forEach(shader -> gl.glAttachShader(newProgram, shader));

        gl.glLinkProgram(newProgram);

        IntBuffer status = newDirectIntBuffer(1);
        gl.glGetProgramiv(newProgram, GL_LINK_STATUS, status);
        if (status.get(0) == GL_FALSE) {

            IntBuffer infoLogLength = newDirectIntBuffer(1);
            gl.glGetProgramiv(newProgram, GL_INFO_LOG_LENGTH, infoLogLength);

            ByteBuffer bufferInfoLog = newDirectByteBuffer(infoLogLength.get(0));
            gl.glGetProgramInfoLog(newProgram, infoLogLength.get(0), null, bufferInfoLog);
            byte[] bytes = new byte[infoLogLength.get(0)];
            bufferInfoLog.get(bytes);
            String infoLog = new String(bytes);

            throw new AssertionError("Linker failure: " + infoLog);
        }

        shaders.forEach(shader -> gl.glDetachShader(newProgram, shader));

        return newProgram;
    }

    private void initializeVertexBuffer(GL3 gl) {
        FloatBuffer vertexBuffer = newDirectFloatBuffer(VERTEX_DATA);

        vbo = JOGL.create(vbos -> gl.glGenBuffers(1, vbos));

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo);
        {
            gl.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        }
        gl.glBindBuffer(GL_ARRAY_BUFFER, GL_NONE);
    }

    private void initializeVertexArrayObject(GL3 gl) {
        vao = JOGL.create(vaos -> gl.glGenVertexArrays(1, vaos));

        gl.glBindVertexArray(vao);
        {
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo);
            {
                gl.glEnableVertexAttribArray(POSITION);
                gl.glVertexAttribPointer(POSITION, 2, GL_FLOAT, false, 5 * 4, 0);

                gl.glEnableVertexAttribArray(COLOR);
                gl.glVertexAttribPointer(COLOR, 3, GL_FLOAT, false, 5 * 4, 2 * 4);
            }
            gl.glBindBuffer(GL_ARRAY_BUFFER, GL_NONE);
        }
        gl.glBindVertexArray(GL_NONE);
    }

    public void display(GL3 gl) {
        gl.glUseProgram(program);
        {
            gl.glBindVertexArray(vao);
            {
                gl.glDrawArrays(GL_TRIANGLES, 0, 6);
            }
            gl.glBindVertexArray(GL_NONE);
        }
        gl.glUseProgram(GL_NONE);
    }

    public void dispose(GL3 gl) {
        gl.glDeleteProgram(program);
    }

    private static IntBuffer newDirectIntBuffer(int size) {
        return (IntBuffer) GLBuffers.newDirectGLBuffer(GL_INT, size);
    }

    private static IntBuffer newDirectIntBuffer(int[] ints) {
        IntBuffer buffer = (IntBuffer) GLBuffers.newDirectGLBuffer(GL_INT, ints.length);
        buffer.put(ints);
        buffer.rewind();
        return buffer;
    }

    private static FloatBuffer newDirectFloatBuffer(float[] floats) {
        FloatBuffer buffer = (FloatBuffer) GLBuffers.newDirectGLBuffer(GL_FLOAT, floats.length);
        buffer.put(floats);
        buffer.rewind();
        return buffer;
    }

    private static ByteBuffer newDirectByteBuffer(int size) {
        return (ByteBuffer) GLBuffers.newDirectGLBuffer(GL_BYTE, size);
    }
}
