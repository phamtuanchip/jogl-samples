/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_320.texture;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2ES3.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.BufferUtils;
import glm.glm;
import glm.mat._4.Mat4;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import glf.Vertex_v2fv2f;
import glm.vec._2.Vec2;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jgli.Texture2d;

/**
 *
 * @author GBarbieri
 */
public class Gl_320_texture_pixel_store extends Test {

    public static void main(String[] args) {
        Gl_320_texture_pixel_store gl_320_texture_pixel_store = new Gl_320_texture_pixel_store();
    }

    public Gl_320_texture_pixel_store() {
        super("gl-320-texture-pixel-store", Profile.CORE, 3, 2);
    }

    private final String SHADERS_SOURCE = "texture-2d";
    private final String SHADERS_ROOT = "src/data/gl_320/texture";
    private final String TEXTURE_DIFFUSE = "kueken7_rgba8_srgb.dds";

    // With DDS textures, v texture coordinate are reversed, from top to bottom
    private int vertexCount = 6;
    private int vertexSize = vertexCount * Vertex_v2fv2f.SIZE;
    private float[] vertexData = {
        -1.0f, -1.0f,/**/ 0.0f, 1.0f,
        +1.0f, -1.0f,/**/ 1.0f, 1.0f,
        +1.0f, +1.0f,/**/ 1.0f, 0.0f,
        +1.0f, +1.0f,/**/ 1.0f, 0.0f,
        -1.0f, +1.0f,/**/ 0.0f, 0.0f,
        -1.0f, -1.0f,/**/ 0.0f, 1.0f};

    private class Buffer {

        public static final int VERTEX = 0;
        public static final int TRANSFORM = 1;
        public static final int MAX = 2;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            vertexArrayName = GLBuffers.newDirectIntBuffer(1), textureName = GLBuffers.newDirectIntBuffer(1);
    private int programName, uniformTransform, uniformDiffuse;

    @Override
    protected boolean begin(GL gl) {

        GL3 gl3 = (GL3) gl;

        boolean validated = true;

        if (validated) {
            validated = initTexture(gl3);
        }
        if (validated) {
            validated = initProgram(gl3);
        }
        if (validated) {
            validated = initBuffer(gl3);
        }
        if (validated) {
            validated = initVertexArray(gl3);
        }

        return validated && checkError(gl3, "begin");
    }

    private boolean initProgram(GL3 gl3) {

        boolean validated = true;

        if (validated) {

            ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE, "vert", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE, "frag", null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.add(vertShaderCode);
            shaderProgram.add(fragShaderCode);

            shaderProgram.init(gl3);

            programName = shaderProgram.program();

            gl3.glBindAttribLocation(programName, Semantic.Attr.POSITION, "position");
            gl3.glBindAttribLocation(programName, Semantic.Attr.TEXCOORD, "texCoord");
            gl3.glBindFragDataLocation(programName, Semantic.Frag.COLOR, "color");

            shaderProgram.link(gl3, System.out);
        }
        if (validated) {

            uniformTransform = gl3.glGetUniformBlockIndex(programName, "Transform");
            uniformDiffuse = gl3.glGetUniformLocation(programName, "diffuse");
        }

        return validated & checkError(gl3, "initProgram");
    }

    private boolean initBuffer(GL3 gl3) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        IntBuffer uniformBufferOffset = GLBuffers.newDirectIntBuffer(1);

        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl3.glBufferData(GL_ARRAY_BUFFER, vertexSize, vertexBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl3.glGetIntegerv(
                GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT,
                uniformBufferOffset);

        int uniformBlockSize = Math.max(Mat4.SIZE, uniformBufferOffset.get(0));

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
        gl3.glBufferData(GL_UNIFORM_BUFFER, uniformBlockSize, null, GL_DYNAMIC_DRAW);
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(vertexBuffer);
        BufferUtils.destroyDirectBuffer(uniformBufferOffset);

        return checkError(gl3, "initBuffer");
    }

    private boolean initTexture(GL3 gl3) {

        try {
            gl3.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            gl3.glGenTextures(1, textureName);

            gl3.glActiveTexture(GL_TEXTURE0);
            gl3.glBindTexture(GL_TEXTURE_2D, textureName.get(0));
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            jgli.Texture2d texture = new Texture2d(jgli.Load.load(TEXTURE_ROOT + "/" + TEXTURE_DIFFUSE));
            jgli.Gl.Format format = jgli.Gl.translate(texture.format());

            // Setup the pixel storage to load only a rectangle in the middle of the source texture
            gl3.glPixelStorei(GL_UNPACK_ROW_LENGTH, texture.dimensions()[0]);
            gl3.glPixelStorei(GL_UNPACK_SKIP_PIXELS, texture.dimensions()[0] / 4);
            gl3.glPixelStorei(GL_UNPACK_SKIP_ROWS, texture.dimensions()[1] / 4);

            gl3.glTexImage2D(GL_TEXTURE_2D, 0,
                    format.internal.value,
                    texture.dimensions()[0] / 2, texture.dimensions()[1] / 2,
                    0,
                    format.external.value, format.type.value,
                    texture.data());

            gl3.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
            gl3.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            gl3.glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
            gl3.glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);

        } catch (IOException ex) {
            Logger.getLogger(Gl_320_texture_pixel_store.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    private boolean initVertexArray(GL3 gl3) {

        gl3.glGenVertexArrays(1, vertexArrayName);
        gl3.glBindVertexArray(vertexArrayName.get(0));
        {
            gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, Vertex_v2fv2f.SIZE, 0);
            gl3.glVertexAttribPointer(Semantic.Attr.TEXCOORD, 2, GL_FLOAT, false, Vertex_v2fv2f.SIZE, Vec2.SIZE);
            gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl3.glEnableVertexAttribArray(Semantic.Attr.TEXCOORD);
        }
        gl3.glBindVertexArray(0);

        return true;
    }

    @Override
    protected boolean render(GL gl) {

        GL3 gl3 = (GL3) gl;

        {
            gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
            ByteBuffer pointer = gl3.glMapBufferRange(
                    GL_UNIFORM_BUFFER, 0, Mat4.SIZE,
                    GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

            Mat4 projection = glm.perspective_((float) Math.PI * 0.25f, 4.0f / 3.0f, 0.1f, 100.0f);
            Mat4 model = new Mat4(1.0f);
            Mat4 mvp = projection.mul(viewMat4()).mul(model);

            pointer.asFloatBuffer().put(mvp.toFa_());

            // Make sure the uniform buffer is uploaded
            gl3.glUnmapBuffer(GL_UNIFORM_BUFFER);
        }

        gl3.glViewport(0, 0, windowSize.x, windowSize.y);
        clearColor.put(new float[]{1.0f, 0.5f, 0.0f, 1.0f}).rewind();
        gl3.glClearBufferfv(GL_COLOR, 0, clearColor);

        gl3.glUseProgram(programName);
        gl3.glUniform1i(uniformDiffuse, 0);
        gl3.glUniformBlockBinding(programName, uniformTransform, Semantic.Uniform.TRANSFORM0);

        gl3.glActiveTexture(GL_TEXTURE0);
        gl3.glBindTexture(GL_TEXTURE_2D, textureName.get(0));
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM));
        gl3.glBindVertexArray(vertexArrayName.get(0));

        gl3.glDrawArraysInstanced(GL_TRIANGLES, 0, vertexCount, 1);

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL3 gl3 = (GL3) gl;

        gl3.glDeleteBuffers(Buffer.MAX, bufferName);
        gl3.glDeleteProgram(programName);
        gl3.glDeleteTextures(1, textureName);
        gl3.glDeleteVertexArrays(1, vertexArrayName);

        BufferUtils.destroyDirectBuffer(bufferName);
        BufferUtils.destroyDirectBuffer(textureName);
        BufferUtils.destroyDirectBuffer(vertexArrayName);

        return checkError(gl3, "end");
    }
}
