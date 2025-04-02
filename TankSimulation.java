// javac -classpath ".;C:\Program Files\lwjgl-release-3.3.4-custom\*" TankSimulation.java
// java -classpath ".;C:\Program Files\lwjgl-release-3.3.4-custom\*" TankSimulation

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

import java.util.LinkedList;

public class TankSimulation {
    private long window;
    private int width = 800;
    private int height = 600;
    private List<Tank> tanks = new LinkedList<>(); // List to store multiple tanks
    private int currentTankIndex = 0; // Index of the currently controlled tank
    private Terrain terrain;

    public static void main(String[] args) {
        new TankSimulation().run();
    }

    public void run() {
        System.out.println("test");
        init();
        loop();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    private void init() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        window = GLFW.glfwCreateWindow(width, height, "Tank Simulation", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        setPerspectiveProjection(45.0f, (float) 800 / (float) 600, 0.1f, 100.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        initLighting();

        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);

        // Define light properties
        FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4).put(new float[] { 0.0f, 10.0f, 10.0f, 1.0f });
        lightPosition.flip();
        GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_POSITION, lightPosition);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        // Clear the screen and depth buffer
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
        // Initialize multiple tanks
        tanks.add(new Tank(0, 0, 0, 1.0f, 0.0f, 0.0f)); // Tank 1
        tanks.add(new Tank(5, 0, 5, 0.01f, 0.0f, 1.0f)); // Tank 2

        // Initialize the terrain
        terrain = new Terrain("terrain.obj"); // Load the terrain from an OBJ file
    }

    private void loop() {
        while (!GLFW.glfwWindowShouldClose(window)) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glLoadIdentity();

            // Update tank movement based on user input
            updateTankMovement();

            // Update the camera to track the currently controlled tank
            updateCamera(tanks.get(currentTankIndex));

            // Render terrain
            terrain.render();

            // Render and update all tanks
            for (Tank tank : tanks) {
                tank.update();
                tank.render(terrain);
            }

            // Handle tank switching
            handleTankSwitching();

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    public void initLighting() {
        // Enable lighting and the first light
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_LIGHT0);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LEQUAL);

        // Set the light position
        FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4).put(new float[] { 0.0f, 10.0f, 10.0f, 1.0f }); 
        lightPosition.flip();
        GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_POSITION, lightPosition);

        // Set brighter ambient, diffuse, and specular light
        FloatBuffer ambientLight = BufferUtils.createFloatBuffer(4).put(new float[] { 0.4f, 0.4f, 0.4f, 1.0f }); // Increase ambient light
        ambientLight.flip();
        GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_AMBIENT, ambientLight);

        FloatBuffer diffuseLight = BufferUtils.createFloatBuffer(4).put(new float[] { 1.0f, 1.0f, 1.0f, 1.0f }); // Increase diffuse light
        diffuseLight.flip();
        GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, diffuseLight);

        FloatBuffer specularLight = BufferUtils.createFloatBuffer(4).put(new float[] { 1.0f, 1.0f, 1.0f, 1.0f }); // Increase specular light
        specularLight.flip();
        GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_SPECULAR, specularLight);

        // Enable color material to allow vertex colors with lighting
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);

        // Set material properties
        FloatBuffer materialAmbient = BufferUtils.createFloatBuffer(4).put(new float[] { 0.6f, 0.6f, 0.6f, 1.0f }); // Brighter ambient reflection
        materialAmbient.flip();
        GL11.glMaterialfv(GL11.GL_FRONT, GL11.GL_AMBIENT, materialAmbient);

        FloatBuffer materialDiffuse = BufferUtils.createFloatBuffer(4).put(new float[] { 0.8f, 0.8f, 0.8f, 1.0f }); // Brighter diffuse reflection
        materialDiffuse.flip();
        GL11.glMaterialfv(GL11.GL_FRONT, GL11.GL_DIFFUSE, materialDiffuse);

        FloatBuffer materialSpecular = BufferUtils.createFloatBuffer(4).put(new float[] { 1.0f, 1.0f, 1.0f, 1.0f }); // Specular highlight
        materialSpecular.flip();
        GL11.glMaterialfv(GL11.GL_FRONT, GL11.GL_SPECULAR, materialSpecular);

        GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, 50.0f); // Set shininess (higher = more specular reflection)

        // Set global ambient light
        FloatBuffer globalAmbient = BufferUtils.createFloatBuffer(4).put(new float[] { 0.5f, 0.5f, 0.5f, 1.0f });
        globalAmbient.flip();
        GL11.glLightModelfv(GL11.GL_LIGHT_MODEL_AMBIENT, globalAmbient);
    }

    private void setPerspectiveProjection(float fov, float aspect, float zNear, float zFar) {
        float ymax = (float) (zNear * Math.tan(Math.toRadians (fov / 2.0)));
        float xmax = ymax * aspect;

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glFrustum(-xmax, xmax, -ymax, ymax, zNear, zFar);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private void setupCamera() {
        // Position the camera behind the tank, following it
        GL11.glTranslatef(0, -5, -20); // Adjust this for better view
        GL11.glRotatef(20, 1, 0, 0); // Slight downward angle
    }

    private float lerp(float start, float end, float alpha) {
        return start + alpha * (end - start);
    }

    private float cameraX = 0;
    private float cameraY = 5;
    private float cameraZ = 10;

    private void updateCamera(Tank tank) {
        float cameraDistance = 10.0f; // Distance behinid the tank
        float cameraHeight = 5.0f; // Height above the tank

        // Calculate the desired camera position behind and above the tank
        float targetCameraX = tank.getX() - (float)(Math.sin(Math.toRadians(tank.getAngle())) * cameraDistance);
        float targetCameraZ = tank.getZ() - (float)(Math.cos(Math.toRadians(tank.getAngle())) * cameraDistance);
        float targetCameraY = tank.getY() + cameraHeight;

        // Smoothly interpolate between the current camera position and the target position
        float alpha = 0.1f; // // Smoothing factor (0 = no movement, 1 = instant movement)
        cameraX = lerp(cameraX, targetCameraX, alpha);
        cameraY = lerp(cameraY, targetCameraY, alpha);
        cameraZ = lerp(cameraZ, targetCameraZ, alpha);

        //  Reset the modelview matrix
        GL11.glLoadIdentity();

        // Set the camera to look at the tank
        gluLookAt(cameraX, cameraY, cameraZ, tank.getX(), tank.getY(), tank.getZ(), 0.0f, 1.0f, 0.0f);
    }

    private void gluLookAt(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        // Step 1: Calculate the forward vector (the direction the camera is looking)
        float[] forward = { centerX - eyeX, centerY - eyeY, centerZ - eyeZ };
        normalize(forward); // Normalize the forward vector

        // Step 2: Define the up vector (Y-axis typically)
        float[] up = { upX, upY, upZ };

        // Step 3: Calculate the side (right) vector using cross product of forward and up
        float[] side = crossProduct(forward, up);
        normalize(side); // Normalize the side vector

        // Step 4: Recompute the true up vector (should be perpendicular to both side and forward)
        up = crossProduct(side, forward);

        // Step 5: Create the lookAt matrix (view matrix)
        FloatBuffer viewMatrix = BufferUtils.createFloatBuffer(16);
        viewMatrix.put(new float[] {
            side[0], up[0], -forward[0], 0,
            side[1], up[1], -forward[1], 0,
            side[2], up[2], -forward[2], 0,
            -dotProduct(side, new float[] { eyeX, eyeY, eyeZ }),
            -dotProduct(up, new float[] { eyeX, eyeY, eyeZ }),
            dotProduct(forward, new float[] { eyeX, eyeY, eyeZ }), 1
        });
        viewMatrix.flip(); // Flip the buffer for use by OpenGL

        // Step 6: Apply the view matrix
        GL11.glMultMatrixf(viewMatrix);
    }

    // Utility functions for vector math
    private void normalize(float[] vector) {
        float length = (float) Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
        if(length != 0) {
            vector[0] /= length;
            vector[1] /= length;
            vector[2] /= length;
        }
    }

    private float[] crossProduct(float[] a, float[] b) {
        return new float[] {
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]
        };
    }

    private float dotProduct(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private void handleTankSwitching() {
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_1) == GLFW.GLFW_PRESS) {
            currentTankIndex = 0; // Switch to Tank 1
        } else if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_2) == GLFW.GLFW_PRESS) {
            currentTankIndex = 1; // Switch to Tank 2
        }
    }

    private void updateTankMovement() {
        // Get the currently controlled tank
        Tank tank = tanks.get(currentTankIndex);
        
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS) {
            tank.accelerate();
        } if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS) {
            tank.decelerate();
        } if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) {
            tank.turnLeft();
        } if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) {
            tank.turnRight();
        }
    }
}

class Tank {
    private float x, y, z; // Tank's position
    private float r, g, b; // Color of the tank
    private float speed = 0; // Current speed
    private float angle = 0; // Direction the tank is facing
    private float maxSpeed = 0.1f;
    private float acceleration = 0.01f;
    private float friction = 0.98f;
    private float turnSpeed = 2.0f; // Speed of turning

    
    public Tank(float x, float y, float z, float r, float g, float b) { // Add turrent movement
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getAngle() {
        return angle;
    }

    public void accelerate() {
        if (speed < maxSpeed) {
            speed += acceleration;
        }
    }

    public void decelerate() {
        if (speed > -maxSpeed) {
            speed -= acceleration;
        }
    }

    public void turnLeft() {
        angle += turnSpeed;
    }

    public void turnRight() {
        angle -= turnSpeed;
    }

    public void update() {
        // Update position based on speed and angle
        x += speed * Math.sin(Math.toRadians(angle));
        z += speed * Math.cos(Math.toRadians(angle));

        // Apply friction to slow down the tank naturally
        speed *= friction;
    }

    public void render(Terrain terrain) {
        // Get the heights of each wheel
        float frontLeftWheelY = terrain.getTerrianHeightAt(x - 0.9f, z + 1.5f);
        float frontRightWheelY = terrain.getTerrianHeightAt(x + 0.9f, z + 1.5f);
        float midFrontLeftWheelY = terrain.getTerrianHeightAt(x - 0.45f, z + 0.75f);
        float midFrontRightWheelY = terrain.getTerrianHeightAt(x + 0.45f, z + 0.75f);
        float midRearLeftWheelY = terrain.getTerrianHeightAt(x - 0.45f, z - 0.75f);
        float midRearRightWheelY = terrain.getTerrianHeightAt(x + 0.45f, z - 0.75f);
        float rearLeftWheelY = terrain.getTerrianHeightAt(x - 0.9f, z - 1.5f);
        float rearRightWheelY = terrain.getTerrianHeightAt(x + 0.9f, z - 1.5f);

        // Calculate the average height of the tank body (based on wheel heights)
        float averageHeight = (frontLeftWheelY + frontRightWheelY + midFrontLeftWheelY + midFrontRightWheelY + midRearLeftWheelY + midRearRightWheelY + rearLeftWheelY + rearRightWheelY) / 8.0f;

        // Tank body dimensions
        float tankBodyHeight = 0.5f; // The height of the tank body

        // Adjust the height of the tank body to be above the wheels
        // The tank body is raised by half of its height so the bottom aligns with the wheels
        float tankBodyYOffset = 4.0f * tankBodyHeight + tankBodyHeight / 2.0f;

        // Calculate pitch (foward/backword tilt) and roll (side tilt)
        float pitch = (frontLeftWheelY + frontRightWheelY + midFrontLeftWheelY + midFrontRightWheelY) / 4.0f - (rearLeftWheelY + rearRightWheelY + midRearLeftWheelY + midRearRightWheelY) / 4.0f;
        float roll = (frontLeftWheelY + frontRightWheelY + midFrontLeftWheelY + midFrontRightWheelY) / 4.0f - (rearLeftWheelY + rearRightWheelY + midRearLeftWheelY + midRearRightWheelY) / 4.0f;

        // Apply the calculated pitch, roll, and average height to the tank body
        GL11.glPushMatrix();

        // Translate the tank body to the average height plus the offset to position it above the wheels
        GL11.glTranslatef(x, averageHeight + tankBodyYOffset, z);

        // Rotate the tank body for pitch (tilt foward/backword) and roll (tilt left/right)
        GL11.glRotatef(roll * 10.0f, 0, 0, 1); // Roll around the Z-axis
        GL11.glRotatef(pitch * 10.0f, 1, 0, 0); // Pitch around the X-axis

        // Rotate the tank in the direction it's facing
        GL11.glRotatef(angle, 0, 1, 0);

        // Render the tank body
        renderTankBody(); // Call the updated renderTankBody method

        // Render the wheels
        renderWheels(terrain); // Render the wheels based on terrain

        GL11.glPopMatrix(); // Restore the transformation state
    }

    private void renderTankBody() {
        GL11.glColor3f(r, g, b); // Set the color of the tank body
        GL11.glShadeModel(GL11.GL_SMOOTH); // Smooth shading for Phong


        FloatBuffer tankBodySpecular = BufferUtils.createFloatBuffer(4).put(new float[] {0.9f, 0.9f, 0.9f, 1.0f});
        tankBodySpecular.flip();
        GL11.glMaterialfv(GL11.GL_FRONT, GL11.GL_SPECULAR, tankBodySpecular);
        GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, 64.0f); // High shininess for tank body

        float length = 4.0f;
        float width = 2.0f;
        float height = 0.5f;

        GL11.glBegin(GL11.GL_QUADS);

        // Front face
        GL11.glNormal3f(0, 0, 1);
        GL11.glVertex3f(-width / 2, -height / 2, length / 2);
        GL11.glVertex3f(width / 2, -height / 2, length / 2);
        GL11.glVertex3f(width / 2, height / 2, length / 2);
        GL11.glVertex3f(-width / 2, height / 2, length / 2);

        // Back face (z = -length/2)
        GL11.glVertex3f(-width / 2, -height / 2, -length / 2);
        GL11.glVertex3f(width / 2, -height / 2, -length / 2);
        GL11.glVertex3f(width / 2, height / 2, -length / 2);
        GL11.glVertex3f(-width / 2, height / 2, -length / 2);

        // Left face (x = -width/2)
        GL11.glVertex3f(-width / 2, -height / 2, -length / 2);
        GL11.glVertex3f(-width / 2, -height / 2, length / 2);
        GL11.glVertex3f(-width / 2, height / 2, length / 2);
        GL11.glVertex3f(-width / 2, height / 2, -length / 2);

        // Right face (x = +width/2)
        GL11.glVertex3f(width / 2, -height / 2, -length / 2);
        GL11.glVertex3f(width / 2, -height / 2, length / 2);
        GL11.glVertex3f(width / 2, height / 2, length / 2);
        GL11.glVertex3f(width / 2, height / 2, -length / 2);

        // Top face (y = +height/2)
        GL11.glVertex3f(-width / 2, height / 2, -length / 2);
        GL11.glVertex3f(width / 2, height / 2, -length / 2);
        GL11.glVertex3f(width / 2, height / 2, length / 2);
        GL11.glVertex3f(-width / 2, height / 2, length / 2);

        // Bottom face (y = -height/2)
        GL11.glVertex3f(-width / 2, -height / 2, -length / 2);
        GL11.glVertex3f(width / 2, -height / 2, -length / 2);
        GL11.glVertex3f(width / 2, -height / 2, length / 2);
        GL11.glVertex3f(-width / 2, -height / 2, length / 2);

        GL11.glEnd();
    }

    private void renderWheel() {
        float radius = 0.4f;
        float width = 0.2f;
        int numSegments = 36;

        GL11.glColor3f(0.2f, 0.2f, 0.2f); // Dark gray for wheels
        GL11.glShadeModel(GL11.GL_SMOOTH);

        FloatBuffer wheelSpecular = BufferUtils.createFloatBuffer(4).put(new float[] {0.5f, 0.5f, 0.5f, 1.0f});
        wheelSpecular.flip();
        GL11.glMaterialfv(GL11.GL_FRONT, GL11.GL_SPECULAR, wheelSpecular);
        GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, 16.0f); // Low shininess for wheels

        GL11.glPushMatrix();
        GL11.glRotatef(90, 0, 1, 0);

        // Front face (at z = -width/2)
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3f(0.0f, 0.0f, -width / 2); // Center of the circle
        for (int i = 0; i <= numSegments; i++) {
            double angle =  2 * Math.PI * i / numSegments;
            GL11.glVertex3f((float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, -width / 2);
        }
        GL11.glEnd();

        // Rear face (at z = +width/2)
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3f(0.0f, 0.0f, width / 2); // Center of the circle
        for (int i = 0; i <= numSegments; i++) {
            double angle =  2 * Math.PI * i / numSegments;
            GL11.glVertex3f((float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, width / 2);
        }
        GL11.glEnd();

        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= numSegments; i++) {
            double angle =  2 * Math.PI * i / numSegments;
            float x = (float) Math.cos(angle) * radius;
            float y = (float) Math.sin(angle) * radius;

            // Set normals to make wheel sides visible
            GL11.glNormal3f(x, y, 0);
            GL11.glVertex3f(x, y, -width / 2);
            GL11.glVertex3f(x, y, width / 2);
        }
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    private void renderWheels(Terrain terrain) {
        GL11.glColor3f(0.0f, 0.0f, 0.0f); // Black color for wheels
    
        // Define the wheel height offset
        float wheelHeightOffset = 0.8f; // Lower the wheels by this amount relative to the tank body
    
        // Front-left wheel
        GL11.glPushMatrix();
        float frontLeftWheelY = terrain.getTerrianHeightAt(this.getX() - 0.9f, this.getZ() + 1.5f);
        GL11.glTranslatef(-0.9f, frontLeftWheelY + 0.5f - wheelHeightOffset, 1.5f);
        renderWheel();
        GL11.glPopMatrix();
    
        // Front-right wheel
        GL11.glPushMatrix();
        float frontRightWheelY = terrain.getTerrianHeightAt(this.getX() + 0.9f, this.getZ() + 1.5f);
        GL11.glTranslatef(0.9f, frontRightWheelY + 0.5f - wheelHeightOffset, 1.5f);
        renderWheel();
        GL11.glPopMatrix();
    
        // Mid-front-left wheel
        GL11.glPushMatrix();
        float midFrontLeftWheelY = terrain.getTerrianHeightAt(this.getX() - 0.9f, this.getZ() + 0.9f);
        GL11.glTranslatef(-0.9f, midFrontLeftWheelY + 0.5f - wheelHeightOffset, 0.9f);
        renderWheel();
        GL11.glPopMatrix();
    
        // Mid-front-right wheel
        GL11.glPushMatrix();
        float midFrontRightWheelY = terrain.getTerrianHeightAt(this.getX() + 0.9f, this.getZ() + 0.9f);
        GL11.glTranslatef(0.9f, midFrontRightWheelY + 0.5f - wheelHeightOffset, 0.9f);
        renderWheel();
        GL11.glPopMatrix();
    
        // Extra wheel 1 (left side)
        GL11.glPushMatrix();
        float extraLeftWheel1Y = terrain.getTerrianHeightAt(this.getX() - 0.9f, this.getZ() + 0.3f);
        GL11.glTranslatef(-0.9f, extraLeftWheel1Y + 0.5f - wheelHeightOffset, 0.3f);
        renderWheel();
        GL11.glPopMatrix();
    
        // Extra wheel 1 (right side)
        GL11.glPushMatrix();
        float extraRightWheel1Y = terrain.getTerrianHeightAt(this.getX() + 0.9f, this.getZ() + 0.3f);
        GL11.glTranslatef(0.9f, extraRightWheel1Y + 0.5f - wheelHeightOffset, 0.3f);
        renderWheel();
        GL11.glPopMatrix();
    
        // Extra wheel 2 (left side)
        GL11.glPushMatrix();
        float extraLeftWheel2Y = terrain.getTerrianHeightAt(this.getX() - 0.9f, this.getZ() - 0.3f);
        GL11.glTranslatef(-0.9f, extraLeftWheel2Y + 0.5f - wheelHeightOffset, -0.3f);
        renderWheel();
        GL11.glPopMatrix();
    
        // Extra wheel 2 (right side)
        GL11.glPushMatrix();
        float extraRightWheel2Y = terrain.getTerrianHeightAt(this.getX() + 0.9f, this.getZ() - 0.3f);
        GL11.glTranslatef(0.9f, extraRightWheel2Y + 0.5f - wheelHeightOffset, -0.3f);
        renderWheel();
        GL11.glPopMatrix();
    
        // Mid-rear-left wheel
        GL11.glPushMatrix();
        float midRearLeftWheelY = terrain.getTerrianHeightAt(this.getX() - 0.9f, this.getZ() - 0.9f);
        GL11.glTranslatef(-0.9f, midRearLeftWheelY + 0.5f - wheelHeightOffset, -0.9f);
        renderWheel();
        GL11.glPopMatrix();
    
        // Mid-rear-right wheel
        GL11.glPushMatrix();
        float midRearRightWheelY = terrain.getTerrianHeightAt(this.getX() + 0.9f, this.getZ() - 0.9f);
        GL11.glTranslatef(0.9f, midRearRightWheelY + 0.5f - wheelHeightOffset, -0.9f);
        renderWheel();
        GL11.glPopMatrix();
    
        // Rear-left wheel
        GL11.glPushMatrix();
        float rearLeftWheelY = terrain.getTerrianHeightAt(this.getX() - 0.9f, this.getZ() - 1.5f);
        GL11.glTranslatef(-0.9f, rearLeftWheelY + 0.5f - wheelHeightOffset, -1.5f);
        renderWheel();
        GL11.glPopMatrix();
    
        // Rear-right wheel
        GL11.glPushMatrix();
        float rearRightWheelY = terrain.getTerrianHeightAt(this.getX() + 0.9f, this.getZ() - 1.5f);
        GL11.glTranslatef(0.9f, rearRightWheelY + 0.5f - wheelHeightOffset, -1.5f);
        renderWheel();
        GL11.glPopMatrix();
    }
}

class OBJLoader {
    public static Model loadModel(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        List<float[]> vertices = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<int[]> faces = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split("\\s+");
            if (tokens[0].equals("v")) {
                float[] vertex = {Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3])};
                vertices.add(vertex);
            } else if (tokens[0].equals("vn")) {
                float[] normal = {Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3])};
                normals.add(normal);
            } else if (tokens[0].equals("f")) {
                int[] face = {Integer.parseInt(tokens[1].split("/")[0]) - 1, Integer.parseInt(tokens[2].split("/")[0]) - 1, Integer.parseInt(tokens[3].split("/")[0]) - 1};
                faces.add(face);    
            }
        }

        float[] verticesArray = new float[vertices.size() * 3];
        float[] normalsArray = new float[vertices.size() * 3];
        int[] indicesArray = new int[faces.size() * 3];

        int vertexIndex = 0;
        for (float[] vertex : vertices) {
            verticesArray[vertexIndex++] = vertex[0];
            verticesArray[vertexIndex++] = vertex[1];
            verticesArray[vertexIndex++] = vertex[2];
        }

        int normalIndex = 0;
        for (float[] normal : normals) {
            normalsArray[normalIndex++] = normal[0];
            normalsArray[normalIndex++] = normal[1];
            normalsArray[normalIndex++] = normal[2];
        }

        int faceIndex = 0;
        for (int[] face : faces) {
            indicesArray[faceIndex++] = face[0];
            indicesArray[faceIndex++] = face[1];
            indicesArray[faceIndex++] = face[2];
        }
        
        reader.close();
        return new Model(verticesArray, normalsArray, indicesArray);
    }
}

class Model {
    private float[] vertices;
    private float[] normals;
    private int[] indices;

    public Model(float[] vertices, float[] normals, int[] indices) {
        this.vertices = vertices;
        this.normals = normals;
        this.indices = indices;
    }

    public float[] getVertices() {
        return vertices;
    }

    public float[] getNormals() {
        return normals;
    }

    public int[] getIndices() {
        return indices;
    }
}

class Terrain {
    private Model model;

    public Terrain(String objFilePath) {
        try {
            this.model = OBJLoader.loadModel(objFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void render() {
        GL11.glColor3f(0.3f, 0.8f, 0.3f); // Lighter green for the terrain
        GL11.glShadeModel(GL11.GL_SMOOTH); // Smooth shading for better Phong effect

        // Adjust terrain material properties to make it brighter
        FloatBuffer terrainAmbient = BufferUtils.createFloatBuffer(4).put(new float[] {0.6f, 0.8f, 0.6f, 1.0f});// Higher ambient light reflection
        FloatBuffer terrainDiffuse = BufferUtils.createFloatBuffer(4).put(new float[] { 0.7f, 0.9f, 0.7f, 1.0f }); // Higher diffuse reflection for visibility
        FloatBuffer terrainSpecular = BufferUtils.createFloatBuffer(4).put(new float[] { 0.2f, 0.2f, 0.2f, 1.0f }); // Light specular reflection for subtle shine

        terrainAmbient.flip();
        terrainDiffuse.flip();
        terrainSpecular.flip();

        GL11.glMaterialfv(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT, terrainAmbient);
        GL11.glMaterialfv(GL11.GL_FRONT_AND_BACK, GL11.GL_DIFFUSE, terrainDiffuse);
        GL11.glMaterialfv(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, terrainSpecular);
        GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 10.0f); // Lower shininess for a more matte look

        float[] vertices = model.getVertices();
        float[] normals = model.getNormals();
        int[] indices = model.getIndices();

        GL11.glBegin(GL11.GL_TRIANGLES);
        for (int i = 0; i < indices.length; i += 3) {
            int vertexIndex1 = indices[i] * 3;
            int vertexIndex2 = indices[i + 1] * 3;
            int vertexIndex3 = indices[i + 2] * 3;

            GL11.glNormal3f(normals[vertexIndex1], normals[vertexIndex1 + 1], normals[vertexIndex1 + 2]);
            GL11.glVertex3f(vertices[vertexIndex1], vertices[vertexIndex1 + 1], vertices[vertexIndex1 + 2]);
            GL11.glNormal3f(normals[vertexIndex2], normals[vertexIndex2 + 1], normals[vertexIndex2 + 2]);
            GL11.glVertex3f(vertices[vertexIndex2], vertices[vertexIndex2 + 1], vertices[vertexIndex2 + 2]);
            GL11.glNormal3f(normals[vertexIndex3], normals[vertexIndex3 + 1], normals[vertexIndex3 + 2]);
            GL11.glVertex3f(vertices[vertexIndex3], vertices[vertexIndex3 + 1], vertices[vertexIndex3 + 2]);
        }
        GL11.glEnd();
    }

    public float getTerrianHeightAt(float x, float z) {
        float[] vertices = model.getVertices(); // Get the terrain vertices
        int[] indices = model.getIndices(); // Get the triangle indices

        // Iterate through all triangles in the terrain mesh
        for (int i = 0; i < indices.length; i += 3) {
            // Get the vertices of the triangle
            int vertexIndex1 = indices[i] * 3;
            int vertexIndex2 = indices[i + 1] * 3;
            int vertexIndex3 = indices[i + 2] * 3;

            // Vertices of the triangle
            float v1X = vertices[vertexIndex1];
            float v1Y = vertices[vertexIndex1 + 1]; // The height at vertex 1
            float v1Z = vertices[vertexIndex1 + 2];

            float v2X = vertices[vertexIndex2];
            float v2Y = vertices[vertexIndex2 + 1]; // The height at vertex 2
            float v2Z = vertices[vertexIndex2 + 2];

            float v3X = vertices[vertexIndex3];
            float v3Y = vertices[vertexIndex3 + 1]; // The height at vertex 3
            float v3Z = vertices[vertexIndex3 + 2];

            // Check if the point (x, z) is inside the triangle
            if (isPointInTriangle(x, z, v1X, v1Z, v2X, v2Z, v3X, v3Z)) {
                // If the point is in the triangle, calculate the height using barycentric interpolation
                return interpolateHeight(x, z, v1X, v1Y, v1Z, v2X, v2Y, v2Z, v3X, v3Y, v3Z);
            }
        }
        // If no triangle was found, return 0 as a default
        return 0.0f;
    }

    private boolean isPointInTriangle(float px, float pz, float v1X, float v1Z, float v2X, float v2Z, float v3X, float v3Z) {
        float d1 = sign(px, pz, v1X, v1Z, v2X, v2Z);
        float d2 = sign(px, pz, v2X, v2Z, v3X, v3Z);
        float d3 = sign(px, pz, v3X, v3Z, v1X, v1Z);

        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);

        return !(hasNeg && hasPos); // Point is inside the triangle if all signs are the same
    }

    private float sign(float px, float pz, float v1X, float v1Z, float v2X, float v2Z) {
        return (px - v2X) * (v1Z - v2Z) - (v1X - v2X) * (pz - v2Z);
    }

    private float interpolateHeight(float x, float z, float v1X, float v1Y, float v1Z, float v2X, float v2Y, float v2Z, float v3X, float v3Y, float v3Z) {
        // Calculate the areas needed for barycentric interpolation
        float areaTotal = triangleArea(v1X, v1Z, v2X, v2Z, v3X, v3Z);
        float area1 = triangleArea(x, z, v2X, v2Z, v3X, v3Z);
        float area2 = triangleArea(x, z, v3X, v3Z, v1X, v1Z);
        float area3 = triangleArea(x, z, v1X, v1Z, v2X, v2Z);

        // Calculate the barycentric weights
        float weight1 = area1 / areaTotal;
        float weight2 = area2 / areaTotal;
        float weight3 = area3 / areaTotal;

        // Interpolate the height using the weights
        return weight1 * v1Y + weight2 * v2Y + weight3 * v3Y;
    }

    private float triangleArea(float x1, float z1, float x2, float z2, float x3, float z3) {
        return Math.abs((x1 * (z2 - z3) + x2 * (z3 - z1) + x3 * (z1 - z2)) / 2.0f);
    }
}

class Bullet {
    // private float x, y, z; // Bullet's position
    // private float speed = 0.1f; // Speed of the bullet
    // private float directionX, directionY, directionZ; // Direction of the bullet

    // public Bullet(float x, float y, float z, float directionX, float directionY, float directionZ) {
    //     this.x = x;
    //     this.y = y;
    //     this.z = z;
    //     this.directionX = directionX;
    //     this.directionY = directionY;
    //     this.directionZ = directionZ;
    // }

    // public void update() {
    //     x += directionX * speed;
    //     y += directionY * speed;
    //     z += directionZ * speed;
    // }

    // public void render() {
    //     GL11.glPushMatrix();
    //     GL11.glTranslatef(x, y, z);
    //     GL11.glColor3f(1.0f, 0.0f, 0.0f); // Red color for the bullet
    //     GL11.glBegin(GL11.GL_QUADS);
    //     GL11.glVertex3f(-0.1f, -0.1f, -0.1f);
    //     GL11.glVertex3f(0.1f, -0.1f, -0.1f);
    //     GL11.glVertex3f(0.1f, 0.1f, -0.1f);
    //     GL11.glVertex3f(-0.1f, 0.1f, -0.1f);
    //     GL11.glEnd();
    //     GL11.glPopMatrix();
    // }


}