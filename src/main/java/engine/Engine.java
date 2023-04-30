package engine;

import graphics.Shader;
import input.Input;
import level.Level;
import math.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Engine implements Runnable {

    public long window;

    private Level level;

    private double fps = 60.0;
    private double frame_cap = 1.0 / fps;
    private long lastFpsTime = 0;
    public DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
    public DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);

    public Engine(String name, int width, int height, int vSync) {
        init(name, width, height, vSync);
    }

    public void run() {
        System.out.println("Alive");

        loop();

        //window callbacks and destroy
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        //terminate GLFW
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public void init(String name, int width, int height, int vSync) {
        //set up error callback, basic implementation, will print the error message in System.err
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to init GLFW");
        }

        //configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        //create window
        window = glfwCreateWindow(width, height, name, NULL, NULL);

        if (window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        //setup key callback, this will be called every time a key is pressed, repeated or released
        glfwSetKeyCallback(window, new Input());

        //get thread stack and push a new frame

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); //int*
            IntBuffer pHeight = stack.mallocInt(1); //int*

            //get window size passed to glfwCreatWidnow
            glfwGetWindowSize(window, pWidth, pHeight);

            //get the resolution of the primary monitor
            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            //center window
            glfwSetWindowPos(window, (vidMode.width() - pWidth.get(0)) / 2,
                    (vidMode.height() - pHeight.get(0)) / 2);
        }//the stack frame is popped automatically

        //make opengl context current
        glfwMakeContextCurrent(window);

        //Critical for LWJGL interoperation with GLFWs OpenGL context
        GL.createCapabilities();

        //set clear color
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        //enable v sync
        glfwSwapInterval(vSync);

        glfwShowWindow(window);
        Shader.loadAll();
        Shader.BG.enable();
        Matrix4f pr_matrix = Matrix4f.orthographic(-10.f, 10.0f, -10.0f * 9.0f / 16.0f, 10.0f * 9.0f / 16.0f, -1.0f, 1.0f);
        Shader.BG.setUniformMat4f("pr_matrix", pr_matrix);
        Shader.BG.disable();

        level = new Level();
    }

    public void loop() {

        double time = Timer.getTime();
        double unprocessed = 0;
        while (!glfwWindowShouldClose(window)) {
            double start = Timer.getTime();
            double passed = start - time;
            unprocessed += passed;

            time = start;

            while (unprocessed >= frame_cap) {
                unprocessed -= frame_cap;

                if (Input.keys[GLFW_KEY_ESCAPE]) {
                    glfwSetWindowShouldClose(window, true);
                }

                //Poll for windows events
                glfwPollEvents();
                glfwGetCursorPos(window, xBuffer, yBuffer);

                System.out.printf("x: %f, y: %f \n", xBuffer.get(0), yBuffer.get(0));
                System.out.printf("unprocessed: %f, frame_cap: %f \n", unprocessed, frame_cap);
            }
            render();
        }
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); //clear the frame buffer
        level.render();
        int i = glGetError();
        if (i != GL_NO_ERROR) {
            System.out.println(i);
        }
        glfwSwapBuffers(window);
    }
}
