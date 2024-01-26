package org.lwjgl3.display;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.system.MemoryUtil.*;

public class Display {

    private static final DisplayMode initial_mode;
    private static DisplayMode current_mode;

    private static String title = "Minecraft 1.8.9";
    private static final long window;

    private static boolean created;
    private static boolean resized;
    private static boolean focused;
    private static boolean vsync;
    private static GLFWImage.Buffer cached_icons;
    private static ByteBuffer[] cached_icon_data;


    static{
        glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err));

        if ( !glfwInit() )
            throw new RuntimeException("Failed to initialize GLFW");

        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        int bpp = vidMode.redBits() + vidMode.greenBits() + vidMode.blueBits();
        current_mode = initial_mode = new DisplayMode(vidMode.width(), vidMode.height(), bpp, vidMode.refreshRate());

        //INITIAL SETUP
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        //glfwWindowHint(GLFW_DOUBLEBUFFER, GLFW_FALSE);
        glfwWindowHint(GLFW_DECORATED, GLFW_TRUE);


        window = glfwCreateWindow(current_mode.getWidth(), current_mode.getHeight(), title, NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        glfwMakeContextCurrent(window);

        GL.createCapabilities();
    }

    public static void setDisplayMode(DisplayMode displayMode) {
        current_mode = displayMode;
        glfwSetWindowSize(window, displayMode.getWidth(), displayMode.getHeight());
        glfwSetWindowPos(
                window,
                (initial_mode.getWidth() - displayMode.getWidth()) / 2,
                (initial_mode.getHeight() - displayMode.getHeight()) / 2
        );
    }

    public static DisplayMode getDisplayMode() {
        return current_mode;
    }

    public static void setFullscreen(boolean fullscreen) {
        if(fullscreen) glfwSetWindowSize(window, initial_mode.getWidth(), initial_mode.getHeight());
        else glfwSetWindowSize(window, current_mode.getWidth(), current_mode.getHeight());
        glfwWindowHint(GLFW_DECORATED, fullscreen ? GLFW_FALSE : GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, fullscreen ? GLFW_FALSE : GLFW_TRUE);
    }

    public static void setResizable(boolean resizable){
        glfwWindowHint(GLFW_RESIZABLE, resizable ? 1 : 0);
    }

    public static void setTitle(String titleIn) {
        title = titleIn;
        glfwSetWindowTitle(window, titleIn);
    }

    public static void setIcon(ByteBuffer[] icons) {
        if (cached_icons != null) {
            cached_icons.free();
            for (ByteBuffer buffer : cached_icon_data) {
                MemoryUtil.memFree(buffer);
            }
        }
        cached_icons = GLFWImage.calloc(icons.length);
        cached_icon_data = new ByteBuffer[icons.length];
        for (int i = 0; i < icons.length; ++i) {
            ByteBuffer iconBuffer = icons[i];
            GLFWImage icon = (GLFWImage)((Object)cached_icons.get(i));
            Display.cached_icon_data[i] = MemoryUtil.memAlloc(iconBuffer.limit());
            byte[] tmp = new byte[iconBuffer.limit()];
            iconBuffer.get(tmp);
            iconBuffer.position(0);
            cached_icon_data[i].put(tmp);
            cached_icon_data[i].position(0);
            int size = (int)Math.sqrt(cached_icon_data[i].limit() >> 2);
            icon.set(size, size, cached_icon_data[i]);
        }
        GLFW.glfwSetWindowIcon(window, cached_icons);
    }

    public static void setVSyncEnabled(boolean vsyncIn) {
        if(glfwGetCurrentContext() != 0){
            glfwSwapInterval(vsyncIn ? GLFW_TRUE : GLFW_FALSE);
        }
        vsync = vsyncIn;
    }
    public static void destroy(){
        glfwDestroyWindow(window);
        created = false;
    }
    public static DisplayMode[] getAvailableDisplayModes(){
            GLFWVidMode.Buffer m = glfwGetVideoModes(glfwGetPrimaryMonitor());
            ArrayList<DisplayMode> modes = new ArrayList<>();
            while(m.hasRemaining()){
                GLFWVidMode vidMode =  m.get();
                int bpp = vidMode.redBits() + vidMode.greenBits() + vidMode.blueBits();
                modes.add(new DisplayMode(vidMode.width(), vidMode.height(), bpp, vidMode.refreshRate()));
            }
            return modes.toArray(new DisplayMode[modes.size()]);
    }
    public static DisplayMode getDesktopDisplayMode(){
            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            int bpp = vidMode.redBits() + vidMode.greenBits() + vidMode.blueBits();
            return new DisplayMode(vidMode.width(), vidMode.height(), bpp, vidMode.refreshRate());
    }
    public static boolean isCreated(){
        return created;
    }
    public static boolean isCloseRequested(){
        return glfwWindowShouldClose(window);
    }
    public static void sync(int fps){
        Sync.sync(fps);
    }
    public static void update(){
            if(glfwGetWindowAttrib(window, GLFW_VISIBLE) == 1) {
                resized = false;
                glfwSwapBuffers(window);
                Mouse.poll_scrollY = 0;
                glfwPollEvents();
                Mouse.createEvent();
            }
    }
    public static boolean wasResized(){
        return resized;
    }
    public static int getWidth(){
        return current_mode.getWidth();
    }
    public static int getHeight(){
        return current_mode.getHeight();
    }
    public static boolean isActive(){
        return focused;
    }

    public static long getWindow() {
        return window;
    }

    public static void releaseContext() {
        glfwSetWindowShouldClose(window, true);
    }

    public static void create() throws LWJGLException {
        create(new PixelFormat(), new ContextAttribs(3, 3).withProfileCore(true));
    }

    public static void create(PixelFormat pf) throws LWJGLException {
        create(pf,  new ContextAttribs(3, 3).withProfileCore(true));
    }

    public static void create(PixelFormat pf, ContextAttribs attributes) throws LWJGLException {
        created = true;
        try ( MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Center the window
            glfwSetWindowPos(
                    window,
                    (initial_mode.getWidth() - pWidth.get(0)) / 2,
                    (initial_mode.getHeight() - pHeight.get(0)) / 2
            );
        }
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, attributes.getVersion_major());
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, attributes.getVersion_minor());
        if(attributes.isProfileCore()) glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        glfwSetWindowSizeCallback(window, new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                current_mode = new DisplayMode(width, height);
                resized = true;
            }
        });
        glfwSetWindowFocusCallback(window, new GLFWWindowFocusCallback() {
            @Override
            public void invoke(long l, boolean focused) {
                Display.focused = focused;
                Mouse.poll_xPos = Mouse.last_x;
                Mouse.poll_yPos = Mouse.last_y;
            }
        });

        Keyboard.create();
        Keyboard.pollGLFW();
        Mouse.create();
        Mouse.pollGLFW();

        //Use raw input, better for 3D camera
        if (glfwRawMouseMotionSupported())
            glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);

        glfwSwapInterval(vsync ? GLFW_TRUE : GLFW_FALSE);
        glfwShowWindow(window);
    }
}
